package com.example.travelcrumbs.data.remote

import retrofit2.http.GET

/** 로컬 서버(server/) API — 경로 · 메서드는 서버 라우터와 1:1. */
interface AppApi {

    /** 로그인된 사용자 정보 — 홈(A-03) 인사말. 서버 `GET /me`. */
    @GET("me")
    suspend fun getMe(): MeResponse
}
