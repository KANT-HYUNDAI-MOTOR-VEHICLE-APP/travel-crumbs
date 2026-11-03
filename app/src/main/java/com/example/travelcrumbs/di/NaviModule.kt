package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.navi.DefaultNaviRepository
import com.example.travelcrumbs.data.navi.NaviRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NaviModule {

    @Binds
    @Singleton
    abstract fun bindNaviRepository(impl: DefaultNaviRepository): NaviRepository
}
