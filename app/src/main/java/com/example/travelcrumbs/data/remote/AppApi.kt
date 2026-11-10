package com.example.travelcrumbs.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 우리 서버 API — 계약 `docs/api/openapi.json`(스펙 navigation 5절)과 경로 · 메서드 1:1.
 * 인증 헤더는 [AuthInterceptor] 가 붙임. 응답 코드로 처리를 가르므로 [Response] 로 받음.
 */
interface AppApi {
    /** 안내 시작 기록(핀 1건) — 201 새로 받음 · 200 이미 받음. */
    @POST("navigation-starts")
    suspend fun createNavigationStart(@Body body: NavigationStartIn): Response<NavigationStartOut>

    /** 장소별 안내 건수 — debug 진단 카드 한 줄(U41). */
    @GET("navigation-starts/stats")
    suspend fun getNavigationStats(@Query("limit") limit: Int): Response<NavigationStatsOut>
}
