package com.example.travelcrumbs.data

import com.example.travelcrumbs.data.llm.LlmDataSource
import com.example.travelcrumbs.data.navi.NaviInfoDataSource
import com.example.travelcrumbs.data.vehicle.VehicleGearDataSource
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pleos SDK 3종의 수명 한 곳 — initialize() 한 번 · release() 한 번.
 * MainActivity 가 onCreate · onDestroy 에서 이 둘만 부름.
 */
@Singleton
class PleosSdkSession @Inject constructor(
    private val vehicle: VehicleGearDataSource,
    private val navi: NaviInfoDataSource,
    private val llm: LlmDataSource,
) {
    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        vehicle.initialize()
        navi.initialize()
        llm.initialize()
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        vehicle.release()
        navi.release()
        llm.release()
    }
}
