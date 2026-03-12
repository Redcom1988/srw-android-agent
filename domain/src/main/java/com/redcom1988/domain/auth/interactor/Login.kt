package com.redcom1988.domain.auth.interactor

import com.redcom1988.domain.auth.model.AuthToken
import com.redcom1988.domain.auth.repository.AuthRepository
import com.redcom1988.domain.auth.repository.TokenStorage
import java.io.IOException

class Login(
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage
) {

    suspend fun await(
        username: String,
        password: String
    ): Result {
        return try {
            val authToken = authRepository.login(
                username = username,
                password = password
            )
            tokenStorage.saveAccessToken(authToken.accessToken)
            tokenStorage.saveRefreshToken(authToken.refreshToken)
            Result.Success(authToken)
        } catch (e: IOException) {
            Result.Error("Unable to connect. Please check your network connection.")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Login failed")
        }
    }

    sealed interface Result {
        data class Success(val authToken: AuthToken) : Result
        data class Error(val message: String) : Result
    }

}