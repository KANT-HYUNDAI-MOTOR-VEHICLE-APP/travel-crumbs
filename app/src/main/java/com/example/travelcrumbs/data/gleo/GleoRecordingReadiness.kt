package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.consent.ConsentRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.model.ConsentType
import javax.inject.Inject

/** Gleo 진입의 사전 조건만 확인. 위치 조회·현재 여행 복원·장소 저장은 수행하지 않음. */
fun interface GleoRecordingReadiness {
    suspend fun check(): GleoReadiness
}

enum class GleoReadiness { READY, NOT_READY, CONSENT_REQUIRED }

class DefaultGleoRecordingReadiness @Inject constructor(
    private val tripRepository: TripRepository,
    private val naviRepository: NaviRepository,
    private val consentRepository: ConsentRepository,
) : GleoRecordingReadiness {

    override suspend fun check(): GleoReadiness {
        // 초기 null은 복원 중일 수도 있음. 조회를 한 번 더 하거나 '여행 없음'으로 단정하지 않음.
        val trip = tripRepository.activeTrip.value ?: return GleoReadiness.NOT_READY
        if (trip.statusCode !in RECORDABLE_STATUSES || !naviRepository.ready.value) return GleoReadiness.NOT_READY
        val agreed = consentRepository.isAgreed(ConsentType.LOCATION_COLLECT).getOrNull()
            ?: return GleoReadiness.NOT_READY
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
