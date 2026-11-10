package com.example.travelcrumbs

import android.app.Application
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #51 버그 — 기록 칸 흐름(A-05 「뒤로」 · 별점 「나중에」) 뒤 「추천」 옛 화면이 보임.
 * 원인: 기본 launchMode(standard)라 뿌리 인텐트와 다른 인텐트(Gleo `OPEN_TRAVEL_CRUMBS` · `am start -n` 뒤 런처 아이콘)가
 * 올 때마다 같은 작업에 MainActivity 가 하나씩 더 쌓임 → 인스턴스마다 앱 틀 상태(목적지)가 따로 →
 * 위 인스턴스가 끝나면 아래 인스턴스의 옛 목적지(추천 B-02)가 드러남(에뮬레이터 실측 2026-11-10 · 스크린샷 41_fix_before_5 · 6).
 * 유일한 액티비티는 singleTop — 같은 작업 맨 위로 오는 인텐트는 새 인스턴스 대신 onNewIntent 로 받음.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MainActivityLaunchModeTest {

    @Test
    fun `B51-1 MainActivity 는 singleTop — 런처 · Gleo 인텐트가 새 인스턴스를 쌓지 않음`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val info = context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)

        assertEquals(ActivityInfo.LAUNCH_SINGLE_TOP, info.launchMode)
    }
}
