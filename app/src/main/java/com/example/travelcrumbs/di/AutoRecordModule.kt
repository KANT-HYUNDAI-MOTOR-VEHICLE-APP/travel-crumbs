package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.autorecord.AutoPlaceRecorder
import com.example.travelcrumbs.data.autorecord.AutoRecordEndTrip
import com.example.travelcrumbs.data.autorecord.AutoRecordLocalDataSource
import com.example.travelcrumbs.data.autorecord.DataStoreAutoRecordLocalDataSource
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.EndTripUseCase
import com.example.travelcrumbs.domain.GuideRequestTracker
import com.example.travelcrumbs.domain.PlaceRecordCleanup
import com.example.travelcrumbs.domain.TimeSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 내비 목적지 자동 장소 기록(#66) — 앱 하나에 하나 · 앱 수명 범위.
 * 스레드는 길안내 요청 입구와 같은 메인(Main.immediate) — 입구의 읽기 전용 질의가 메인에서만 불리게.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AutoRecordModule {

    @Binds
    @Singleton
    abstract fun bindAutoRecordLocalDataSource(impl: DataStoreAutoRecordLocalDataSource): AutoRecordLocalDataSource

    @Binds
    abstract fun bindEndTripUseCase(impl: AutoRecordEndTrip): EndTripUseCase

    @Binds
    abstract fun bindPlaceRecordCleanup(impl: AutoPlaceRecorder): PlaceRecordCleanup

    companion object {
        @Provides
        @Singleton
        fun provideAutoPlaceRecorder(
            navi: NaviRepository,
            driving: DrivingSignalRepository,
            recording: RecordingRepository,
            users: CurrentUserRepository,
            trips: TripRepository,
            unfinishedInputs: UnfinishedInputRepository,
            guideRequests: GuideRequestTracker,
            store: AutoRecordLocalDataSource,
            timeSource: TimeSource,
        ): AutoPlaceRecorder = AutoPlaceRecorder(
            navi = navi,
            driving = driving,
            recording = recording,
            users = users,
            trips = trips,
            unfinishedInputs = unfinishedInputs,
            guideRequests = guideRequests,
            store = store,
            timeSource = timeSource,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
    }
}
