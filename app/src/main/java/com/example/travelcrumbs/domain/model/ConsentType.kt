package com.example.travelcrumbs.domain.model

/** 동의유형코드(CONSENT_TYPE · 표준정의서 v0.35 코드) — [code] 는 user_consent_hist.consent_type_cd 값. */
enum class ConsentType(val code: String) {
    /** 위치 수집 — 첫 여행 시작 시(A-03-1 · R14). */
    LOCATION_COLLECT("LOCATION_COLLECT"),
}
