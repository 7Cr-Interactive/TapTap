package com.app.taptap.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScoreEntity::class, AchievementEntity::class], version = 1, exportSchema = false)
abstract class TapTapDatabase : RoomDatabase() {
    abstract fun dao(): TapTapDao
}
