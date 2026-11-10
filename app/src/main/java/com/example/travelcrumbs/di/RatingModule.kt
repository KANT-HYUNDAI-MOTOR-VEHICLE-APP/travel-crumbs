package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.rating.DataStoreRatingLocalDataSource
import com.example.travelcrumbs.data.rating.DefaultRatingRepository
import com.example.travelcrumbs.data.rating.RatingLocalDataSource
import com.example.travelcrumbs.data.rating.RatingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 별점(#41) — 기기 안 저장소. 서버 별점 API 가 생기면 저장소 바인딩만 바꿈. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RatingModule {

    @Binds
    @Singleton
    abstract fun bindRatingLocalDataSource(impl: DataStoreRatingLocalDataSource): RatingLocalDataSource

    @Binds
    @Singleton
    abstract fun bindRatingRepository(impl: DefaultRatingRepository): RatingRepository
}
