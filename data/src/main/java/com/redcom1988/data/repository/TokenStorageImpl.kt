package com.redcom1988.data.repository

import com.redcom1988.core.network.NetworkPreference
import com.redcom1988.domain.auth.repository.TokenStorage

class TokenStorageImpl(
    private val networkPreference: NetworkPreference
) : TokenStorage {

    override suspend fun saveAccessToken(token: String) {
        networkPreference.accessToken().set(token)
    }

    override suspend fun saveRefreshToken(token: String) {
        networkPreference.refreshToken().set(token)
    }

    override suspend fun getAccessToken(): String? {
        return networkPreference.accessToken().get().takeIf { it.isNotEmpty() }
    }

    override suspend fun getRefreshToken(): String? {
        return networkPreference.refreshToken().get().takeIf { it.isNotEmpty() }
    }

    override suspend fun clearTokens() {
        networkPreference.accessToken().delete()
        networkPreference.refreshToken().delete()
    }
}
