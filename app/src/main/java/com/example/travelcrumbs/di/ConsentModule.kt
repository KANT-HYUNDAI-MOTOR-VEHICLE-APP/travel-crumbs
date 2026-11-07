package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.consent.ConsentRepository
import com.example.travelcrumbs.data.consent.DefaultConsentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ConsentModule {

    @Binds
    @Singleton
    abstract fun bindConsentRepository(impl: DefaultConsentRepository): ConsentRepository
}
