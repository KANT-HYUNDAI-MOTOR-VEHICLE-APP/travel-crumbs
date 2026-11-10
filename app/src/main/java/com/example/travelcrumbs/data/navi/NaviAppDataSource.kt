package com.example.travelcrumbs.data.navi

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.travelcrumbs.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 차량 내비 앱을 앞으로 띄우는 유일한 곳(#51 U0 「내비 보기」 · U3 목적지 없는 여행 시작).
 * 내비 앱의 실행 Intent(패키지 [NAVI_PACKAGE] 의 launch intent)로 엶 — 없으면(내비 없는 기기) 열지 않음.
 * 창 나눔은 앱이 강제할 수 없음(창 제어 권한 signature|privileged · FLAG_ACTIVITY_LAUNCH_ADJACENT 무시 — 에뮬레이터 실측) —
 * 나눈 상태면 내비 칸에 · 아니면 앱 영역 전체로 뜸.
 */
@Singleton
class NaviAppDataSource @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext

    private fun launchIntent(): Intent? =
        runCatching { appContext.packageManager.getLaunchIntentForPackage(NAVI_PACKAGE) }.getOrNull()

    /** 이 기기에 내비 앱이 있어 열 수 있나. */
    fun available(): Boolean = launchIntent() != null

    /** 내비 앱을 엶 — 열었으면 참. */
    fun open(): Boolean {
        val intent = launchIntent() ?: return false
        return runCatching {
            appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrElse {
            Log.w(TAG, "내비 앱 열기 실패: ${it.javaClass.simpleName}")
            false
        }
    }

    private companion object {
        const val TAG = "NaviAppDataSource"

        /** Pleos Connect 차량 내비 앱 패키지(에뮬레이터 실측) — 값의 원본은 app/build.gradle.kts 하나(매니페스트 <queries> 와 공유). */
        const val NAVI_PACKAGE = BuildConfig.NAVI_PACKAGE
    }
}
