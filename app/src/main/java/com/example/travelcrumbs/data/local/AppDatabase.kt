package com.example.travelcrumbs.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/** 기기 안 Room — 지금은 핀 표 하나(스펙 navigation U43). 백업 대상 아님(allowBackup=false). */
@Database(entities = [PinEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pinDao(): PinDao

    companion object {
        const val FILE_NAME = "travel_crumbs.db"
    }
}
