package com.redcom1988.domain.auth.interactor

import com.redcom1988.domain.auth.repository.AuthRepository
import com.redcom1988.domain.auth.repository.TokenStorage

class Logout(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage
) {
    suspend fun await(): Result {
        return try {
            val refreshToken = tokenStorage.getRefreshToken()

            if (!refreshToken.isNullOrEmpty()) {
                authRepository.logout(refreshToken)
            }

            tokenStorage.clearTokens()

            Result.Success
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class Error(val error: Throwable) : Result
    }

}