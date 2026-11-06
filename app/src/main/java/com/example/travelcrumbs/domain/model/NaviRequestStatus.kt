package com.example.travelcrumbs.domain.model

/**
 * 길안내 요청 상태 코드 — 데이터 표준 v0.3 코드 그룹 `NAVI_REQ_STATUS`(R10 「앱 로직과 연결된 상태 코드는 앱 상수로도 정의」 ·
 * 스펙 navigation 4.1). 앱 안에서 이 코드값을 두는 곳은 여기 한 곳(android-architecture 규칙 6).
 *
 * [code] 는 표준 가안 글자 그대로 — 코드값 형식(M01 · 영문 대문자 / 숫자)이 정해지면 이 파일의 값만 고침(U59).
 * 요청이 없는 때(화면 상태 「대기」)는 코드가 없음 · 실패 이유(앱 안 값 · [RouteFailReason])는 표준 코드 그룹이 아님.
 */
enum class NaviRequestStatus(val code: String) {
    /** NaviHelper 호출 뒤 결과 대기 — 「다시 시도」도 새 요청. */
    REQUESTING("REQUESTING"),

    /** 차량 길안내 시작됨 — onRouteStarted 또는 내비 상태 대조(결정 기록 U4 넓힘). */
    SUCCESS("SUCCESS"),

    /** 호출 실패(더미 경로 폴백) — 「다시 시도」 · 「더미 경로」는 이 상태에서만. */
    FAILED("FAILED"),
}
