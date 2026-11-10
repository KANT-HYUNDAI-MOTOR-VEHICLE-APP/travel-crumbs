package com.example.travelcrumbs.domain

import java.time.ZoneId

/** 벽시계 — 테스트가 시계 · 시간대를 바꿔 끼울 수 있게 한 곳으로. */
fun interface TimeSource {
    fun nowMillis(): Long

    /** 날짜 표시(「11월 9일 안내」)에 쓰는 시간대 — 기본은 기기 설정. */
    fun zoneId(): ZoneId = ZoneId.systemDefault()
}
