package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.recording.RoomRecordingLocalDataSource
import com.example.travelcrumbs.data.recording.DefaultRecordingRepository
import com.example.travelcrumbs.data.recording.RecordingLocalDataSource
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.time.SystemTimeSource
import com.example.travelcrumbs.data.unfinishedinput.DataStoreUnfinishedInputLocalDataSource
import com.example.travelcrumbs.data.unfinishedinput.DefaultUnfinishedInputRepository
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputLocalDataSource
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.domain.NotReadyRecordPlaceUseCase
import com.example.travelcrumbs.domain.NotReadyStartVoiceInputUseCase
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.StartVoiceInputUseCase
import com.example.travelcrumbs.domain.TimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 앱 뼈대(#6) — 여행 상태 · 작성 중 내용 · 버튼 연결 지점(#21 이름 바꿈).
 * 「이 장소 기록」(#3) · 「음성」(#5) 은 아래 바인딩 한 줄씩을 실제 구현으로 바꿔 끼움.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ShellModule {

    @Binds
    @Singleton
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource

    @Binds
    @Singleton
    abstract fun bindRecordingLocalDataSource(
        impl: RoomRecordingLocalDataSource,
    ): RecordingLocalDataSource

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(impl: DefaultRecordingRepository): RecordingRepository

    @Binds
    @Singleton
    abstract fun bindUnfinishedInputLocalDataSource(
        impl: DataStoreUnfinishedInputLocalDataSource,
    ): UnfinishedInputLocalDataSource

    @Binds
    @Singleton
    abstract fun bindUnfinishedInputRepository(impl: DefaultUnfinishedInputRepository): UnfinishedInputRepository

    @Binds
    abstract fun bindRecordPlaceUseCase(impl: NotReadyRecordPlaceUseCase): RecordPlaceUseCase

    @Binds
    abstract fun bindStartVoiceInputUseCase(impl: NotReadyStartVoiceInputUseCase): StartVoiceInputUseCase
}
