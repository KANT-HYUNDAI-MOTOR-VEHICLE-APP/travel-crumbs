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

    /**
     * TIMEOUT 뒤 「바로 앞 요청」을 기억하는 기한(밀리초 · 스펙 3.2 · 심사 N1) — 이 안에 늦은 시작 알림 · 재개 대조가 맞으면 시작됨.
     * 지나면 리스너를 거두고 더 대조하지 않음.
     */
    const val TIMED_OUT_REQUEST_MEMORY_MILLIS: Long = 5 * 60_000L

    /** 시작 알림을 놓쳤을 때 내비 목적지와 요청 장소를 같다고 보는 거리(미터 · R1). */
    const val ROUTE_MATCH_RADIUS_METERS: Double = 100.0

    /**
     * 내비 목적지 자동 장소 기록(#66 스펙 3.8 「수치」) — 기록 대기 최대 건수 · 「PARKED 아직」 대기의 기한(도착 뒤 이 안에 PARKED 를 못 보면 지나친 것) ·
     * 모든 대기의 절대 만료(도착 시각 + 이 값) · 「이 앱 요청」 판정의 시작됨 뒤 창(3.6 (b) · N17). 값은 스펙 추천(N16 · N17) — 사람 확인.
     */
    const val AUTO_RECORD_PENDING_MAX_COUNT: Int = 5
    const val AUTO_RECORD_PARK_WAIT_MILLIS: Long = 10 * 60_000L
    const val AUTO_RECORD_PENDING_MAX_AGE_MILLIS: Long = 24 * 60 * 60_000L
    const val AUTO_RECORD_APP_REQUEST_WINDOW_MILLIS: Long = 60_000L

    /** 「가 볼 장소」 고정 최대 개수(스펙 navigation 결정 기록 U16). */
    const val PINNED_PLACES_MAX: Int = 3

    /** 핀을 남길 때 기록 상태(#6 RecordingState)를 기다리는 한도(밀리초) — 넘으면 「못 읽음」 → 핀 없음(U42). */
    const val RECORDING_STATUS_READ_TIMEOUT_MILLIS: Long = 2_000L

    /** 핀 다시 보내기 첫 간격 · 최대 간격(밀리초) — 30초부터 두 배 · 최대 1시간(결정 기록 U30). */
    const val PIN_UPLOAD_RETRY_INITIAL_MILLIS: Long = 30_000L
    const val PIN_UPLOAD_RETRY_MAX_MILLIS: Long = 3_600_000L

    /** debug 진단 카드 「안내 건수」 한 줄에 보일 장소 수(스펙 navigation 4.6 · U41). */
    const val PLACE_STATS_CARD_LIMIT: Int = 3
    /** 별점 범위(#41 B-06) — 별 1 ~ 5. */
    const val RATING_SCORE_MIN: Int = 1
    const val RATING_SCORE_MAX: Int = 5

    /** 주행 화면 「내비 안내」 카드가 내비 안내 상태를 다시 읽는 간격(밀리초 · #51 U0) — 화면이 앞에 있을 때만. */
    const val NAVI_STATUS_REFRESH_MILLIS: Long = 15_000L

    /**
     * 기기 안 기본 사용자(#51 U1) — 로그인 화면(C-01) 전까지 한 명. 로그인 담당이 CurrentUserRepository 구현을 바꾸면 안 씀.
     * 기본 닉네임은 화면 문구가 아니라 user_mst.nickname_nm 의 처음 값(데이터)이라 여기 둠 — C-03 에서 바꿈.
     */
    const val LOCAL_USER_NO: Long = 1L
    const val DEFAULT_NICKNAME_NM: String = "여행자"

    /** 입력 길이 한도(글자) — 닉네임 · 여행명 · 장소 이름은 표준 도메인 「명」 100 · 메모는 「메모」 500 안에서 화면에 맞게. */
    const val NICKNAME_MAX_LENGTH: Int = 20
    const val TRIP_NAME_MAX_LENGTH: Int = 40
    const val PLACE_NAME_MAX_LENGTH: Int = 60
    const val MEMO_MAX_LENGTH: Int = 500

    /**
     * 내 공개 여행이 추천 로드맵에 오를 때 쓰는 번호 = 이 값 + trip_no(#51 U6 · U7) — 시드 로드맵 번호(101 ~ 301)와 겹치지 않게.
     * 별점 기록(기기 안)도 이 번호로 남음. 서버 추천 API 가 생기면 서버 번호로 바뀜.
     */
    const val LOCAL_TRIP_RECO_NO_OFFSET: Long = 1_000_000L

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
