package com.example.travelcrumbs.di

import com.example.travelcrumbs.domain.RouteGuidanceRequester
import com.example.travelcrumbs.domain.TrackerRouteGuidanceRequester
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 추천 길안내 요청 포트(#37 · #41) — 성호님 길안내 입구(GuideRequestTracker) 하나로(#51 U7) · 모든 빌드 같은 바인딩. */
@Module
@InstallIn(SingletonComponent::class)
abstract class GuidanceModule {

    @Binds
    @Singleton
    abstract fun bindRouteGuidanceRequester(impl: TrackerRouteGuidanceRequester): RouteGuidanceRequester
}
