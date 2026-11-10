package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.model.ConsentType
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 사전 조건을 확인하고 앱 시작 때 기기 안 여행 상태를 처음 읽는 동안만 기다림(#25 U3). */
fun interface GleoRecordingReadiness {
    suspend fun check(onRestoringTrip: (Boolean) -> Unit): GleoReadiness
}

enum class GleoReadiness { READY, NOT_READY, CONSENT_REQUIRED, UNCONFIRMED }

/**
 * develop 의 기기 안(Room) 기록 구조에 맞춘 준비 확인 — 여행 · 동의 · 내비를 「이 장소 기록」 버튼과 같은 출처에서 읽음.
 * 여행 원본 = [RecordingRepository.progress] · 동의 = [CurrentUserRepository.consents] · 내비 = [NaviRepository.ready].
 * 전체 응답 기한(7초)은 [GleoCommandProcessor] 가 지킴 — 여기서는 기다림마다 짧은 한도만 둠.
 */
class DefaultGleoRecordingReadiness @Inject constructor(
    private val recording: RecordingRepository,
    private val users: CurrentUserRepository,
    private val naviRepository: NaviRepository,
) : GleoRecordingReadiness {

    override suspend fun check(onRestoringTrip: (Boolean) -> Unit): GleoReadiness {
        val progress = try {
            // 콜드 스타트에서 기기 안 여행을 처음 읽는 데 시간이 걸리면 그동안만 「불러오는 중」 표시.
            withTimeoutOrNull(RESTORE_NOTICE_AFTER_MILLIS) { recording.progress.first() }
                ?: run {
                    onRestoringTrip(true)
                    recording.progress.first().also { onRestoringTrip(false) }
                }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            return GleoReadiness.UNCONFIRMED
        }
        val tripNo = progress.tripNo ?: return GleoReadiness.NOT_READY
        if (!progress.status.inProgress) return GleoReadiness.NOT_READY
        // 앱이 막 켜졌으면 내비 연결을 잠깐 기다림 — 끝내 안 되면 확인된 준비 미충족.
        if (!naviRepository.ready.value) {
            withTimeoutOrNull(NAVI_READY_WAIT_MILLIS) { naviRepository.ready.first { it } }
                ?: return GleoReadiness.NOT_READY
        }
        val agreed = try {
            users.consents.first()[ConsentType.LOCATION_COLLECT]?.agreed == true
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            return GleoReadiness.UNCONFIRMED
        }
        if (!agreed) return GleoReadiness.CONSENT_REQUIRED
        // 기다리는 동안 여행이 끝나거나 바뀌었으면 기존 기록 기능을 시작하지 않음.
        val current = recording.progress.first()
        return if (current.tripNo == tripNo && current.status.inProgress && naviRepository.ready.value) {
            GleoReadiness.READY
        } else {
            GleoReadiness.NOT_READY
        }
    }

    private companion object {
        const val RESTORE_NOTICE_AFTER_MILLIS = 300L
        const val NAVI_READY_WAIT_MILLIS = 3_000L
    }
}
