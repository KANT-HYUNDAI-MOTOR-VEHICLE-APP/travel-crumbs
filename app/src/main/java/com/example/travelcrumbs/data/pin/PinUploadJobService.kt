package com.example.travelcrumbs.data.pin

import android.app.job.JobParameters
import android.app.job.JobService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 예약된 다시 보내기(스펙 navigation 4.6) — 대기 핀을 보내고, 또 실패하면 저장소가 다음 간격으로 예약.
 * onStartJob 은 바로 돌아오고(메인 스레드를 막지 않음 · Android 14 ANR 규칙) 일은 코루틴에서.
 */
@AndroidEntryPoint
class PinUploadJobService : JobService() {

    @Inject
    lateinit var pinRepository: PinRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        val retryIndex = params.extras.getInt(KEY_RETRY_INDEX, 0)
        scope.launch {
            runCatching { pinRepository.uploadPending(retryIndex) }
            jobFinished(params, false)
        }
        return true
    }

    /** 조건이 깨져 멈춤(연결 끊김 등) — 시스템이 다시 예약하게 함(핀은 대기 그대로). */
    override fun onStopJob(params: JobParameters): Boolean = true

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val KEY_RETRY_INDEX = "retry_index"
    }
}
