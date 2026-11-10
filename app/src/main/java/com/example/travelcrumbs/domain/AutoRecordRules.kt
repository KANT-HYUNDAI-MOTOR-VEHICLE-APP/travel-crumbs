package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviActiveRoute
import com.example.travelcrumbs.domain.model.NaviDestination
import com.example.travelcrumbs.domain.model.PendingArrival
import com.example.travelcrumbs.domain.model.Place

/**
 * 내비 목적지 자동 장소 기록(#66)의 판정 — 전부 순수 계산(가상 시간 시험 · android 의존 없음 · 스펙 3.8 「PARKED → DRIVING 판정」).
 * 기한 · 개수 · 반경은 [AppConfig] 한 곳.
 */
object AutoRecordRules {

    /** 목적지 좌표를 쓸 수 있나 — 범위 밖 · (0, 0) 은 못 씀(3.3 「좌표 쓸 수 없음」). */
    fun usable(destination: NaviDestination): Boolean =
        destination.latitude in -90.0..90.0 &&
            destination.longitude in -180.0..180.0 &&
            !(destination.latitude == 0.0 && destination.longitude == 0.0)

    /** 도착 알림 이름과 진행 중 직접 안내 이름이 같나(앞뒤 공백 제거 · N8 (a)). */
    fun sameName(arrivedName: String, guideName: String): Boolean = arrivedName.trim() == guideName.trim()

    /**
     * 기한이 지난 대기를 버림 — 도착 시각 + 24시간(모든 대기 · 절대 만료) · 「PARKED 아직」이면 도착 + 10분([applyParkWait] 일 때만).
     * 앱 새로 시작 때는 10분 기한을 보지 않음(시동이 꺼졌다 켜졌으면 차가 섰던 것 · 3.4 「앱 새로 시작」).
     */
    fun purge(pendings: List<PendingArrival>, now: Long, applyParkWait: Boolean): List<PendingArrival> =
        pendings.filter { p ->
            val age = now - p.arrivedAt
            age < AppConfig.AUTO_RECORD_PENDING_MAX_AGE_MILLIS &&
                (!applyParkWait || p.parkedSeen || age < AppConfig.AUTO_RECORD_PARK_WAIT_MILLIS)
        }

    /** 「PARKED 봄」 표시 — PARKED 를 받은 때 · 앱 새로 시작 때. */
    fun markParked(pendings: List<PendingArrival>): List<PendingArrival> = pendings.map { it.copy(parkedSeen = true) }

    /** 대기 1건 추가 — 도착 순 · [AppConfig.AUTO_RECORD_PENDING_MAX_COUNT] 를 넘으면 가장 오래된 것부터 버림. */
    fun add(pendings: List<PendingArrival>, arrival: PendingArrival): List<PendingArrival> =
        (pendings + arrival).sortedBy { it.arrivedAt }.takeLast(AppConfig.AUTO_RECORD_PENDING_MAX_COUNT)

    /**
     * 새 직접 안내 시작 — 「PARKED 아직」인 대기 가운데 새 목적지와 반경 밖인 것은 지나친 것으로 버림 · 같은 목적지 재시작 · 재탐색은 유지(3.4).
     */
    fun dropPassed(pendings: List<PendingArrival>, newDestination: NaviDestination): List<PendingArrival> =
        pendings.filter { p ->
            p.parkedSeen || near(p.latitude, p.longitude, newDestination.latitude, newDestination.longitude)
        }

    /**
     * 확정 판정(3.4 표) — [candidates] 를 도착 순으로 보며 기존 장소([existing] 좌표) · 앞서 확정한 것과 반경 안이면 빼고
     * 남은 것(기록할 것)을 돌려줌.
     */
    fun toRecord(candidates: List<PendingArrival>, existing: List<Pair<Double, Double>>): List<PendingArrival> {
        val taken = existing.toMutableList()
        val result = mutableListOf<PendingArrival>()
        candidates.sortedBy { it.arrivedAt }.forEach { p ->
            if (taken.none { (lat, lng) -> near(p.latitude, p.longitude, lat, lng) }) {
                result += p
                taken += p.latitude to p.longitude
            }
        }
        return result
    }

    /**
     * 수동 「이 장소 기록」 직전(3.4 「수동」 행 · N9) — 수동 위치가 있으면 그 반경 안의 대기는 버리고(사람이 남긴 행이 그 목적지 기록) 나머지는 먼저 확정 후보.
     * 위치가 없으면 전부 확정 후보.
     */
    fun beforeManual(pendings: List<PendingArrival>, manualLat: Double?, manualLng: Double?): List<PendingArrival> {
        if (manualLat == null || manualLng == null) return pendings
        return pendings.filterNot { near(it.latitude, it.longitude, manualLat, manualLng) }
    }

    /**
     * 「이 앱 요청」인가(3.6) — (a) 내부 요청 칸의 장소와 같음 또는 (b) 마지막 시작됨 장소와 같고 그때부터 창
     * [AppConfig.AUTO_RECORD_APP_REQUEST_WINDOW_MILLIS] 안(경계 포함). 같음 = 기존 판정(이름 + 반경 · [isHeadingTo]).
     */
    fun isAppRequest(
        destination: NaviDestination,
        pendingRequest: Place?,
        lastStarted: Place?,
        lastStartedAt: Long?,
        now: Long,
    ): Boolean {
        val route = NaviActiveRoute(true, destination.name, destination.latitude, destination.longitude)
        if (pendingRequest != null && route.isHeadingTo(pendingRequest)) return true
        return lastStarted != null && lastStartedAt != null &&
            now - lastStartedAt <= AppConfig.AUTO_RECORD_APP_REQUEST_WINDOW_MILLIS &&
            route.isHeadingTo(lastStarted)
    }

    private fun near(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Boolean =
        distanceMeters(lat1, lng1, lat2, lng2) <= AppConfig.ROUTE_MATCH_RADIUS_METERS
}
