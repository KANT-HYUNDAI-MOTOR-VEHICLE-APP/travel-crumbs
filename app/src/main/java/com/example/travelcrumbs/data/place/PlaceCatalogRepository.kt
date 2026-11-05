package com.example.travelcrumbs.data.place

import com.example.travelcrumbs.domain.model.Place
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 「장소 고르기」 후보의 단일 출처(스펙 navigation 4.1) — 화면 · ViewModel 은 이 저장소로만 받음. */
interface PlaceCatalogRepository {
    /** 후보 전부 · 후보 JSON 순서. */
    suspend fun candidates(): List<Place>
}

@Singleton
class DefaultPlaceCatalogRepository @Inject constructor(
    private val source: PlaceSource,
) : PlaceCatalogRepository {
    private val mutex = Mutex()
    private var cached: List<Place>? = null

    override suspend fun candidates(): List<Place> = mutex.withLock {
        cached ?: source.places().also { cached = it }
    }
}
