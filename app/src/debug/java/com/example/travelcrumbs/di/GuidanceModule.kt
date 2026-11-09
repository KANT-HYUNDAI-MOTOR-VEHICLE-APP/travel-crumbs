package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.guidance.DemoRouteGuidanceRequester
import com.example.travelcrumbs.domain.RouteGuidanceRequester
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 길안내 요청 포트(#37) — debug 빌드: 시연용 구현(요청 기록 · 성공 답).
 * #4(`navigation/main`)의 NaviRepository.requestRoute 가 develop 에 들어오면 main 의 바인딩 하나로 옮기고
 * 이 파일과 release 의 같은 이름 파일을 지움.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GuidanceModule {

    @Binds
    @Singleton
    abstract fun bindRouteGuidanceRequester(impl: DemoRouteGuidanceRequester): RouteGuidanceRequester
}
