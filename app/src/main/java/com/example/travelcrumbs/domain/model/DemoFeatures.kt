package com.example.travelcrumbs.domain.model

/**
 * 빌드 타입에 따라 켜지는 시연 기능 — 값은 di 가 빌드 타입에서 정함(domain 은 빌드 설정을 모름).
 *
 * [dummyRoute] — 길안내 실패 폴백의 「더미 경로」(스펙 navigation E12 · E13) · debug 빌드에서만 참 ·
 * release 엔 「다시 시도」만(결정 U55 (a) · 2026-11-06 최성호).
 */
data class DemoFeatures(
    val dummyRoute: Boolean,
)
