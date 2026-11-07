package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.user.DefaultUserRepository
import com.example.travelcrumbs.data.user.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: DefaultUserRepository): UserRepository
}
