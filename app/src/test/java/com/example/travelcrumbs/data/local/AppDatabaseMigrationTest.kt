package com.example.travelcrumbs.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * 판 1(`pins`) 기기에 앱 업데이트 → 판 2(`navi_req_log`) — 핀 순서 · 장소 · 시각 · 시간대 · 보냄 상태가 그대로 옮겨짐.
 * 판 1 파일은 schemas/…/1.json 의 createSql · identityHash 로 손으로 만듦.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AppDatabaseMigrationTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private var database: AppDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(FILE)
    }

    @Test
    fun `판 1 핀 표가 판 2 navi_req_log 로 옮겨짐 — 순서 · 값 · 보냄 상태 그대로`() = runBlocking {
        context.openOrCreateDatabase(FILE, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(V1_CREATE)
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$V1_HASH')")
            db.execSQL("INSERT INTO pins VALUES (1, 'a', 1000, 32400, 'SENT')")
            db.execSQL("INSERT INTO pins VALUES (2, 'b', 2000, 32400, 'PENDING')")
            db.version = 1
        }

        val db = Room.databaseBuilder(context, AppDatabase::class.java, FILE)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        database = db

        val rows = db.naviReqLogDao().observeAll().first()
        assertEquals(
            listOf(
                NaviReqLogEntity(1, "a", 1000L, 32_400, "SENT"),
                NaviReqLogEntity(2, "b", 2000L, 32_400, "PENDING"),
            ),
            rows,
        )
        assertEquals(listOf("b"), db.naviReqLogDao().withState("PENDING").map { it.placeId })
        assertEquals(3, db.naviReqLogDao().append("c", 3000L, 0, "PENDING"))
    }

    private companion object {
        const val FILE = "migration-test.db"
        const val V1_HASH = "b778891ee68619977a270351c85c080e"
        const val V1_CREATE =
            "CREATE TABLE IF NOT EXISTS `pins` (`sequence` INTEGER NOT NULL, `placeId` TEXT NOT NULL, " +
                "`startedAtMillis` INTEGER NOT NULL, `utcOffsetSeconds` INTEGER NOT NULL, `uploadState` TEXT NOT NULL, " +
                "PRIMARY KEY(`sequence`))"
    }
}
