package com.app.taptap.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.taptap.analytics.AnalyticsManager
import com.app.taptap.data.ScoreEntity
import com.app.taptap.data.TapTapDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val dao: TapTapDao,
    private val analyticsManager: AnalyticsManager? = null
) : ViewModel() {

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    private val _combo = MutableStateFlow(0)
    val combo: StateFlow<Int> = _combo

    private val _timeRemainingMs = MutableStateFlow(100000.0)
    val timeRemainingMs: StateFlow<Double> = _timeRemainingMs

    private val _isGameOver = MutableStateFlow(false)
    val isGameOver: StateFlow<Boolean> = _isGameOver

    private val _isShieldActive = MutableStateFlow(false)
    val isShieldActive: StateFlow<Boolean> = _isShieldActive

    private val _freezeDurationLeftMs = MutableStateFlow(0L)
    val freezeDurationLeftMs: StateFlow<Long> = _freezeDurationLeftMs

    var gameMode = "NORMAL"
    var maxTimeMs = 100000.0

    var hasRevived = false
        private set

    var hasDoubledScore = false
        private set

    fun initGame(mode: String, startWithBooster: Boolean = false) {
        gameMode = mode
        maxTimeMs = if (mode == "ZIP") 60000.0 else 100000.0
        _timeRemainingMs.value = maxTimeMs
        _isGameOver.value = false
        hasRevived = false
        hasDoubledScore = false

        if (startWithBooster) {
            activateShield()
            activateFreeze()
        }
    }

    fun incrementScore(type: String) {
        when (type) {
            "RAINBOW" -> {
                _score.value += 5
                _combo.value += 20
            }
            "NORMAL" -> {
                _combo.value++
                _score.value += 1
            }
            "BLAST_COLLATERAL" -> {
                _score.value += 1
            }
        }
        checkAchievements()
    }

    fun hitNail() {
        if (_isShieldActive.value) {
            _isShieldActive.value = false
        } else {
            _combo.value = 0
            _score.value = (_score.value - 3).coerceAtLeast(0)
            _timeRemainingMs.value = (_timeRemainingMs.value - 10000.0).coerceAtLeast(0.0)
        }
    }

    fun activateFreeze() {
        _freezeDurationLeftMs.value = 5000L
    }

    fun activateShield() {
        _isShieldActive.value = true
    }

    fun canRevive(): Boolean = !hasRevived

    fun reviveGame() {
        if (!hasRevived) {
            hasRevived = true
            _isGameOver.value = false
            _timeRemainingMs.value = (maxTimeMs * 0.5).coerceAtLeast(30000.0)
            activateShield()
        }
    }

    fun canDoubleScore(): Boolean = !hasDoubledScore

    fun doubleScore() {
        if (!hasDoubledScore) {
            hasDoubledScore = true
            _score.value = _score.value * 2
        }
    }

    fun updateTime(deltaMs: Long) {
        if (_freezeDurationLeftMs.value > 0) {
            _freezeDurationLeftMs.value = (_freezeDurationLeftMs.value - deltaMs).coerceAtLeast(0)
            return
        }

        val drainPerSec = if (gameMode == "ZIP") 1000.0 else 3500.0 + (_score.value * 30.0)
        val frameDrain = drainPerSec * (deltaMs / 1000.0)
        
        _timeRemainingMs.value = (_timeRemainingMs.value - frameDrain).coerceAtLeast(0.0)
        if (_timeRemainingMs.value <= 0) {
            _isGameOver.value = true
        }
    }

    fun addTimeBonus() {
        if (gameMode == "NORMAL") {
            val refillMs = (8000.0 - (_score.value * 15.0)).coerceAtLeast(4000.0)
            val comboBonusMs = (_combo.value * 150.0).coerceAtMost(3000.0)
            _timeRemainingMs.value = (_timeRemainingMs.value + refillMs + comboBonusMs).coerceAtMost(maxTimeMs)
        }
    }

    fun saveScore(name: String) {
        analyticsManager?.logScoreSubmitted(gameMode, _score.value)
        viewModelScope.launch {
            dao.insertScore(ScoreEntity(name = name, score = _score.value, mode = gameMode))
        }
    }

    private fun checkAchievements() {
        viewModelScope.launch {
            if (_combo.value >= 50) {
                analyticsManager?.logAchievementUnlocked("combo_king")
                dao.unlockAchievement("combo_king", System.currentTimeMillis())
            }
        }
    }

    // Scaling factors
    fun getSpeedFactor(): Float {
        return 1f + (_score.value / 100f).coerceAtMost(1f)
    }

    fun getShrinkFactor(): Float {
        return (1f - (_score.value / 200f)).coerceAtLeast(0.5f)
    }
    
    fun getMovementSpeed(): Float {
        return if (_score.value > 50) (_score.value - 50) * 2f else 0f
    }
}
