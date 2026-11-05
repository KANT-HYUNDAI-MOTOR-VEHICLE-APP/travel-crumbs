package com.example.travelcrumbs.di

import android.content.Context
import androidx.room.Room
import com.example.travelcrumbs.data.auth.AccessTokenSource
import com.example.travelcrumbs.data.auth.DebugSeedAccessTokenSource
import com.example.travelcrumbs.data.local.AppDatabase
import com.example.travelcrumbs.data.local.PinDao
import com.example.travelcrumbs.data.pin.DefaultPinRepository
import com.example.travelcrumbs.data.pin.JobSchedulerPinUploadScheduler
import com.example.travelcrumbs.data.pin.PinLocalDataSource
import com.example.travelcrumbs.data.pin.PinRepository
import com.example.travelcrumbs.data.pin.PinUploadScheduler
import com.example.travelcrumbs.data.pin.RoomPinLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 핀(여행의 목적지 기록) · 서버 보내기(스펙 navigation 4.1 · 4.6).
 * 핀을 여행에 붙이는 일(#3)은 [bindPinRepository] 를 · 로그인 토큰(#1)은 [bindAccessTokenSource] 를 바꿔 끼움.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PinModule {

    @Binds
    @Singleton
    abstract fun bindPinLocalDataSource(impl: RoomPinLocalDataSource): PinLocalDataSource

    @Binds
    @Singleton
    abstract fun bindPinRepository(impl: DefaultPinRepository): PinRepository

    @Binds
    @Singleton
    abstract fun bindPinUploadScheduler(impl: JobSchedulerPinUploadScheduler): PinUploadScheduler

    @Binds
    @Singleton
    abstract fun bindAccessTokenSource(impl: DebugSeedAccessTokenSource): AccessTokenSource

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.FILE_NAME).build()

        @Provides
        fun providePinDao(database: AppDatabase): PinDao = database.pinDao()
    }
}
