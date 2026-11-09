package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.map.OfflineMapRepository
import com.example.travelcrumbs.domain.model.OfflineMapPack

/** 오프라인 지도 묶음 fake — 기본은 빈 묶음(상세는 기존 경로 그림 그대로). */
class FakeOfflineMapRepository(
    var pack: OfflineMapPack = OfflineMapPack(emptyList()),
) : OfflineMapRepository {
    var loads = 0
        private set

    override suspend fun mapPack(): OfflineMapPack {
        loads++
        return pack
    }
}
