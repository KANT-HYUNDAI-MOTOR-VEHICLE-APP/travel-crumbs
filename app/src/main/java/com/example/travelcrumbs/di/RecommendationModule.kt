package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.data.recommendation.SeedRecommendationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 추천(#37) — 공개 로드맵 저장소. 서버 API 가 생기면 이 바인딩 한 줄을 서버 구현으로 바꿈. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RecommendationModule {

    @Binds
    @Singleton
    abstract fun bindRecommendationRepository(impl: SeedRecommendationRepository): RecommendationRepository
}
