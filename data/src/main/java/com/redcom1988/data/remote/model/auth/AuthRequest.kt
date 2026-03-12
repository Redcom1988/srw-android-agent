package com.redcom1988.data.remote.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String? = null,
    val password: String? = null,
    val refreshToken: String? = null
)