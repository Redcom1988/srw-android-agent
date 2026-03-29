package com.redcom1988.core.network.interceptor

import com.redcom1988.core.network.NetworkPreference
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val preference: NetworkPreference
): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.toString()
        
        if (url.contains("/auth/refresh") || url.contains("/auth/login")) {
            return chain.proceed(chain.request())
        }
        
        val accessToken = preference.accessToken().get()
        
        if (accessToken.isEmpty()) {
            return chain.proceed(chain.request())
        }
        
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        return chain.proceed(request)
    }
}
