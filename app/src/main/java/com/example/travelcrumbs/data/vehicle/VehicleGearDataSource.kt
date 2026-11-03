package com.example.travelcrumbs.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import android.content.Context
import android.util.Log
import com.example.travelcrumbs.domain.model.Gear
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Pleos Vehicle SDK 를 감싸는 유일한 파일 — `ai.pleos.*` 타입은 이 패키지 밖으로 나가지 않음.
 * initialize() 한 번 · release() 한 번은 PleosSdkSession 이 부름.
 */
@Singleton
class VehicleGearDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val vehicle = Vehicle(context.applicationContext)
    private val initialized = AtomicBoolean(false)

    fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            runCatching { vehicle.initialize() }
                .onFailure { Log.w(TAG, "Vehicle initialize 실패: ${it.javaClass.simpleName}") }
        }
    }

    fun release() {
        if (initialized.compareAndSet(true, false)) {
            runCatching { vehicle.release() }
                .onFailure { Log.w(TAG, "Vehicle release 실패: ${it.javaClass.simpleName}") }
        }
    }

    /** 기어 변화 — 첫 값은 조회로, 뒤는 콜백으로. */
    fun gearFlow(): Flow<Gear> = callbackFlow {
        val gearApi = vehicle.getGear()
        val listener = object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) {
                trySend(gear.toDomain())
            }

            override fun onFailed(exception: Exception) {
                Log.w(TAG, "기어 콜백 실패: ${exception.javaClass.simpleName}")
                trySend(Gear.UNKNOWN)
            }
        }

        runCatching { gearApi.registerGearSelection(listener) }
            .onFailure {
                Log.w(TAG, "기어 콜백 등록 실패: ${it.javaClass.simpleName}")
                trySend(Gear.UNKNOWN)
            }

        // 서비스 바인딩이 initialize() 뒤에 비동기로 끝나므로 첫 조회는 몇 번 다시 시도
        launch {
            repeat(FIRST_READ_ATTEMPTS) { attempt ->
                var done = false
                runCatching {
                    gearApi.getCurrentGearSelectionState(
                        { gear ->
                            done = true
                            trySend(gear.toDomain())
                        },
                        { exception ->
                            Log.w(
                                TAG,
                                "기어 조회 실패(${attempt + 1}): ${exception.javaClass.simpleName}",
                            )
                        },
                    )
                }.onFailure { Log.w(TAG, "기어 조회 호출 실패: ${it.javaClass.simpleName}") }
                delay(FIRST_READ_INTERVAL_MS)
                if (done) return@launch
            }
        }

        awaitClose {
            runCatching { gearApi.unregisterGearSelection(listener) }
        }
    }

    private fun VehicleGear?.toDomain(): Gear = when (this) {
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        VehicleGear.GEAR_DRIVE -> Gear.DRIVE
        else -> Gear.UNKNOWN
    }

    private companion object {
        const val TAG = "VehicleGearDataSource"
        const val FIRST_READ_ATTEMPTS = 10
        const val FIRST_READ_INTERVAL_MS = 500L
    }
}
