package com.example.travelcrumbs.domain.model

/**
 * Gleo AI 가 Intent 로 보내 온 Action — SDK 타입이 아니라 순수 값.
 * [parameters] 는 ACTION_PARAM JSON 을 문자열 쌍으로만 풀어 둔 것 — 이 앱은 아직 쓰는 파라미터가 없음.
 */
data class GleoAction(
    val functionName: String,
    val parameters: Map<String, String>,
)
