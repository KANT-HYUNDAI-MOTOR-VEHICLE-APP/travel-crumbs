package com.example.travelcrumbs.domain.model

/** Callback 의 status — 문서상 이 셋뿐. */
enum class GleoStatus(val wireName: String) {
    SUCCESS("success"),
    ASKING("asking"),
    FAIL("fail"),
}

/**
 * Gleo AI 에 돌려줄 답. [message] 는 영어 완전한 문장.
 * `data` · `mode` 는 선택 칸이라 이 앱이 돌려줄 것이 생길 때 넣음.
 */
data class GleoCallback(
    val functionName: String,
    val status: GleoStatus,
    val message: String,
)
