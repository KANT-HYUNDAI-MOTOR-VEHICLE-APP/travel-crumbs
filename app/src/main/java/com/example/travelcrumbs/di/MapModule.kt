package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.map.AssetOfflineMapRepository
import com.example.travelcrumbs.data.map.OfflineMapRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 추천 상세의 시연용 오프라인 도로 지도 — 앱에 묶은 에셋만 읽음(네트워크 · 위치 없음). */
@Module
@InstallIn(SingletonComponent::class)
abstract class MapModule {

    @Binds
    @Singleton
    abstract fun bindOfflineMapRepository(impl: AssetOfflineMapRepository): OfflineMapRepository
}
