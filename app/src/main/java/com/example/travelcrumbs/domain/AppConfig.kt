package com.example.travelcrumbs.domain

/** 바뀔 수치 · 고정 문자열 한 곳. */
object AppConfig {
    /** SDK 호출 응답을 기다리는 한도(밀리초). */
    const val SDK_CALL_TIMEOUT_MS: Long = 10_000L

    /** 내비 앱 전환 뒤 경로 탐색 시작 콜백을 기다리는 한도. */
    const val ROUTE_START_TIMEOUT_MILLIS: Long = 30_000L

    /** 앱 재개 직후 늦은 콜백 처리를 기다린 다음 현재 목적지를 대조. */
    const val NAVI_QUERY_SETTLE_DELAY_MILLIS: Long = 1_500L
    const val ROUTE_MATCH_RADIUS_METERS: Double = 100.0

    /** 「준비 중」 안내가 보였다 사라지기까지(밀리초) — 스펙 app-shell 2-5. */
    const val NOT_READY_NOTICE_MILLIS: Long = 2_000L

    /** 경과 표시 단위(밀리초) — 분 단위로만 바뀜(스펙 D6). */
    const val ELAPSED_UNIT_MILLIS: Long = 60_000L

    /** 화면이 안 보인 뒤 uiState 구독을 멈추기까지(밀리초) — 회전 · 창 크기 변경 때 다시 시작하지 않을 만큼. */
    const val UI_STATE_STOP_TIMEOUT_MILLIS: Long = 5_000L

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
