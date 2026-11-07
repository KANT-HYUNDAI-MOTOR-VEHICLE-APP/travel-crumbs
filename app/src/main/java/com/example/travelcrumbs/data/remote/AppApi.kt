package com.example.travelcrumbs.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    /** 경로점 묶음 저장(trip_route_point_dtl) — 여행 기록 중(A-05). 서버 `POST /trips/{trip_uuid}/route-points`. */
    @POST("trips/{trip_uuid}/route-points")
    suspend fun createRoutePoints(
        @Path("trip_uuid") tripUuid: String,
        @Body body: RoutePointsCreateRequest,
    ): RoutePointsCreateResponse

    /** 「이 장소 기록」 임시 저장(trip_place_dtl) — A-05. 서버 `POST /trips/{trip_uuid}/places`. */
    @POST("trips/{trip_uuid}/places")
    suspend fun createPlace(
        @Path("trip_uuid") tripUuid: String,
        @Body body: TripPlaceCreateRequest,
    ): TripPlaceResponse

    /** 기록한 장소 목록 — draftYn = "Y" 면 입력 안 한 임시 기록만(A-06). 서버 `GET /trips/{trip_uuid}/places`. */
    @GET("trips/{trip_uuid}/places")
    suspend fun getPlaces(
        @Path("trip_uuid") tripUuid: String,
        @Query("draft_yn") draftYn: String?,
    ): List<TripPlaceItem>

    /** 장소 경험 입력 「저장」 — 임시 기록 완성(draft_yn = N). 서버 `PUT /trips/{trip_uuid}/places/{trip_place_no}`. */
    @PUT("trips/{trip_uuid}/places/{trip_place_no}")
    suspend fun updatePlace(
        @Path("trip_uuid") tripUuid: String,
        @Path("trip_place_no") tripPlaceNo: Long,
        @Body body: TripPlaceUpdateRequest,
    ): TripPlaceItem

    /** 경험 태그 목록(tag_mst). 서버 `GET /tags`. */
    @GET("tags")
    suspend fun getTags(): List<TagResponse>

    /** 장소유형 목록(place_type_mst) — 카테고리 → 유형 버튼(A-06). 서버 `GET /place-types`. */
    @GET("place-types")
    suspend fun getPlaceTypes(): List<PlaceTypeResponse>

    /** 여행 종료 — 완료 · 거리 · 소요시간 · 간소화 경로 · 상태 이력(A-08). 서버 `POST /trips/{trip_uuid}/end`. */
    @POST("trips/{trip_uuid}/end")
    suspend fun endTrip(@Path("trip_uuid") tripUuid: String): TripDetailResponse

    /** 여행 상세(A-08). 서버 `GET /trips/{trip_uuid}`. */
    @GET("trips/{trip_uuid}")
    suspend fun getTrip(@Path("trip_uuid") tripUuid: String): TripDetailResponse

    /** 「제목 수정」 — trip_nm 수정 + 데이터변경이력. 서버 `PUT /trips/{trip_uuid}`. */
    @PUT("trips/{trip_uuid}")
    suspend fun updateTrip(@Path("trip_uuid") tripUuid: String, @Body body: TripUpdateRequest): TripDetailResponse
}
