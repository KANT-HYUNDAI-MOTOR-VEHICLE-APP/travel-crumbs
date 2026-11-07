package com.example.travelcrumbs.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 서버 `GET /me` 응답 — JSON 키는 user_mst 표준 컬럼명 그대로. */
@Serializable
data class MeResponse(
    @SerialName("user_no") val userNo: Long,
    @SerialName("nickname_nm") val nicknameNm: String,
)
