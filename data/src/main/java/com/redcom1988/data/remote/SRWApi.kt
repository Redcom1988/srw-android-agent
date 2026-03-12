package com.redcom1988.data.remote

import com.redcom1988.core.network.GET
import com.redcom1988.core.network.NetworkHelper
import com.redcom1988.core.network.NetworkPreference
import com.redcom1988.core.network.POST
import com.redcom1988.core.network.await
import com.redcom1988.core.network.json
import com.redcom1988.data.remote.model.auth.AuthRequest
import com.redcom1988.data.remote.model.submission.PickupRequest
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File

class SRWApi(
    private val networkHelper: NetworkHelper,
    private val preference: NetworkPreference
) {
    suspend fun login(username: String, password: String): Response {
        val requestBody = json.encodeToString(AuthRequest(username, password))
            .toRequestBody("application/json".toMediaType())

        return networkHelper.client.newCall(
            POST(
                url = preference.baseUrl().get() + "/auth/login/agent",
                body = requestBody
            )
        ).await()
    }

    suspend fun refreshToken(refreshToken: String): Response {
        val requestBody = json.encodeToString(AuthRequest(refreshToken = refreshToken))
            .toRequestBody("application/json".toMediaType())

        return networkHelper.client.newCall(
            POST(
                url = preference.baseUrl().get() + "/auth/refresh",
                body = requestBody
            )
        ).await()
    }

    suspend fun logout(refreshToken: String): Response {
        val requestBody = json.encodeToString(AuthRequest(refreshToken = refreshToken))
            .toRequestBody("application/json".toMediaType())

        return networkHelper.client.newCall(
            POST(
                url = preference.baseUrl().get() + "/auth/logout",
                body = requestBody
            )
        ).await()
    }

    suspend fun getSubmissions(
        page: Int = 1,
        pageSize: Int = 20
    ): Response {
        val url = preference.baseUrl().get() + "/agents/submissions?page=$page&pageSize=$pageSize"

        return networkHelper.client.newCall(
            GET(
                url = url,
            )
        ).await()
    }

    suspend fun finishPickup(
        id: Int,
        notes: String?
    ): Response {
        val requestBody = json.encodeToString(PickupRequest(notes = notes))
            .toRequestBody("application/json".toMediaType())

        return networkHelper.client.newCall(
            POST(
                url = preference.baseUrl().get() + "/agents/submissions/$id/pickup"
            )
        ).await()
    }
}