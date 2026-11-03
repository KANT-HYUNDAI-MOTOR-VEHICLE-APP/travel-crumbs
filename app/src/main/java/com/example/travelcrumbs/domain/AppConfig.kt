package com.example.travelcrumbs.domain

/** 바뀔 수치 한 곳. */
object AppConfig {
    /** SDK 호출 응답을 기다리는 한도(밀리초). */
    const val SDK_CALL_TIMEOUT_MS: Long = 10_000L

    /** 네트워크 타임아웃(초). */
    const val NETWORK_TIMEOUT_SECONDS: Long = 20L

    /** 골격 확인용 짧은 프롬프트. */
    const val SMOKE_PROMPT: String = "한 문장으로 자기소개 해 줘."
}
