package com.example.travelcrumbs.data.pin

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import com.example.travelcrumbs.domain.uploadRetryDelayMillis
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 프레임워크 JobScheduler 로 예약 — 프로세스가 없어도 시스템이 앱을 깨워 [PinUploadJobService] 를 부름 · 연결되어 있을 때만.
 * WorkManager 를 쓰지 않은 까닭: 그 라이브러리 매니페스트가 WAKE_LOCK · RECEIVE_BOOT_COMPLETED · FOREGROUND_SERVICE 를
 * 합쳐 넣어 최소 권한(심사)에서 벗어남. 연결 조건(setRequiredNetworkType)은 targetSdk 34 에서 ACCESS_NETWORK_STATE 가 필요.
 * 재부팅 뒤엔 예약이 남지 않음(setPersisted 는 RECEIVE_BOOT_COMPLETED 필요) — 다음 앱 실행 때 [scheduleOnAppStart] 가 다시 예약.
 */
@Singleton
class JobSchedulerPinUploadScheduler @Inject constructor(
    @ApplicationContext context: Context,
) : PinUploadScheduler {
    private val appContext = context.applicationContext
    private val jobScheduler: JobScheduler? = appContext.getSystemService(JobScheduler::class.java)

    override fun scheduleRetry(retryIndex: Int) {
        schedule(delayMillis = uploadRetryDelayMillis(retryIndex), nextRetryIndex = retryIndex + 1)
    }

    override fun scheduleOnAppStart() {
        if (jobScheduler?.getPendingJob(JOB_ID) != null) return
        schedule(delayMillis = 0L, nextRetryIndex = 0)
    }

    private fun schedule(delayMillis: Long, nextRetryIndex: Int) {
        val scheduler = jobScheduler ?: return
        val job = JobInfo.Builder(JOB_ID, ComponentName(appContext, PinUploadJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setMinimumLatency(delayMillis)
            .setExtras(PersistableBundle().apply { putInt(PinUploadJobService.KEY_RETRY_INDEX, nextRetryIndex) })
            .build()
        runCatching { scheduler.schedule(job) }
            .onFailure { Log.w(TAG, "다시 보내기 예약 실패: ${it.javaClass.simpleName}") }
    }

    private companion object {
        const val TAG = "PinUploadScheduler"

        /** 앱 안 JobScheduler 작업 id — 핀 보내기 하나뿐. */
        const val JOB_ID = 4_001
    }
}
