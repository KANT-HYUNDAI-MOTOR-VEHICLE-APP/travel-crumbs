package com.example.travelcrumbs.domain

/** 벽시계 — 테스트가 시계를 바꿔 끼울 수 있게 한 곳으로. */
fun interface TimeSource {
    fun nowMillis(): Long
}
