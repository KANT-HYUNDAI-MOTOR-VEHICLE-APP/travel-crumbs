package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.trip.LocalTagRepository
import com.example.travelcrumbs.data.trip.LocalTripRepository
import com.example.travelcrumbs.data.trip.TagRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.local.AppDatabase
import com.example.travelcrumbs.data.local.TravelDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 내 여행 · 태그(#51 U1) — 지금은 기기 안 Room. 서버 API 가 생기면(ADR 0015 — 원본은 서버) 이 바인딩만 서버 구현으로.
 * 지금 사용자는 di/UserModule.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: LocalTripRepository): TripRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: LocalTagRepository): TagRepository

    companion object {
        @Provides
        fun provideTravelDao(database: AppDatabase): TravelDao = database.travelDao()
    }
}
