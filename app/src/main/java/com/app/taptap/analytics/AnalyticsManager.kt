package com.app.taptap.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {

    /**
     * Logs screen view event when user opens a screen.
     */
    fun logScreenView(screenName: String, className: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, className)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    /**
     * Logs when a game session starts.
     */
    fun logGameStart(gameMode: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, gameMode)
            putString("game_mode", gameMode)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LEVEL_START, bundle)
    }

    /**
     * Logs when a game session ends.
     */
    fun logGameOver(gameMode: String, finalScore: Int, maxCombo: Int) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, gameMode)
            putString("game_mode", gameMode)
            putInt(FirebaseAnalytics.Param.SCORE, finalScore)
            putInt("max_combo", maxCombo)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LEVEL_END, bundle)
    }

    /**
     * Logs when user manually exits a game in progress.
     */
    fun logGameExit(gameMode: String, currentScore: Int) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
            putInt("score_at_exit", currentScore)
        }
        firebaseAnalytics.logEvent("game_exit", bundle)
    }

    /**
     * Logs bubble pops with bubble type and current combo.
     */
    fun logBubblePop(bubbleType: String, currentCombo: Int) {
        val bundle = Bundle().apply {
            putString("bubble_type", bubbleType)
            putInt("combo_count", currentCombo)
        }
        firebaseAnalytics.logEvent("bubble_pop", bundle)
    }

    /**
     * Logs when a special power-up bubble is activated.
     */
    fun logPowerUpActivated(powerUpType: String) {
        val bundle = Bundle().apply {
            putString("power_up_type", powerUpType)
        }
        firebaseAnalytics.logEvent("power_up_activated", bundle)
    }

    /**
     * Logs when a nail (hazard) is hit.
     */
    fun logNailHit(shieldBlocked: Boolean, currentScore: Int, currentCombo: Int) {
        val bundle = Bundle().apply {
            putBoolean("shield_blocked", shieldBlocked)
            putInt("score", currentScore)
            putInt("combo", currentCombo)
        }
        firebaseAnalytics.logEvent("nail_hit", bundle)
    }

    /**
     * Logs when user reaches a combo milestone (e.g. 5, 10, 15...).
     */
    fun logComboMilestone(comboCount: Int) {
        val bundle = Bundle().apply {
            putInt("combo_count", comboCount)
        }
        firebaseAnalytics.logEvent("combo_milestone", bundle)
    }

    /**
     * Logs when an achievement is unlocked.
     */
    fun logAchievementUnlocked(achievementId: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, achievementId)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle)
    }

    /**
     * Logs when score is submitted to local leaderboard.
     */
    fun logScoreSubmitted(gameMode: String, score: Int) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
            putInt(FirebaseAnalytics.Param.SCORE, score)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.POST_SCORE, bundle)
    }

    /**
     * Logs when high scores tab is viewed.
     */
    fun logHighScoresViewed(gameMode: String) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
        }
        firebaseAnalytics.logEvent("high_scores_viewed", bundle)
    }

    /**
     * Logs when high scores for a mode are reset.
     */
    fun logHighScoresReset(gameMode: String) {
        val bundle = Bundle().apply {
            putString("game_mode", gameMode)
        }
        firebaseAnalytics.logEvent("high_scores_reset", bundle)
    }

    /**
     * Logs support QR click on Help screen.
     */
    fun logSupportQrClicked() {
        firebaseAnalytics.logEvent("support_qr_clicked", null)
    }

    /**
     * Logs interstitial ad events.
     */
    fun logAdStatus(adType: String, status: String, errorCode: Int? = null) {
        val bundle = Bundle().apply {
            putString("ad_type", adType)
            putString("status", status)
            errorCode?.let { putInt("error_code", it) }
        }
        firebaseAnalytics.logEvent("ad_status", bundle)
    }
}
