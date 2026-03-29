package com.redcom1988.srwagent.screens.submissiondetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.SubcomposeAsyncImage
import com.redcom1988.domain.submission.model.Submission
import com.redcom1988.domain.submission.model.SubmissionStatus
import com.redcom1988.srwagent.components.AppBar
import com.redcom1988.srwagent.components.StatusBadge
import com.redcom1988.srwagent.screens.map.MapRoutingScreen
import com.redcom1988.srwagent.screens.submissionimages.SubmissionImagesScreen
import com.redcom1988.srwagent.util.formatLastUpdated
import com.redcom1988.srwagent.util.toReadableStatus
import kotlin.time.ExperimentalTime
import androidx.core.net.toUri

data class SubmissionDetailScreen(
    val submission: Submission
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = remember { SubmissionDetailScreenModel() }
        val uiState by screenModel.uiState.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(uiState.isSuccess) {
            if (uiState.isSuccess) {
                navigator.pop()
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(uiState.error) {
            uiState.error?.let { error ->
                snackbarHostState.showSnackbar(error.message ?: "Failed to finish pickup")
                screenModel.clearError()
            }
        }

        SubmissionDetailScreenContent(
            submission = submission,
            isLoading = uiState.isLoading,
            onNavigateUp = { navigator.pop() },
            onViewImages = { sub, index -> navigator.push(SubmissionImagesScreen(sub, index)) },
            onOpenInMaps = { address ->
                val geoUri = "geo:0,0?q=${Uri.encode(address)}".toUri()
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(mapIntent)
                } else {
                    val browserIntent = Intent(Intent.ACTION_VIEW,
                        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}".toUri())
                    context.startActivity(browserIntent)
                }
            },
            onNavigateToRoute = { lat, lng, address ->
                navigator.push(MapRoutingScreen(lat, lng, address))
            },
            onFinishPickup = { notes ->
                screenModel.finishPickup(submission.id, notes)
            },
            snackbarHostState = snackbarHostState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun SubmissionDetailScreenContent(
    submission: Submission,
    isLoading: Boolean,
    onNavigateUp: () -> Unit,
    onViewImages: (Submission, Int) -> Unit,
    onOpenInMaps: (String) -> Unit,
    onNavigateToRoute: (Double, Double, String?) -> Unit,
    onFinishPickup: (String?) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var showFinishPickupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppBar(
                title = "Submission #${submission.id}",
                navigateUp = onNavigateUp,
                shadowElevation = 4.dp
            )
        },
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
            ) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = {
                        val lat = submission.submissionLatitude?.toDouble() ?: 0.0
                        val lng = submission.submissionLongitude?.toDouble() ?: 0.0
                        val address = submission.submissionAddress
                        onNavigateToRoute(lat, lng, address)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !submission.submissionAddress.isNullOrBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate to Location")
                }
                if (submission.status == SubmissionStatus.ASSIGNED) {
                    Button(
                        onClick = { showFinishPickupDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finish Pickup")
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Client Name",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = submission.clientName ?: "Unknown Client",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(status = submission.status)
            }


            // Row: Vehicle icon + Assigned time
            submission.assignedAt?.let { assignedAt ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Assignment Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatLastUpdated(assignedAt),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Row: Location icon + Pickup Location
            val pickupAddress = submission.submissionAddress
            if (!pickupAddress.isNullOrBlank())
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Pickup Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pickupAddress,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

            // Images section
            val submissionImages = submission.images
            if (!submissionImages.isNullOrEmpty()) {
                val imageUrls = submissionImages.map { it.url }
                Text(
                    text = "Images",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                ImagePager(
                    images = imageUrls,
                    onClick = { index -> onViewImages(submission, index) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showFinishPickupDialog) {
        FinishPickupDialog(
            onDismiss = { showFinishPickupDialog = false },
            onConfirm = { notes ->
                showFinishPickupDialog = false
                onFinishPickup(notes)
            },
            isLoading = isLoading
        )
    }
}

@Composable
private fun ImagePager(
    images: List<String>,
    onClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(images) { index, imageUrl ->
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = { onClick(index) })
            ) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "Image ${index + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = "Failed to load",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FinishPickupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
    isLoading: Boolean
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Finish Pickup") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add optional notes for this pickup:")
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Notes (optional)") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(notes.takeIf { it.isNotBlank() }) },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
