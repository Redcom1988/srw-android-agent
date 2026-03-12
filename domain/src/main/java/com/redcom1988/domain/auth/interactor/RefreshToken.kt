package com.redcom1988.domain.auth.interactor

import com.redcom1988.domain.auth.model.AuthToken
import com.redcom1988.domain.auth.repository.AuthRepository
import com.redcom1988.domain.auth.repository.TokenStorage

class RefreshToken(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage
) {

    suspend fun await(): Result {
        return try {
            val refreshToken = tokenStorage.getRefreshToken()

            if (refreshToken.isNullOrEmpty()) {
                return Result.Error(Exception("No refresh token available"))
            }

            val authToken = authRepository.refreshToken(refreshToken)

            tokenStorage.saveAccessToken(authToken.accessToken)
            tokenStorage.saveRefreshToken(authToken.refreshToken)

            Result.Success(authToken)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    sealed interface Result {
        data class Success(val authToken: AuthToken) : Result
        data class Error(val error: Throwable) : Result
    }

}