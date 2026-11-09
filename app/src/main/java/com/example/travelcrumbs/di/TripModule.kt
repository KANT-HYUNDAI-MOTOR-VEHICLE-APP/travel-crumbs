package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.trip.LocalTagRepository
import com.example.travelcrumbs.data.trip.LocalTripRepository
import com.example.travelcrumbs.data.trip.TagRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.data.user.LocalCurrentUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 내 여행 · 태그 · 지금 사용자(#51 U1) — 지금은 기기 안 Room. 서버 API 가 생기면(ADR 0015 — 원본은 서버) 이 바인딩만 서버 구현으로.
 * 로그인 담당(C-01)은 [bindCurrentUserRepository] 한 줄을 바꿔 끼움.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {

    @Binds
    @Singleton
    abstract fun bindCurrentUserRepository(impl: LocalCurrentUserRepository): CurrentUserRepository

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: LocalTripRepository): TripRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: LocalTagRepository): TagRepository
}
