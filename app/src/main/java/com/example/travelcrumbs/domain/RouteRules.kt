package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.RoutePoint
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 경로 기록(A-05) 규칙 — 차가 움직였을 때만 경로점을 남김.
 * 멈춘 동안은 점이 생기지 않으므로 「같은 위치에서 다음 점까지 걸린 시간 = 멈춘 시간」으로 나중에 체류 시간을 계산할 수 있음.
 */
object RouteRules {
    /** 내비에 위치를 묻는 간격(밀리초). */
    const val POLL_MILLIS: Long = 5_000L

    /** 이만큼(미터) 이상 움직였을 때만 새 경로점. */
    const val MIN_MOVE_METERS: Double = 20.0

    /** 이만큼 모이면 서버로 보냄. */
    const val BATCH_SIZE: Int = 6

    /** 덜 모여도 이 시간(밀리초)이 지나면 보냄. */
    const val FLUSH_MILLIS: Long = 60_000L

    /** 서버에 못 보낸 점을 기기에 들고 있는 최대 개수 — 넘으면 오래된 것부터 버림. */
    const val MAX_PENDING: Int = 500

    fun shouldKeep(last: RoutePoint?, next: RoutePoint): Boolean =
        last == null || distanceMeters(last, next) >= MIN_MOVE_METERS

    /** 두 점 사이 거리(미터 · 하버사인). */
    fun distanceMeters(a: RoutePoint, b: RoutePoint): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }
}
