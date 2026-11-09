package com.example.travelcrumbs.data.remote

import com.example.travelcrumbs.data.auth.AccessTokenSource
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** `Authorization: Bearer <토큰>` 을 붙임 — 토큰이 없으면 붙이지 않음. 갱신(TokenAuthenticator)은 #1. */
class AuthInterceptor @Inject constructor(
    private val tokens: AccessTokenSource,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokens.accessToken() ?: return chain.proceed(chain.request())
        return chain.proceed(
            chain.request().newBuilder().header("Authorization", "Bearer $token").build(),
        )
    }
}
