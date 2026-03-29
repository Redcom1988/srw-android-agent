package com.redcom1988.srwagent.screens.map

import cafe.adriel.voyager.core.model.ScreenModel
import com.google.android.gms.maps.model.LatLng
import com.redcom1988.core.di.util.inject
import com.redcom1988.srwagent.data.RouteResult
import com.redcom1988.srwagent.data.RouteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MapRoutingUiState(
    val userLocation: LatLng? = null,
    val routePoints: List<LatLng> = emptyList(),
    val isLoadingRoute: Boolean = true,
    val routeError: String? = null
)

class MapRoutingScreenModel : ScreenModel {
    private val routeService: RouteService = inject()

    private val _state = MutableStateFlow(MapRoutingUiState())
    val state: StateFlow<MapRoutingUiState> = _state.asStateFlow()

    fun updateUserLocation(location: LatLng) {
        _state.value = _state.value.copy(userLocation = location)
    }

    suspend fun fetchRoute(destinationLatLng: LatLng) {
        val userLocation = _state.value.userLocation ?: run {
            _state.value = _state.value.copy(isLoadingRoute = false, routeError = "Unable to get current location")
            return
        }
        
        _state.value = _state.value.copy(isLoadingRoute = true, routeError = null)
        
        val result = routeService.getRoute(userLocation, destinationLatLng)
        when (result) {
            is RouteResult.Success -> {
                _state.value = _state.value.copy(
                    routePoints = result.points,
                    isLoadingRoute = false
                )
            }
            is RouteResult.Error -> {
                _state.value = _state.value.copy(
                    routeError = result.message,
                    isLoadingRoute = false
                )
            }
        }
    }
}
