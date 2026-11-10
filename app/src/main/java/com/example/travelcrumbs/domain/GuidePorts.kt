package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.RouteRequestResult
import kotlinx.coroutines.flow.StateFlow

/**
 * 길안내 요청 입구([GuideRequestTracker])가 내비에 닿는 길 — data 의 NaviRepository 가 구현(domain 은 data 를 모름 · 규칙 1).
 * SDK 타입은 없음(규칙 7).
 */
interface NaviGuidance {
    /** 내비 서비스에 붙었는지. */
    val ready: StateFlow<Boolean>

    /** 그 장소로 길안내 요청 — 시작 · 오류 알림이 올 때까지 기다림. 한도 · 연결 끊김은 부르는 쪽(awaitRouteResult). */
    suspend fun requestRoute(place: Place): RouteRequestResult

    /** 지금 안내 상태(안내 중 · 목적지) — 시작 알림을 놓쳤을 때 대조(R1). 실패는 예외. */
    suspend fun activeRoute(): NaviActiveRoute
}

/** 안내가 시작됐을 때 핀을 남기는 길 — data 의 PinRepository 가 구현(기록 중일 때만 남기는 규칙은 구현 쪽 · U42). */
interface GuidanceStartRecorder {
    /** 핀을 남겼으면 참. */
    suspend fun recordGuidanceStart(placeId: String): Boolean
}
