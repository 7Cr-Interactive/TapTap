package com.app.taptap.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TapTapDao {
    @Query("SELECT * FROM scores WHERE mode = :mode ORDER BY score DESC LIMIT 5")
    fun getTopScores(mode: String): Flow<List<ScoreEntity>>

    @Insert
    suspend fun insertScore(score: ScoreEntity)

    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("UPDATE achievements SET isUnlocked = 1, unlockTimestamp = :timestamp WHERE id = :id")
    suspend fun unlockAchievement(id: String, timestamp: Long)

    @Query("DELETE FROM scores WHERE mode = :mode")
    suspend fun clearScores(mode: String)
}
