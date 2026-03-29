package com.redcom1988.srwagent.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.redcom1988.domain.submission.model.Submission
import com.redcom1988.srwagent.components.AppBar
import com.redcom1988.srwagent.components.StatusBadge
import com.redcom1988.srwagent.screens.login.LoginScreen
import com.redcom1988.srwagent.screens.submissiondetail.PickupResult
import com.redcom1988.srwagent.screens.submissiondetail.PickupResultBus
import com.redcom1988.srwagent.screens.submissiondetail.SubmissionDetailScreen
import com.redcom1988.srwagent.util.formatLastUpdated
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.ExperimentalTime

object HomeScreen: Screen {
    private fun readResolve(): Any = HomeScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { HomeScreenModel() }
        val lazyPagingItems = screenModel.submissionsPagingData.collectAsLazyPagingItems()
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) {
            PickupResultBus.events.collectLatest { result ->
                if (result is PickupResult.Success) {
                    lazyPagingItems.refresh()
                }
            }
        }

        HomeScreenContent(
            lazyPagingItems = lazyPagingItems,
            isLoggingOut = uiState.isLoggingOut,
            logoutError = uiState.logoutError,
            onClickLogout = { screenModel.logout() },
            onRefresh = { lazyPagingItems.refresh() },
            onLogoutSuccess = {
                navigator.replaceAll(LoginScreen)
            },
            onErrorShown = { screenModel.clearError() },
            navigator = navigator
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun HomeScreenContent(
        lazyPagingItems: LazyPagingItems<Submission>,
        isLoggingOut: Boolean = false,
        logoutError: Throwable? = null,
        onClickLogout: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onLogoutSuccess: () -> Unit = {},
        onErrorShown: () -> Unit = {},
        navigator: Navigator
    ) {
        val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
        var showLogoutDialog by remember { mutableStateOf(false) }
        var wasLoggingOut by remember { mutableStateOf(false) }

        LaunchedEffect(isLoggingOut) {
            if (wasLoggingOut && !isLoggingOut && logoutError == null) {
                onLogoutSuccess()
            } else if (logoutError != null) {
                onErrorShown()
            }
            wasLoggingOut = isLoggingOut
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = "Submissions", // TODO String Resource
                    actions = {
                        IconButton(
                            onClick = { showLogoutDialog = true },
                            enabled = !isLoggingOut,
                            content = {
                                if (isLoggingOut) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    },
                    shadowElevation = 4.dp
                )
            }
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { lazyPagingItems.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (// Error state (only show when not refreshing)
                        lazyPagingItems.loadState.refresh) {
                        is LoadState.Error if !isRefreshing -> {
                            item {
                                val error = (lazyPagingItems.loadState.refresh as LoadState.Error).error
                                Column(
                                    modifier = Modifier.fillParentMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Error: ${error.message}", // TODO String Resource
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    TextButton(onClick = { lazyPagingItems.retry() }) {
                                        Text("Retry") // TODO String Resource
                                    }
                                }
                            }
                        }

                        // Empty state
                        is LoadState.NotLoading if lazyPagingItems.itemCount == 0 -> {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No submissions found") // TODO String Resource
                                }
                            }
                        }

                        else -> {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { it.id }
                            ) { index ->
                                val submission = lazyPagingItems[index]
                                if (submission != null) {
                                    SubmissionCard(
                                        submission = submission,
                                        onClick = {
                                            navigator.push(SubmissionDetailScreen(submission))
                                        }
                                    )
                                }
                            }

                            if (lazyPagingItems.loadState.append is LoadState.Loading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            // Error indicator when loading more fails
                            if (lazyPagingItems.loadState.append is LoadState.Error) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(onClick = { lazyPagingItems.retry() }) {
                                            Text("Load more failed. Retry") // TODO String Resource
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout") },
                text = { Text("Are you sure you want to logout?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onClickLogout()
                        }
                    ) {
                        Text("Logout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
    @Composable
    fun SubmissionCard(
        submission: Submission,
        onClick: () -> Unit = {}
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = submission.clientName ?: "Unknown Client",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val address = submission.submissionAddress
                if (!address.isNullOrBlank()) {
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    submission.assignedAt?.let { assignedAt ->
                        Text(
                            text = "Assigned: ${formatLastUpdated(assignedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    StatusBadge(status = submission.status)
                }
            }
        }
    }
}