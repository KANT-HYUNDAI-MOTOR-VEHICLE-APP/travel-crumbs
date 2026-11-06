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

    /**
     * 길안내 요청 뒤 시작 · 오류 알림을 기다리는 한도(밀리초 · 스펙 navigation 3.2 T14).
     * 스펙은 [SDK_CALL_TIMEOUT_MS](10초) 재사용이지만 에뮬레이터 실측(2026-11-05)에서 onRouteStarted 가
     * 요청 뒤 약 11.5초(경로 탐색 약 3초 + 내비 경로 선택 화면의 자동 시작 카운트다운 약 8초)에 와 10초면 늘 TIMEOUT —
     * 따로 둠 · 값은 사람 확정 전 임시(builder 보고서 「스펙 밖」).
     */
    const val ROUTE_START_TIMEOUT_MILLIS: Long = 30_000L

    /**
     * 대조 조회 전 기다림(밀리초 · R1) — 앱이 얼림에서 풀리면 그동안 쌓인 내비 콜백(진짜 onRouteStarted 나
     * 다른 앱 조회에 대한 옛 onRouteStateInfo)이 한꺼번에 옴. 에뮬레이터 실측: 그 사이에 조회하면 옛 값(안내 중 아님)을 받음.
     */
    const val NAVI_QUERY_SETTLE_DELAY_MILLIS: Long = 1_500L

    /** 시작 알림을 놓쳤을 때 내비 목적지와 요청 장소를 같다고 보는 거리(미터 · R1). */
    const val ROUTE_MATCH_RADIUS_METERS: Double = 100.0

    /** 「가 볼 장소」 고정 최대 개수(스펙 navigation 결정 기록 U16). */
    const val PINNED_PLACES_MAX: Int = 3

    /** 핀을 남길 때 기록 상태(#6 RecordingState)를 기다리는 한도(밀리초) — 넘으면 「못 읽음」 → 핀 없음(U42). */
    const val RECORDING_STATUS_READ_TIMEOUT_MILLIS: Long = 2_000L

    /** 핀 다시 보내기 첫 간격 · 최대 간격(밀리초) — 30초부터 두 배 · 최대 1시간(결정 기록 U30). */
    const val PIN_UPLOAD_RETRY_INITIAL_MILLIS: Long = 30_000L
    const val PIN_UPLOAD_RETRY_MAX_MILLIS: Long = 3_600_000L

    /** debug 진단 카드 「안내 건수」 한 줄에 보일 장소 수(스펙 navigation 4.6 · U41). */
    const val PLACE_STATS_CARD_LIMIT: Int = 3

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
