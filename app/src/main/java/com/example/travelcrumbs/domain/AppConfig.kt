package com.example.travelcrumbs.domain

/** 바뀔 수치 · 고정 문자열 한 곳. */
object AppConfig {
    /** SDK 호출 응답을 기다리는 한도(밀리초). */
    const val SDK_CALL_TIMEOUT_MS: Long = 10_000L

    /** 네트워크 타임아웃(초). */
    const val NETWORK_TIMEOUT_SECONDS: Long = 20L

    /**
     * Gleo Interface 의 유일한 function 이름.
     * `app/src/main/assets/schema.json` 의 `actionCalls[].function.name` · `AndroidManifest.xml` 의
     * `<action>` 과 **글자가 같아야 함** — 셋 다 문자열이라 빌드가 잡아 주지 않음.
     */
    const val GLEO_FUNCTION_OPEN: String = "OPEN_TRAVEL_CRUMBS"

    /** Gleo 에 돌려줄 문장(영어 완전한 문장) — 화면 문구가 아니라 프로토콜 값이라 strings.xml 이 아님. */
    const val GLEO_OPEN_SUCCESS_MESSAGE: String = "Travel Crumbs is now open on the screen."
    const val GLEO_OPEN_FAIL_MESSAGE: String = "Travel Crumbs could not read the request."
}
