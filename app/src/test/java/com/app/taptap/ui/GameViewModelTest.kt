package com.app.taptap.ui

import com.app.taptap.data.AchievementEntity
import com.app.taptap.data.ScoreEntity
import com.app.taptap.data.TapTapDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GameViewModelTest {

    private lateinit var viewModel: GameViewModel
    private val mockDao = object : TapTapDao {
        override fun getTopScores(mode: String): Flow<List<ScoreEntity>> = emptyFlow()
        override suspend fun insertScore(score: ScoreEntity) {}
        override fun getAllAchievements(): Flow<List<AchievementEntity>> = emptyFlow()
        override suspend fun unlockAchievement(id: String, timestamp: Long) {}
    }

    @Before
    fun setup() {
        viewModel = GameViewModel(mockDao)
    }

    @Test
    fun `incrementScore NORMAL increases score and combo`() {
        viewModel.incrementScore("NORMAL")
        assertEquals(1, viewModel.score.value)
        assertEquals(1, viewModel.combo.value)
    }

    @Test
    fun `hitNail with no shield decreases score and time`() {
        viewModel.initGame("NORMAL")
        viewModel.incrementScore("NORMAL") // score 1
        viewModel.incrementScore("NORMAL") // score 2
        viewModel.incrementScore("NORMAL") // score 3
        
        val initialTime = viewModel.timeRemainingMs.value
        viewModel.hitNail()
        
        assertEquals(0, viewModel.score.value)
        assertEquals(0, viewModel.combo.value)
        assertEquals(initialTime - 10000.0, viewModel.timeRemainingMs.value, 0.1)
    }

    @Test
    fun `activateShield prevents score loss on hitNail`() {
        viewModel.incrementScore("NORMAL")
        viewModel.activateShield()
        
        viewModel.hitNail()
        
        assertEquals(1, viewModel.score.value)
        assertEquals(false, viewModel.isShieldActive.value)
    }

    @Test
    fun `scaling factors increase with score`() {
        viewModel.incrementScore("NORMAL") // 1
        val initialSpeed = viewModel.getSpeedFactor()
        
        repeat(100) { viewModel.incrementScore("NORMAL") }
        
        assert(viewModel.getSpeedFactor() > initialSpeed)
        assert(viewModel.getShrinkFactor() < 1f)
    }
}
