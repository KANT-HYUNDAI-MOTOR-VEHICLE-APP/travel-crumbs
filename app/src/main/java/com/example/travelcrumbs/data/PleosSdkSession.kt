package com.example.travelcrumbs.data

import com.example.travelcrumbs.data.autorecord.AutoPlaceRecorder
import com.example.travelcrumbs.data.navi.NaviInfoDataSource
import com.example.travelcrumbs.data.vehicle.VehicleGearDataSource
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pleos SDK 의 수명 한 곳 — initialize() 한 번 · release() 한 번.
 * MainActivity 가 onCreate · onDestroy 에서 이 둘만 부름.
 * 내비 목적지 자동 기록(#66)의 알림 · 주행 판정 구독도 이 세션 동안만(SDK 가 살아 있는 동안 · F23).
 *
 * LLM SDK 는 Playground v2.6.0(2026-10-19) 「Removed Features」로 제공이 끝나 빠짐.
 * Gleo AI 연동은 Gleo Interface(data/gleo) 가 맡음 — SDK 가 아니라 Intent · Broadcast 라 수명이 없음.
 */
@Singleton
class PleosSdkSession @Inject constructor(
    private val vehicle: VehicleGearDataSource,
    private val navi: NaviInfoDataSource,
    private val autoRecorder: AutoPlaceRecorder,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        vehicle.initialize()
        navi.initialize()
        autoRecorder.attach()
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        autoRecorder.detach()
        vehicle.release()
        navi.release()
    }
}
