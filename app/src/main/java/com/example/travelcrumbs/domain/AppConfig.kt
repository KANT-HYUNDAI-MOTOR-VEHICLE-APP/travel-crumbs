package com.example.travelcrumbs.domain

/** 바뀔 수치 · 고정 문자열 한 곳. */
object AppConfig {
    /** SDK 호출 응답을 기다리는 한도(밀리초). */
    const val SDK_CALL_TIMEOUT_MS: Long = 10_000L

    /** 「준비 중」 안내가 보였다 사라지기까지(밀리초) — 스펙 app-shell 2-5. */
    const val NOT_READY_NOTICE_MILLIS: Long = 2_000L

    /** 경과 표시 단위(밀리초) — 분 단위로만 바뀜(스펙 D6). */
    const val ELAPSED_UNIT_MILLIS: Long = 60_000L

    /** 화면이 안 보인 뒤 uiState 구독을 멈추기까지(밀리초) — 회전 · 창 크기 변경 때 다시 시작하지 않을 만큼. */
    const val UI_STATE_STOP_TIMEOUT_MILLIS: Long = 5_000L

    /** 네트워크 타임아웃(초). */
    const val NETWORK_TIMEOUT_SECONDS: Long = 20L

    /**
     * Gleo Interface function 이름.
     * `app/src/main/assets/schema.json` 의 `actionCalls[].function.name` · `AndroidManifest.xml` 의
     * `<action>` 과 **글자가 같아야 함** — 셋 다 문자열이라 빌드가 잡아 주지 않음.
     */
    const val GLEO_FUNCTION_OPEN: String = "OPEN_TRAVEL_CRUMBS"
    const val GLEO_FUNCTION_RECORD: String = "RECORD_CURRENT_PLACE"

    /** 공식 7초 제한보다 이른 응답 시도 목표. 전달 지연까지 포함한 전체 시간 보장 수치가 아님. */
    const val GLEO_RESPONSE_TARGET_MILLIS: Long = 6_000L

    /** 무파라미터 Action의 외부 입력 크기 한도. */
    const val GLEO_MAX_ACTION_PARAM_CHARS: Int = 1_024

    /** Gleo 에 돌려줄 문장(영어 완전한 문장) — 화면 문구가 아니라 프로토콜 값이라 strings.xml 이 아님. */
    const val GLEO_OPEN_SUCCESS_MESSAGE: String = "Travel Crumbs is now open on the screen."
    const val GLEO_OPEN_FAIL_MESSAGE: String = "Travel Crumbs could not read the request."
    const val GLEO_RECORD_SUCCESS_MESSAGE: String = "The current place has been recorded in the active trip."
    const val GLEO_RECORD_INPUT_FAIL_MESSAGE: String = "The recording request could not be read."
    const val GLEO_RECORD_NOT_READY_MESSAGE: String = "Travel Crumbs is not ready to record a place yet."
    const val GLEO_RECORD_CONSENT_MESSAGE: String = "Location consent is required before recording a place."
    const val GLEO_RECORD_UNCONFIRMED_MESSAGE: String = "The app could not confirm that the place was recorded."
    const val GLEO_RECORD_TIMEOUT_MESSAGE: String =
        "The recording result could not be confirmed in time. Check the trip before trying again."
}
