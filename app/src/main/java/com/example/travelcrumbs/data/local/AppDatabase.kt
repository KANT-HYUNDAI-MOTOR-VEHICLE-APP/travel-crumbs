package com.example.travelcrumbs.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 기기 안 Room — 핀 표(스펙 navigation U43) + 여행 표(#51 U1). 백업 대상 아님(allowBackup=false).
 * 판 2 = 핀 표 이름을 데이터 표준(`navi_req_log`)으로 — 판 1 기기의 핀은 [MIGRATION_1_2] 로 옮김.
 * 판 3 = 여행 표 7개(user_mst · user_consent_hist · trip_mst · trip_companion_dtl · trip_place_dtl · tag_mst · trip_place_tag_rel) 추가 —
 * 표만 늘어 Room 자동 마이그레이션(schemas/…/2.json → 3.json)으로 · 핀 표는 그대로.
 */
@Database(
    entities = [
        NaviReqLogEntity::class,
        UserMstEntity::class,
        UserConsentHistEntity::class,
        TripMstEntity::class,
        TripCompanionDtlEntity::class,
        TripPlaceDtlEntity::class,
        TagMstEntity::class,
        TripPlaceTagRelEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun naviReqLogDao(): NaviReqLogDao

    abstract fun travelDao(): TravelDao

    companion object {
        const val FILE_NAME = "travel_crumbs.db"

        /**
         * 판 1(`pins` · sequence · startedAtMillis) → 판 2(`navi_req_log` · navi_req_log_no · created_at).
         * 새 표를 만들고 옮긴 뒤 옛 표를 지움 — ALTER TABLE RENAME COLUMN 은 SQLite 3.25(API 30)부터라 minSdk 28 에서 못 씀.
         * CREATE 문은 schemas/…/2.json 의 createSql 과 같아야 함(Room 이 열 때 대조).
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `navi_req_log` (`navi_req_log_no` INTEGER NOT NULL, " +
                        "`placeId` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `utcOffsetSeconds` INTEGER NOT NULL, " +
                        "`uploadState` TEXT NOT NULL, PRIMARY KEY(`navi_req_log_no`))",
                )
                db.execSQL(
                    "INSERT INTO `navi_req_log` (`navi_req_log_no`, `placeId`, `created_at`, `utcOffsetSeconds`, `uploadState`) " +
                        "SELECT `sequence`, `placeId`, `startedAtMillis`, `utcOffsetSeconds`, `uploadState` FROM `pins`",
                )
                db.execSQL("DROP TABLE `pins`")
            }
        }
    }
}
