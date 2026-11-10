package com.example.travelcrumbs.data.navi

import java.util.concurrent.atomic.AtomicInteger

/**
 * 내비 상태 조회(get*Info)가 진행 중인 동안을 셈 — 내비는 `onError` 를 모든 구독자에게 보내므로, 조회 중에 온 오류는
 * 그 조회의 것일 수 있어 길안내 요청의 결과로 쓰지 않음(리뷰 P3). 조회가 끝나면 다시 인정.
 */
internal class NaviQueryWindow {
    private val inFlight = AtomicInteger(0)

    /** 조회 하나가 진행 중인가. */
    val open: Boolean get() = inFlight.get() > 0

    suspend fun <T> around(query: suspend () -> T): T {
        inFlight.incrementAndGet()
        try {
            return query()
        } finally {
            inFlight.decrementAndGet()
        }
    }
}
