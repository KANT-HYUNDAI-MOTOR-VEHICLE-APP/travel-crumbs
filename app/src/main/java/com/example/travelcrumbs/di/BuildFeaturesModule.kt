package com.example.travelcrumbs.di

import com.example.travelcrumbs.BuildConfig
import com.example.travelcrumbs.domain.model.DemoFeatures
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 빌드 타입별 시연 기능 — 「더미 경로」 스위치의 출처는 여기 한 곳(ViewModel 은 [DemoFeatures] 만 받음 · 시험은 fake 값).
 * 본문이 `BuildConfig.DEBUG` 한 식인지는 시험이 고정(BuildFeaturesModuleTest · T110).
 */
@Module
@InstallIn(SingletonComponent::class)
object BuildFeaturesModule {

    /** 「더미 경로」는 debug 빌드에서만(스펙 navigation U55 (a)). */
    @Provides
    fun provideDemoFeatures(): DemoFeatures = DemoFeatures(dummyRoute = BuildConfig.DEBUG)
}
