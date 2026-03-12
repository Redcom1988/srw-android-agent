package com.redcom1988.srwagent.screens.submissionimages

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.redcom1988.domain.submission.model.Submission
import com.redcom1988.srwagent.components.ImagePagerViewer

data class SubmissionImagesScreen(
    val submission: Submission,
    val initialPage: Int = 0
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val imageUrls = submission.images?.map { it.url } ?: emptyList()

        BackHandler(onBack = { navigator.pop() })

        ImagePagerViewer(
            images = imageUrls,
            initialPage = initialPage.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0)),
            title = "Submission #${submission.id}",
            onNavigateUp = { navigator.pop() },
            canDelete = false
        )
    }
}
