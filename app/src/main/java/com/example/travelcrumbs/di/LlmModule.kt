package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.llm.DefaultLlmRepository
import com.example.travelcrumbs.data.llm.LlmRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @Singleton
    abstract fun bindLlmRepository(impl: DefaultLlmRepository): LlmRepository
}
