package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.gleo.DefaultGleoActionRepository
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.data.gleo.DefaultGleoRecordingReadiness
import com.example.travelcrumbs.data.gleo.GleoRecordingReadiness
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GleoModule {

    @Binds
    @Singleton
    abstract fun bindGleoActionRepository(impl: DefaultGleoActionRepository): GleoActionRepository

    @Binds
    abstract fun bindGleoRecordingReadiness(impl: DefaultGleoRecordingReadiness): GleoRecordingReadiness
}
