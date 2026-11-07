package com.example.travelcrumbs.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 서버 `POST /consents` 요청 — JSON 키는 user_consent_hist 표준 컬럼명 그대로. */
@Serializable
data class ConsentCreateRequest(
    @SerialName("consent_type_cd") val consentTypeCd: String,
    @SerialName("consent_yn") val consentYn: String,
)

/** 서버 `GET /consents/latest` · `POST /consents` 응답 — 최신 동의 상태. 이력이 없으면 consentYn = null. */
@Serializable
data class ConsentLatestResponse(
    @SerialName("consent_type_cd") val consentTypeCd: String,
    @SerialName("consent_yn") val consentYn: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
