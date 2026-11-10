package com.example.travelcrumbs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** 핀 표(navi_req_log) 읽기 · 쓰기. */
@Dao
interface NaviReqLogDao {
    @Query("SELECT * FROM navi_req_log ORDER BY navi_req_log_no ASC")
    fun observeAll(): Flow<List<NaviReqLogEntity>>

    @Query("SELECT * FROM navi_req_log WHERE uploadState = :state ORDER BY navi_req_log_no ASC")
    suspend fun withState(state: String): List<NaviReqLogEntity>

    @Query("SELECT COALESCE(MAX(navi_req_log_no), 0) FROM navi_req_log")
    suspend fun lastSequence(): Int

    @Insert
    suspend fun insert(row: NaviReqLogEntity)

    @Query("UPDATE navi_req_log SET uploadState = :state WHERE navi_req_log_no = :sequence")
    suspend fun setState(sequence: Int, state: String)

    /** 다음 순서 번호로 한 건 — 읽기 · 쓰기를 한 트랜잭션으로(빈 번호 · 겹친 번호 없음). */
    @Transaction
    suspend fun append(placeId: String, startedAtMillis: Long, utcOffsetSeconds: Int, state: String): Int {
        val next = lastSequence() + 1
        insert(NaviReqLogEntity(next, placeId, startedAtMillis, utcOffsetSeconds, state))
        return next
    }
}
