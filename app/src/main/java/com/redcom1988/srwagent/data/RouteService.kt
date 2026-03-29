package com.redcom1988.srwagent.data

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class RouteService(
    private val context: Context
) {
    private val apiKey: String by lazy {
        val packageInfo = context.packageManager.getApplicationInfo(context.packageName, 128)
        packageInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
    }
    private val client = OkHttpClient()

    suspend fun getRoute(origin: LatLng, destination: LatLng): RouteResult = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("https://maps.googleapis.com/maps/api/directions/json?")
                append("origin=${origin.latitude},${origin.longitude}")
                append("&destination=${destination.latitude},${destination.longitude}")
                append("&mode=driving")
                append("&key=$apiKey")
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw IOException("Empty response")

            val jsonResponse = JSONObject(body)
            val status = jsonResponse.optString("status")

            if (status == "OK") {
                val routes = jsonResponse.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val overviewPolyline = route.optJSONObject("overview_polyline")
                    val pointsEncoded = overviewPolyline?.optString("points")
                    if (!pointsEncoded.isNullOrEmpty()) {
                        val points = decodePolyline(pointsEncoded)
                        RouteResult.Success(points)
                    } else {
                        RouteResult.Error("No route points found")
                    }
                } else {
                    RouteResult.Error("No routes found")
                }
            } else {
                RouteResult.Error(status.ifEmpty { "Unknown error" })
            }
        } catch (e: Exception) {
            RouteResult.Error(e.message ?: "Failed to get route")
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var byte: Int

            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)

            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0

            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)

            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }

        return poly
    }
}

sealed class RouteResult {
    data class Success(val points: List<LatLng>) : RouteResult()
    data class Error(val message: String) : RouteResult()
}
