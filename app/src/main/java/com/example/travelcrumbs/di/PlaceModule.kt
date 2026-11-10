package com.example.travelcrumbs.di

import com.example.travelcrumbs.data.place.DataStorePinnedPlaceLocalDataSource
import com.example.travelcrumbs.data.place.DefaultPinnedPlacesRepository
import com.example.travelcrumbs.data.place.DefaultPlaceCatalogRepository
import com.example.travelcrumbs.data.place.PinnedPlaceLocalDataSource
import com.example.travelcrumbs.data.place.PinnedPlacesRepository
import com.example.travelcrumbs.data.place.PlaceAssetDataSource
import com.example.travelcrumbs.data.place.PlaceCatalogRepository
import com.example.travelcrumbs.data.place.PlaceSource
import com.example.travelcrumbs.data.place.PlaceStatsRepository
import com.example.travelcrumbs.data.place.RemotePlaceStatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 장소(스펙 navigation 4.1) — 후보 · 고정 목록 · 장소별 안내 건수.
 * 후보의 장래 출처(#3 방문 기록 · 공유받은 여행)는 [bindPlaceSource] 한 줄을 바꿔 끼움.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlaceModule {

    @Binds
    @Singleton
    abstract fun bindPlaceSource(impl: PlaceAssetDataSource): PlaceSource

    @Binds
    @Singleton
    abstract fun bindPlaceCatalogRepository(impl: DefaultPlaceCatalogRepository): PlaceCatalogRepository

    @Binds
    @Singleton
    abstract fun bindPinnedPlaceLocalDataSource(
        impl: DataStorePinnedPlaceLocalDataSource,
    ): PinnedPlaceLocalDataSource

    @Binds
    @Singleton
    abstract fun bindPinnedPlacesRepository(impl: DefaultPinnedPlacesRepository): PinnedPlacesRepository

    @Binds
    @Singleton
    abstract fun bindPlaceStatsRepository(impl: RemotePlaceStatsRepository): PlaceStatsRepository
}
