package com.example.travelcrumbs.data.vehicle

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import com.example.travelcrumbs.domain.model.UxRestriction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * AAOS 주행 규제(CarUxRestrictionsManager)를 감싸는 유일한 파일 — `android.car.*` 타입은 이 패키지 밖으로 안 나감.
 * 권한 없이 읽힘. 서비스에 닿지 못하면 UNKNOWN 하나를 내보냄 → 판정은 기어만으로(스펙 app-shell 3-1).
 * 문서: https://developer.android.com/reference/android/car/drivingstate/CarUxRestrictionsManager
 */
@Singleton
class UxRestrictionsDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext

    fun restrictionFlow(): Flow<UxRestriction> = callbackFlow {
        val car = runCatching { Car.createCar(appContext) }
            .onFailure { Log.w(TAG, "Car 연결 실패: ${it.javaClass.simpleName}") }
            .getOrNull()
        val manager = car?.let {
            runCatching { it.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager }
                .onFailure { e -> Log.w(TAG, "규제 서비스 없음: ${e.javaClass.simpleName}") }
                .getOrNull()
        }

        if (manager == null) {
            trySend(UxRestriction.UNKNOWN)
        } else {
            val registered = runCatching {
                manager.registerListener { restrictions -> trySend(restrictions.toDomain()) }
            }.onFailure { Log.w(TAG, "규제 콜백 등록 실패: ${it.javaClass.simpleName}") }.isSuccess
            val current = runCatching { manager.currentCarUxRestrictions }.getOrNull()
            trySend(if (registered) current.toDomain() else UxRestriction.UNKNOWN)
        }

        awaitClose {
            runCatching { manager?.unregisterListener() }
            runCatching { car?.disconnect() }
        }
    }

    private fun CarUxRestrictions?.toDomain(): UxRestriction = when {
        this == null -> UxRestriction.UNKNOWN
        isRequiresDistractionOptimization -> UxRestriction.RESTRICTED
        else -> UxRestriction.UNRESTRICTED
    }

    private companion object {
        const val TAG = "UxRestrictionsDataSrc"
    }
}
