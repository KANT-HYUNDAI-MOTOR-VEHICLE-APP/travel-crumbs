package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.guidance.UnavailableRouteGuidanceRequester
import com.example.travelcrumbs.domain.RouteGuidanceRequester
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 길안내 요청 포트(#37) — release 빌드: 내비 연결 전이라 「안내할 수 없음」으로 답함(시연용 구현은 debug 에만).
 * #4 의 길안내 요청이 develop 에 들어오면 main 의 바인딩 하나로 옮기고 이 파일을 지움.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GuidanceModule {

    @Binds
    abstract fun bindRouteGuidanceRequester(impl: UnavailableRouteGuidanceRequester): RouteGuidanceRequester
}
