package com.example.travelcrumbs.data.pin

import com.example.travelcrumbs.data.local.NaviReqLogDao
import com.example.travelcrumbs.data.local.NaviReqLogEntity
import com.example.travelcrumbs.domain.model.Pin
import com.example.travelcrumbs.domain.model.PinUploadState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 핀 기기 저장(Room · 표 navi_req_log)을 감쌈 — 저장소만 부름. */
interface PinLocalDataSource {
    val pins: Flow<List<Pin>>

    /** 다음 순서로 한 건(보냄 상태 대기) — 붙인 순서 번호를 돌려줌. */
    suspend fun append(placeId: String, startedAtMillis: Long, utcOffsetSeconds: Int): Int

    suspend fun pending(): List<Pin>

    suspend fun setState(sequence: Int, state: PinUploadState)
}

@Singleton
class RoomPinLocalDataSource @Inject constructor(
    private val dao: NaviReqLogDao,
) : PinLocalDataSource {

    override val pins: Flow<List<Pin>> = dao.observeAll().map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun append(placeId: String, startedAtMillis: Long, utcOffsetSeconds: Int): Int =
        dao.append(placeId, startedAtMillis, utcOffsetSeconds, PinUploadState.PENDING.name)

    override suspend fun pending(): List<Pin> =
        dao.withState(PinUploadState.PENDING.name).mapNotNull { it.toDomain() }

    override suspend fun setState(sequence: Int, state: PinUploadState) = dao.setState(sequence, state.name)

    private fun NaviReqLogEntity.toDomain(): Pin? {
        val state = PinUploadState.entries.firstOrNull { it.name == uploadState } ?: return null
        return Pin(sequence, placeId, startedAtMillis, utcOffsetSeconds, state)
    }
}
