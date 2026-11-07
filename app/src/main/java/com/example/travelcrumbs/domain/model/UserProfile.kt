package com.example.travelcrumbs.domain.model

/** 로그인된 사용자 — 화면이 쓰는 모양(user_mst 에서 옴). */
data class UserProfile(
    val userNo: Long,
    val nickname: String,
)
