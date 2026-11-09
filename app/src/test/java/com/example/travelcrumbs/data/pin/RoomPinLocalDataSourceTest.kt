package com.example.travelcrumbs.data.pin

import android.app.Application
import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import com.example.travelcrumbs.data.local.AppDatabase
import com.example.travelcrumbs.domain.model.PinUploadState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 핀 기기 저장(Room · 스펙 navigation U43) — 실제 DB 파일로 앱 재실행(닫고 다시 열기)을 흉내. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoomPinLocalDataSourceTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private var database: AppDatabase? = null

    private fun open(): RoomPinLocalDataSource {
        database?.close()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, FILE).allowMainThreadQueries().build()
        database = db
        return RoomPinLocalDataSource(db.naviReqLogDao())
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(FILE)
    }

    @Test
    fun `T46 핀 3건 대기 — DB 를 닫고 다시 열어도 순서 · 대기 그대로`() = runBlocking {
        val first = open()
        listOf("a", "b", "c").forEachIndexed { i, id -> first.append(id, 1_000L + i, 32_400) }

        val again = open()

        val pins = again.pins.first()
        assertEquals(listOf("a", "b", "c"), pins.map { it.placeId })
        assertEquals(listOf(1, 2, 3), pins.map { it.sequence })
        assertEquals(List(3) { PinUploadState.PENDING }, pins.map { it.uploadState })
        assertEquals(32_400, pins.first().utcOffsetSeconds)
    }

    @Test
    fun `T64 T61 보냄 상태만 바뀌고 핀은 남음 · 대기 목록에서 빠짐`() = runBlocking {
        val store = open()
        store.append("a", 1L, 0)
        store.append("b", 2L, 0)

        store.setState(1, PinUploadState.SENT)
        store.setState(2, PinUploadState.REJECTED)

        assertEquals(2, store.pins.first().size)
        assertEquals(emptyList<String>(), store.pending().map { it.placeId })
    }

    private companion object {
        const val FILE = "pins-test.db"
    }
}
