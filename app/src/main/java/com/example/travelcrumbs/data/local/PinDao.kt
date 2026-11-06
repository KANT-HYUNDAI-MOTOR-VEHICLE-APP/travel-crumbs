package com.example.travelcrumbs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PinDao {
    @Query("SELECT * FROM pins ORDER BY sequence ASC")
    fun observeAll(): Flow<List<PinEntity>>

    @Query("SELECT * FROM pins WHERE uploadState = :state ORDER BY sequence ASC")
    suspend fun withState(state: String): List<PinEntity>

    @Query("SELECT COALESCE(MAX(sequence), 0) FROM pins")
    suspend fun lastSequence(): Int

    @Insert
    suspend fun insert(pin: PinEntity)

    @Query("UPDATE pins SET uploadState = :state WHERE sequence = :sequence")
    suspend fun setState(sequence: Int, state: String)

    /** 다음 순서 번호로 한 건 — 읽기 · 쓰기를 한 트랜잭션으로(빈 번호 · 겹친 번호 없음). */
    @Transaction
    suspend fun append(placeId: String, startedAtMillis: Long, utcOffsetSeconds: Int, state: String): Int {
        val next = lastSequence() + 1
        insert(PinEntity(next, placeId, startedAtMillis, utcOffsetSeconds, state))
        return next
    }
}
