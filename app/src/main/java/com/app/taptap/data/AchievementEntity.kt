package com.app.taptap.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockTimestamp: Long? = null
)
