package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.vehicle.DefaultDrivingSignalRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VehicleModule {

    @Binds
    @Singleton
    abstract fun bindDrivingSignalRepository(
        impl: DefaultDrivingSignalRepository,
    ): DrivingSignalRepository
}
