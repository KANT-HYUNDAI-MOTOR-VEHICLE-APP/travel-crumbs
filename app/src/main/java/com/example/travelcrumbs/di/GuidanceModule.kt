package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.navi.NaviRouteGuidanceRequester
import com.example.travelcrumbs.domain.RouteGuidanceRequester
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 추천 길안내 요청 포트(#37 · #41) — 실제 차량 내비(#4 NaviRepository)로 · 모든 빌드 같은 바인딩. */
@Module
@InstallIn(SingletonComponent::class)
abstract class GuidanceModule {

    @Binds
    @Singleton
    abstract fun bindRouteGuidanceRequester(impl: NaviRouteGuidanceRequester): RouteGuidanceRequester
}
