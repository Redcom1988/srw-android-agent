package com.redcom1988.domain.auth.repository

import com.redcom1988.domain.auth.model.AuthToken

interface AuthRepository {
    suspend fun login(
        username: String,
        password: String
    ): AuthToken
    suspend fun logout(refreshToken: String)
    suspend fun refreshToken(refreshToken: String): AuthToken
}
