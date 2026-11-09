package com.example.travelcrumbs.data.auth

import com.example.travelcrumbs.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 우리 서버 액세스 토큰을 내주는 곳 — 로그인(토큰 발급 · 갱신 · 보관)은 #1 이 이 자리를 바꿔 끼움(스펙 navigation U27 · U28).
 * 토큰이 없으면 서버로 보내지 않음(4.6). 토큰 값은 로그에 남기지 않음.
 */
interface AccessTokenSource {
    fun accessToken(): String?
}

/**
 * #1 전 임시 — debug 빌드 전용 시드 토큰(`local.properties` 의 `travelcrumbs.debugSeedToken` · 저장소에 커밋하지 않음).
 * release 빌드는 값이 빈 문자열이라 늘 「토큰 없음」.
 */
@Singleton
class DebugSeedAccessTokenSource @Inject constructor() : AccessTokenSource {
    override fun accessToken(): String? =
        BuildConfig.DEBUG_SEED_TOKEN.takeIf { BuildConfig.DEBUG && it.isNotBlank() }
}
