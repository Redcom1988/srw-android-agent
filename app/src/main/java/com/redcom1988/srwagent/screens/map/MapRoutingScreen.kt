package com.redcom1988.srwagent.screens.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.redcom1988.srwagent.components.AppBar
import androidx.core.net.toUri

data class MapRoutingScreen(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
) : Screen {

    private fun openGoogleMaps(context: Context) {
        val gmmIntentUri = "google.navigation:q=$latitude,$longitude&mode=d".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val browserUri =
                "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=driving".toUri()
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            context.startActivity(browserIntent)
        }
    }

    @Suppress("MissingPermission")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { MapRoutingScreenModel() }
        val state by screenModel.state.collectAsState()

        val destinationLatLng = remember(latitude, longitude) {
            LatLng(latitude, longitude)
        }

        val fusedLocationClient = remember {
            LocationServices.getFusedLocationProviderClient(context)
        }

        val locationPermissionState =
            rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

        LaunchedEffect(Unit) {
            if (!locationPermissionState.status.isGranted) {
                locationPermissionState.launchPermissionRequest()
            }
        }

        LaunchedEffect(locationPermissionState.status.isGranted) {
            if (locationPermissionState.status.isGranted) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            screenModel.updateUserLocation(LatLng(it.latitude, it.longitude))
                        }
                    }
                } catch (e: SecurityException) {
                }
            }
        }

        LaunchedEffect(state.userLocation) {
            if (latitude != 0.0 && longitude != 0.0) {
                state.userLocation?.let {
                    screenModel.fetchRoute(destinationLatLng)
                }
            }
        }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(destinationLatLng, 15f)
        }

        LaunchedEffect(state.routePoints, destinationLatLng) {
            if (state.routePoints.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.Builder()
                state.routePoints.forEach { boundsBuilder.include(it) }
                boundsBuilder.include(destinationLatLng)
                state.userLocation?.let { boundsBuilder.include(it) }
                try {
                    val bounds = boundsBuilder.build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } catch (e: Exception) {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            destinationLatLng,
                            12f
                        )
                    )
                }
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = "Navigation",
                    navigateUp = { navigator.pop() },
                    shadowElevation = 4.dp
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { openGoogleMaps(context) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Open in Google Maps"
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = locationPermissionState.status.isGranted
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = locationPermissionState.status.isGranted
                    )
                ) {
                    state.userLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Current Location"
                        )
                    }

                    Marker(
                        state = MarkerState(position = destinationLatLng),
                        title = address ?: "Destination",
                        snippet = if (address != null) "$latitude, $longitude" else null
                    )

                    if (state.routePoints.isNotEmpty()) {
                        Polyline(
                            points = state.routePoints,
                            color = Color.Blue,
                            width = 12f
                        )
                    }
                }

                if (state.isLoadingRoute) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.routeError?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "Could not load route: $error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
