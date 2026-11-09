package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.consent.ConsentRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.trip.TripRestoreStatus
import com.example.travelcrumbs.domain.model.ConsentType
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** 사전 조건을 확인하고 앱 시작 때 이미 진행 중인 여행 복원만 기다림. */
fun interface GleoRecordingReadiness {
    suspend fun check(onRestoringTrip: (Boolean) -> Unit): GleoReadiness
}

enum class GleoReadiness { READY, NOT_READY, CONSENT_REQUIRED, UNCONFIRMED }

class DefaultGleoRecordingReadiness @Inject constructor(
    private val tripRepository: TripRepository,
    private val naviRepository: NaviRepository,
    private val consentRepository: ConsentRepository,
) : GleoRecordingReadiness {

    override suspend fun check(onRestoringTrip: (Boolean) -> Unit): GleoReadiness {
        val restoring = tripRepository.activeTrip.value == null &&
            tripRepository.startupRestoreStatus.value == TripRestoreStatus.PENDING
        if (restoring) {
            onRestoringTrip(true)
            combine(tripRepository.activeTrip, tripRepository.startupRestoreStatus) { trip, status ->
                trip != null || status != TripRestoreStatus.PENDING
            }.first { it }
            onRestoringTrip(false)
        }
        val trip = tripRepository.activeTrip.value ?: return when (tripRepository.startupRestoreStatus.value) {
            TripRestoreStatus.COMPLETED -> GleoReadiness.NOT_READY
            else -> GleoReadiness.UNCONFIRMED
        }
        if (trip.statusCode !in RECORDABLE_STATUSES) return GleoReadiness.NOT_READY
        if (restoring) naviRepository.ready.first { it }
        if (!naviRepository.ready.value) return GleoReadiness.NOT_READY
        val agreed = consentRepository.isAgreed(ConsentType.LOCATION_COLLECT).getOrElse { error ->
            if (error is CancellationException) throw error
            return GleoReadiness.UNCONFIRMED
        }
        if (!agreed) return GleoReadiness.CONSENT_REQUIRED
        // 동의를 기다리는 동안 여행이 끝나거나 바뀌었으면 기존 UseCase를 시작하지 않음.
        val current = tripRepository.activeTrip.value
        return if (current != null && current.tripUuid == trip.tripUuid && current.statusCode in RECORDABLE_STATUSES && naviRepository.ready.value) {
            GleoReadiness.READY
        } else {
            GleoReadiness.NOT_READY
        }
    }

    private companion object {
        val RECORDABLE_STATUSES = setOf("RECORDING", "PAUSED")
    }
}
