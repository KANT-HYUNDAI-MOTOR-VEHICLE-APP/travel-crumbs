package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.pin.PinRepository
import com.example.travelcrumbs.domain.GuideRequestTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 길안내 요청 입구(#4 · 스펙 navigation 3.2) — 앱 하나에 하나 · 앱 수명 범위.
 * 홈 ViewModel 이 사라지거나(주행 전용 화면) 앱이 얼어도 요청 · 한도 · 늦은 알림 대조가 이어짐.
 * 스레드는 홈 ViewModel 이 쓰던 것과 같은 메인(Main.immediate) — NaviHelper 호출 스레드가 바뀌지 않게.
 */
@Module
@InstallIn(SingletonComponent::class)
object GuideModule {

    @Provides
    @Singleton
    fun provideGuideRequestTracker(navi: NaviRepository, pins: PinRepository): GuideRequestTracker =
        GuideRequestTracker(
            navi = navi,
            recorder = pins,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
}
