package com.example.travelcrumbs.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 로컬 서버(server/) API — 경로 · 메서드는 서버 라우터와 1:1. */
interface AppApi {

    /** 로그인된 사용자 정보 — 홈(A-03) 인사말. 서버 `GET /me`. */
    @GET("me")
    suspend fun getMe(): MeResponse

    /** 최신 동의 상태 — 위치정보 동의(A-03-1). 서버 `GET /consents/latest`. */
    @GET("consents/latest")
    suspend fun getLatestConsent(@Query("consent_type_cd") consentTypeCd: String): ConsentLatestResponse

    /** 동의 기록(user_consent_hist 1건 추가). 서버 `POST /consents`. */
    @POST("consents")
    suspend fun createConsent(@Body body: ConsentCreateRequest): ConsentLatestResponse

    /** 코드 목록(cd_dtl) — 예: COMPANION_TYPE 동행자 버튼(A-04). 서버 `GET /codes/{cd_grp_cd}`. */
    @GET("codes/{cd_grp_cd}")
    suspend fun getCodes(@Path("cd_grp_cd") cdGrpCd: String): List<CodeResponse>

    /** 기록 중인 여행(trip_mst RECORDING). 서버 `GET /trips/current`. */
    @GET("trips/current")
    suspend fun getCurrentTrip(): CurrentTripResponse

    /** 여행 시작 — trip_mst · trip_companion_dtl · trip_status_hist 저장. 서버 `POST /trips`. */
    @POST("trips")
    suspend fun createTrip(@Body body: TripCreateRequest): TripResponse
}
