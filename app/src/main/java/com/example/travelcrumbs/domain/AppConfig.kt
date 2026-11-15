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

    /**
     * 두 좌표를 「같은 곳」으로 보는 거리(미터 · withinMatchRadius 한 곳에서만 씀) — 시작 알림을 놓쳤을 때 내비 목적지와 요청 장소 대조(R1) ·
     * 「이 앱 요청」 판정 · 자동 기록(#66)의 지나침 · 확정 중복(현재 여행 장소 · 앞선 확정) · 수동 기록 직전 대기 정리.
     */
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
    const val DEFAULT_NICKNAME_NM: String = "서윤맘"

    /** v1.7 사용자 입력 길이 — Kotlin [String.length] 기준. */
    const val NICKNAME_MIN_LENGTH: Int = 2
    const val NICKNAME_MAX_LENGTH: Int = 12
    const val TRIP_NAME_MIN_LENGTH: Int = 1
    const val TRIP_NAME_MAX_LENGTH: Int = 30
    const val PLACE_NAME_MAX_LENGTH: Int = 60
    const val MEMO_MAX_LENGTH: Int = 200

    /**
     * 내 공개 여행이 추천 로드맵에 오를 때 쓰는 번호 = 이 값 + trip_no(#51 U6 · U7) — 시드 로드맵 번호(101 ~ 301)와 겹치지 않게.
     * 별점 기록(기기 안)도 이 번호로 남음. 서버 추천 API 가 생기면 서버 번호로 바뀜.
     */
    const val LOCAL_TRIP_RECO_NO_OFFSET: Long = 1_000_000L

    /**
     * 홈 「내 근처 리뷰 많은 로드맵」(#55 · 스펙 home-nearby 3-3 · N1 · N8) — 근처 반경(km · 경계 포함) · 카드 최대 장수.
     * 반경 안 로드맵이 없으면 같은 정렬로 전국 대체(N2).
     */
    const val NEARBY_ROADMAP_RADIUS_KM: Int = 10
    const val NEARBY_ROADMAP_MAX_CNT: Int = 10

    /** 홈 근처 로드맵 — 마지막 위치 성공 뒤 이 시간 안의 재진입은 위치를 다시 묻지 않음(N7 · PR #64 리뷰 · 동의 · 묶음 변화 · 내비 재연결은 바로). */
    const val NEARBY_RELOOKUP_MIN_INTERVAL_MILLIS: Long = 5 * 60_000L

    /** 네트워크 타임아웃(초). */
    const val NETWORK_TIMEOUT_SECONDS: Long = 20L

    /**
     * Gleo Interface function 이름.
     * 빌드 타입별 `assets/schema.json` 의 `actionCalls[].function.name` · `AndroidManifest.xml` 의
     * `<action>` 과 **글자가 같아야 함** — 셋 다 문자열이라 빌드가 잡아 주지 않음.
     */
    const val GLEO_FUNCTION_RECORD: String = "RECORD_CURRENT_PLACE"
    const val GLEO_FUNCTION_CONTROL: String = "CONTROL_CURRENT_TRIP"
    const val GLEO_FUNCTION_INFO: String = "SHOW_TRAVEL_INFO"

    /** Gleo 함수 파라미터(#69 스펙 4.1) — schema.json 의 properties 이름 · enum 값과 글자가 같아야 함. */
    const val GLEO_PARAM_OPERATION: String = "operation"
    const val GLEO_PARAM_INFO_TYPE: String = "infoType"
    const val GLEO_PARAM_REGION: String = "region"
    const val GLEO_MAX_REGION_CHARS: Int = 50

    /** CONTROL_CURRENT_TRIP 의 operation · SHOW_TRAVEL_INFO 의 infoType enum 값(schema.json 과 같은 글자). */
    const val GLEO_OP_PAUSE: String = "PAUSE"
    const val GLEO_OP_RESUME: String = "RESUME"
    const val GLEO_OP_END: String = "END"
    const val GLEO_INFO_TODAY_RECORDS: String = "TODAY_RECORDS"
    const val GLEO_INFO_RECOMMENDED_ROADMAPS: String = "RECOMMENDED_ROADMAPS"

    /** 응답 문장에 다시 쓰는 사용자 지역 말의 최대 글자(글자 · 숫자 · 공백만 남긴 뒤). */
    const val GLEO_REGION_ECHO_CHARS: Int = 20

    /**
     * 콜백을 받는 Gleo 앱 패키지 — 암묵적 broadcast 로 다른 앱이 여행 정보를 받지 않도록 고정(Pleos 리뷰 #69).
     * 에뮬레이터 Pleos Connect v2.1.0 로그로 확인(processAppActionResponse · ai.umos.ambientai). 실기에서 다시 확인.
     */
    const val GLEO_PACKAGE: String = "ai.umos.ambientai"

    /** Gleo 화면 요청(추천 · 내 여행 · 종료 확인)을 따를 유효 시간 — 늦게 만들어진 화면이 지난 명령을 다시 따르지 않게(밀리초). */
    const val GLEO_SCREEN_REQUEST_FRESH_MILLIS: Long = 10_000L

    /** 추천 로드맵을 말해 줄 개수 · 현재 위치를 기다리는 앱 안 한도(밀리초 · 7초 응답 예산 안). */
    const val GLEO_RECOMMENDATION_COUNT: Int = 3
    const val GLEO_LOCATION_WAIT_MILLIS: Long = 3_000L

    /**
     * 실제 Gleo 가 Intent action 앞에 붙이는 접두어(schema.json 의 packageName + ".").
     * 공식 예제는 접두어 없는 함수명이라 두 꼴을 모두 받음 — 매니페스트 intent-filter 도 둘 다 선언.
     */
    const val GLEO_ACTION_PREFIX: String = "com.example.travelcrumbs."

    /** 앱이 Action을 수신한 뒤 응답을 시도하는 기한. 실제 Gleo의 전체 경과와 별도. */
    const val GLEO_RESPONSE_TARGET_MILLIS: Long = 7_000L

    /** 무파라미터 Action의 외부 입력 크기 한도. */
    const val GLEO_MAX_ACTION_PARAM_CHARS: Int = 1_024

    /** Gleo 에 돌려줄 문장(영어 완전한 문장) — 화면 문구가 아니라 프로토콜 값이라 strings.xml 이 아님. */
    const val GLEO_INPUT_FAIL_MESSAGE: String = "Travel Crumbs could not read the request."
    const val GLEO_BUSY_MESSAGE: String = "Another Travel Crumbs request is still being processed."
    const val GLEO_TIMEOUT_MESSAGE: String = "Travel Crumbs could not answer in time."
    const val GLEO_NO_TRIP_MESSAGE: String = "There is no trip in progress."
    const val GLEO_PAUSED_MESSAGE: String = "The trip recording is paused."
    const val GLEO_ALREADY_PAUSED_MESSAGE: String = "The trip recording is already paused."
    const val GLEO_RESUMED_MESSAGE: String = "The trip recording has resumed."
    const val GLEO_ALREADY_RUNNING_MESSAGE: String = "The trip recording is already running."
    const val GLEO_END_CONFIRM_MESSAGE: String = "Please confirm ending the trip on the screen."
    const val GLEO_CONTROL_FAIL_MESSAGE: String = "The trip could not be changed."
    const val GLEO_INFO_FAIL_MESSAGE: String = "Travel Crumbs could not read the travel information."
    const val GLEO_NO_ROADMAPS_MESSAGE: String = "There are no recommended roadmaps yet."
    /** %s = 사용자가 말한 지역(정리한 것). */
    const val GLEO_NO_REGION_ROADMAPS_MESSAGE: String = "There are no recommended roadmaps for %s yet."
    /** %1$d = 오늘 장소 수 · %2$s = place/places · %3$s = 지금 여행 문장. */
    const val GLEO_TODAY_MESSAGE: String = "You recorded %1${'$'}d %2${'$'}s today. %3${'$'}s"
    /** %1$s = recording/paused · %2$d = 장소 수 · %3$s = place/places. */
    const val GLEO_CURRENT_TRIP_MESSAGE: String = "The current trip is %1${'$'}s with %2${'$'}d %3${'$'}s."
    /** %s = 로드맵 이름들. */
    const val GLEO_TOP_ROADMAPS_MESSAGE: String = "Top recommended roadmaps: %s."
    /** %1$s = 지역 · %2$s = 로드맵 이름들. */
    const val GLEO_TOP_REGION_ROADMAPS_MESSAGE: String = "Top recommended roadmaps in %1${'$'}s: %2${'$'}s."
    const val GLEO_RECORD_SUCCESS_MESSAGE: String = "The current place has been recorded in the active trip."
    const val GLEO_RECORD_INPUT_FAIL_MESSAGE: String = "The recording request could not be read."
    const val GLEO_RECORD_NOT_READY_MESSAGE: String = "Travel Crumbs is not ready to record a place yet."
    const val GLEO_RECORD_CONSENT_MESSAGE: String = "Location consent is required before recording a place."
    const val GLEO_RECORD_FAIL_MESSAGE: String = "The place could not be recorded."
    const val GLEO_RECORD_UNCONFIRMED_MESSAGE: String = "The app could not confirm that the place was recorded."
    const val GLEO_RECORD_TIMEOUT_MESSAGE: String =
        "The recording result could not be confirmed in time. Check the trip before trying again."

    /** 추천 지도(#59) 첫 맞춤 반경(킬로미터 · N3) — 내 위치 중심 이 반경이 화면에 다 들어오게. 위치 사용(#55 문구 반영 뒤) 때 씀. */
    const val RECO_MAP_FIRST_FIT_RADIUS_KM: Double = 10.0

    /** 위도 1도의 거리(킬로미터 · 지구 평균 반지름 6371.0 km 기준) — 반경 → 지도 범위 계산. */
    const val KM_PER_DEGREE_LAT: Double = 111.195

    /** 추천 지도 위치 조회 시간 제한(밀리초) — 넘으면 「위치 없음」. 위치 사용(#55 문구 반영 뒤) 때 씀. */
    const val RECO_MAP_LOCATION_TIMEOUT_MILLIS: Long = 10_000L

    /** 추천 지도 한 화면 표시(낱개 핀 + 묶음 핀) 최대 수(N6). */
    const val RECO_MAP_MARKER_LIMIT: Int = 50

    /** 화면에서 이 거리(dp) 안에 겹치는 핀은 숫자 묶음 핀 하나로(N6). */
    const val RECO_MAP_CLUSTER_RADIUS_DP: Float = 40f

    /** 지도 확대 범위 — 자산은 확대 단계 12까지 · 화면은 15까지 늘려 보임(N8 결정 2026-11-10). */
    const val RECO_MAP_MIN_ZOOM: Double = 5.0
    const val RECO_MAP_MAX_ZOOM: Double = 15.0
}
