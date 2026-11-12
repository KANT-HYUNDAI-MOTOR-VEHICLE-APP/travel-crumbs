package com.example.travelcrumbs.domain.model

/** Callback 의 status — 문서상 이 셋뿐. */
enum class GleoStatus(val wireName: String) {
    SUCCESS("success"),
    ASKING("asking"),
    FAIL("fail"),
}

/**
 * Gleo AI 에 돌려줄 답. [message] 는 영어 완전한 문장.
 * [data] = 선택 칸 · JSON 배열 문자열(공식 Callback 계약) — 정보 조회(#69)만 넣음. `mode` 는 기본값을 씀.
 */
data class GleoCallback(
    val functionName: String,
    val status: GleoStatus,
    val message: String,
    val data: String? = null,
)
