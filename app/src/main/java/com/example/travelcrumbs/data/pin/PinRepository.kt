package com.example.travelcrumbs.data.pin

import android.util.Log
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.remote.NavigationStartRemoteDataSource
import com.example.travelcrumbs.data.remote.SendOutcome
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.GuidanceStartRecorder
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.Pin
import com.example.travelcrumbs.domain.model.PinUploadState
import com.example.travelcrumbs.domain.model.RecordingStatus
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 핀(여행의 목적지 기록)의 단일 출처(스펙 navigation 4.1 「저장소 경계」 · 결정 기록 U45).
 * 핀을 실제 여행에 붙이는 일은 #3 이 이 인터페이스의 구현을 바꿔 끼워서 함 — 화면 · ViewModel · 도메인은 그대로.
 */
interface PinRepository : GuidanceStartRecorder {
    /** 현재 여행의 핀을 순서대로(1, 2, 3 …) — 보냄 상태와 무관 · 앱 재실행 뒤에도 같음. */
    val pins: Flow<List<Pin>>

    /**
     * 안내가 시작됨(onRouteStarted) — 기록 중(RECORDING)일 때만 핀 1건(U21 · U42) → 바로 보내기 1회(U30).
     * 일시정지 · 기록 상태를 못 읽음이면 아무것도 남기지 않음. 핀을 남겼으면 참.
     */
    override suspend fun recordGuidanceStart(placeId: String): Boolean

    /** 대기 핀을 서버로 보냄 — 다시 보낼 것이 남으면 [retryIndex] 번째 간격으로 예약. */
    suspend fun uploadPending(retryIndex: Int = 0): UploadRun
}

/** 대기 핀 보내기 한 바퀴의 끝. */
enum class UploadRun {
    /** 대기 핀이 없음(전부 끝 · 거절). */
    DONE,

    /** 네트워크 · 5xx 등 — 간격을 늘려 다시 예약함. */
    RETRY_SCHEDULED,

    /** 토큰 없음 · 401 · 403 — 대기로 둠 · 간격 예약 없음(다음 실행 · 다음 핀 · #1 갱신 때 다시). */
    WAITING_FOR_AUTH,
}

@Singleton
class DefaultPinRepository @Inject constructor(
    private val local: PinLocalDataSource,
    private val remote: NavigationStartRemoteDataSource,
    private val recordingRepository: RecordingRepository,
    private val timeSource: TimeSource,
    private val scheduler: PinUploadScheduler,
) : PinRepository {

    /** 한 프로세스 안에서 보내기는 한 번에 하나 — 즉시 보내기와 예약 작업이 겹쳐도 같은 핀을 두 번 보내지 않게. */
    private val uploadMutex = Mutex()

    override val pins: Flow<List<Pin>> = local.pins

    override suspend fun recordGuidanceStart(placeId: String): Boolean = try {
        record(placeId)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        // 로그엔 예외 이름만 — 장소 id · 시각을 남기지 않음(4.4)
        Log.w(TAG, "핀 남기기 실패: ${error.javaClass.simpleName}")
        throw error
    }

    private suspend fun record(placeId: String): Boolean {
        val status = withTimeoutOrNull(AppConfig.RECORDING_STATUS_READ_TIMEOUT_MILLIS) {
            recordingRepository.progress.map { it.status }.first()
        }
        if (status != RecordingStatus.RECORDING) return false
        val now = timeSource.nowMillis()
        local.append(placeId, now, offsetSecondsAt(now))
        uploadPending(retryIndex = 0)
        return true
    }

    override suspend fun uploadPending(retryIndex: Int): UploadRun = uploadMutex.withLock {
        for (pin in local.pending()) {
            when (remote.send(pin.placeId, pin.startedAtMillis, pin.utcOffsetSeconds)) {
                SendOutcome.ACCEPTED -> local.setState(pin.sequence, PinUploadState.SENT)
                SendOutcome.REJECTED -> {
                    // 다시 보내도 같은 결과 — 거절 · 핀은 남음 · 로그엔 결과 코드만(4.4)
                    Log.w(TAG, "안내 시작 기록 거절: HTTP 422")
                    local.setState(pin.sequence, PinUploadState.REJECTED)
                }
                SendOutcome.WAIT_FOR_AUTH -> return@withLock UploadRun.WAITING_FOR_AUTH
                SendOutcome.RETRY -> {
                    scheduler.scheduleRetry(retryIndex)
                    return@withLock UploadRun.RETRY_SCHEDULED
                }
            }
        }
        UploadRun.DONE
    }

    private fun offsetSecondsAt(epochMillis: Long): Int =
        ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(epochMillis)).totalSeconds

    private companion object {
        const val TAG = "PinRepository"
    }
}
