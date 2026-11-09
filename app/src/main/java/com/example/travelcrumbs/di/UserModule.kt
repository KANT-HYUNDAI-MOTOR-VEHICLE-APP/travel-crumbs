package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.data.user.LocalCurrentUserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 지금 사용자(#51 U1) — 로그인 담당(C-01)은 [bindCurrentUserRepository] 한 줄을 로그인 구현으로 바꿔 끼움. */
@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {

    @Binds
    @Singleton
    abstract fun bindCurrentUserRepository(impl: LocalCurrentUserRepository): CurrentUserRepository
}
