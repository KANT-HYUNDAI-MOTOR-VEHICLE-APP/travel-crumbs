package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.code.CodeRepository
import com.example.travelcrumbs.data.code.DefaultCodeRepository
import com.example.travelcrumbs.data.trip.DefaultTripRepository
import com.example.travelcrumbs.data.trip.PassThroughTripNameSuggester
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.domain.TripNameSuggester
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 여행 시작 설정(A-04)이 쓰는 저장소들. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TripModule {

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: DefaultTripRepository): TripRepository

    @Binds
    @Singleton
    abstract fun bindCodeRepository(impl: DefaultCodeRepository): CodeRepository

    /** Gleo AI 여행명 추천 — Gleo 담당 팀원이 이 한 줄의 구현 클래스만 바꾸면 됨. */
    @Binds
    abstract fun bindTripNameSuggester(impl: PassThroughTripNameSuggester): TripNameSuggester
}
