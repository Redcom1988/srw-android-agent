package com.redcom1988.srwagent.components

import android.view.ViewConfiguration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample

const val GRID_STICKY_HEADER_KEY_PREFIX = "sticky:"

@Composable
fun Modifier.drawHorizontalScrollbar(
    state: LazyGridState,
    reverseScrolling: Boolean = false,
    positionOffsetPx: Float = 0f,
): Modifier = drawScrollbar(state, Orientation.Horizontal, reverseScrolling, positionOffsetPx)

@Composable
fun Modifier.drawVerticalScrollbar(
    state: LazyGridState,
    reverseScrolling: Boolean = false,
    positionOffsetPx: Float = 0f,
): Modifier = drawScrollbar(state, Orientation.Vertical, reverseScrolling, positionOffsetPx)

@Composable
private fun Modifier.drawScrollbar(
    state: LazyGridState,
    orientation: Orientation,
    reverseScrolling: Boolean,
    positionOffset: Float,
): Modifier = drawScrollbar(
    orientation,
    reverseScrolling,
) { reverseDirection, atEnd, thickness, color, alpha ->
    val layoutInfo = state.layoutInfo

    val viewportSize = if (orientation == Orientation.Horizontal) {
        layoutInfo.viewportSize.width
    } else {
        layoutInfo.viewportSize.height
    }

    val contentViewportSize = viewportSize - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding

    val items = layoutInfo.visibleItemsInfo
    val itemsSize = items.fastSumBy {
        if (orientation == Orientation.Horizontal) it.size.width else it.size.height
    }
    val showScrollbar = items.size < layoutInfo.totalItemsCount || itemsSize > contentViewportSize
    val estimatedItemSize = if (items.isEmpty()) 0f else itemsSize.toFloat() / items.size
    val totalSize = estimatedItemSize * layoutInfo.totalItemsCount

    val thumbSize = (contentViewportSize / totalSize * contentViewportSize).coerceAtMost(contentViewportSize.toFloat())

    val startOffset = if (items.isEmpty()) {
        0f
    } else {
        items
            .fastFirstOrNull { (it.key as? String)?.startsWith(GRID_STICKY_HEADER_KEY_PREFIX)?.not() ?: true }
            ?.run {
                val itemOffset = if (orientation == Orientation.Horizontal) {
                    offset.x
                } else {
                    offset.y
                }

                val scrollProgress = if (totalSize > 0) {
                    (estimatedItemSize * index - itemOffset) / totalSize
                } else {
                    0f
                }

                val availableScrollArea = contentViewportSize - thumbSize
                val calculatedOffset = scrollProgress * availableScrollArea

                val paddingOffset = if (reverseDirection) {
                    layoutInfo.afterContentPadding
                } else {
                    layoutInfo.beforeContentPadding
                }

                (paddingOffset + calculatedOffset).coerceIn(0f, viewportSize - thumbSize)
            } ?: 0f
    }

    val drawScrollbar = onDrawScrollbar(
        orientation, reverseDirection, atEnd, showScrollbar,
        thickness, color, alpha, thumbSize, startOffset, positionOffset,
    )
    drawContent()
    drawScrollbar()
}

private fun ContentDrawScope.onDrawScrollbar(
    orientation: Orientation,
    reverseDirection: Boolean,
    atEnd: Boolean,
    showScrollbar: Boolean,
    thickness: Float,
    color: Color,
    alpha: () -> Float,
    thumbSize: Float,
    scrollOffset: Float,
    positionOffset: Float,
): DrawScope.() -> Unit {
    val topLeft = if (orientation == Orientation.Horizontal) {
        Offset(
            if (reverseDirection) size.width - scrollOffset - thumbSize else scrollOffset,
            if (atEnd) size.height - positionOffset - thickness else positionOffset,
        )
    } else {
        Offset(
            if (atEnd) size.width - positionOffset - thickness else positionOffset,
            if (reverseDirection) size.height - scrollOffset - thumbSize else scrollOffset,
        )
    }
    val thumbSizeFinal = if (orientation == Orientation.Horizontal) {
        Size(thickness, thumbSize)
    } else {
        Size(thumbSize, thickness)
    }

    return {
        if (showScrollbar && thumbSize > 0) {
            drawRect(
                color = color,
                topLeft = topLeft,
                size = thumbSizeFinal,
                alpha = alpha(),
            )
        }
    }
}

@Composable
@OptIn(FlowPreview::class)
private fun Modifier.drawScrollbar(
    orientation: Orientation,
    reverseScrolling: Boolean,
    onDraw: ContentDrawScope.(
        reverseDirection: Boolean,
        atEnd: Boolean,
        thickness: Float,
        color: Color,
        alpha: () -> Float,
    ) -> Unit,
): Modifier {
    val scrolled = remember {
        MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }
    val nestedScrollConnection = remember(orientation, scrolled) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val delta = if (orientation == Orientation.Horizontal) consumed.x else consumed.y
                if (delta != 0f) scrolled.tryEmit(Unit)
                return Offset.Zero
            }
        }
    }

    val alpha = remember { Animatable(0f) }
    LaunchedEffect(scrolled, alpha) {
        scrolled
            .sample(100)
            .collectLatest {
                alpha.snapTo(1f)
                alpha.animateTo(0f, animationSpec = FadeOutAnimationSpec)
            }
    }

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val reverseDirection = if (orientation == Orientation.Horizontal) {
        if (isLtr) reverseScrolling else !reverseScrolling
    } else {
        reverseScrolling
    }
    val atEnd = if (orientation == Orientation.Vertical) isLtr else true

    val context = LocalContext.current
    val thickness = remember { ViewConfiguration.get(context).scaledScrollBarSize.toFloat() }
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.364f)

    return this
        .nestedScroll(nestedScrollConnection)
        .drawWithContent {
            onDraw(reverseDirection, atEnd, thickness, color, alpha::value)
        }
}

private val FadeOutAnimationSpec = tween<Float>(
    durationMillis = ViewConfiguration.getScrollBarFadeDuration(),
    delayMillis = ViewConfiguration.getScrollDefaultDelay(),
)

@Composable
fun LazyGridScrollbar(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier.drawVerticalScrollbar(state),
        state = state,
        contentPadding = contentPadding,
        content = content,
    )
}
