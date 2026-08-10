package com.app.taptap

import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class HighScoreActivity : AppCompatActivity() {

    private var currentMode = "NORMAL"
    private lateinit var soundPool: SoundPool
    private var bubbleSoundId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_score)

        val root = findViewById<View>(R.id.main_high_score_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        setupSoundPool()

        val normalScoresBtn = findViewById<Button>(R.id.normalScoresBtn)
        val zipScoresBtn = findViewById<Button>(R.id.zipScoresBtn)
        val backButton = findViewById<Button>(R.id.backButton)
        val resetScoresButton = findViewById<Button>(R.id.resetScoresButton)

        loadScores("NORMAL")

        normalScoresBtn.setOnClickListener {
            playClickSound()
            currentMode = "NORMAL"
            loadScores("NORMAL")
        }

        zipScoresBtn.setOnClickListener {
            playClickSound()
            currentMode = "ZIP"
            loadScores("ZIP")
        }

        backButton.setOnClickListener {
            playClickSound()
            finish()
        }

        resetScoresButton.setOnClickListener {
            playClickSound()
            AlertDialog.Builder(this)
                .setTitle("Reset $currentMode Scores")
                .setMessage("Are you sure you want to delete all high scores for $currentMode mode?")
                .setPositiveButton("Yes") { _, _ ->
                    playClickSound()
                    HighScoreManager.clearScores(this, currentMode)
                    loadScores(currentMode)
                }
                .setNegativeButton("Cancel") { _, _ ->
                    playClickSound()
                }
                .show()
        }
    }

    private fun setupSoundPool() {
        val attr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()
        soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attr).build()
        bubbleSoundId = soundPool.load(this, R.raw.bubble_pop, 1)
    }

    private fun playClickSound() {
        if (::soundPool.isInitialized) {
            soundPool.play(bubbleSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun loadScores(mode: String) {
        val scoreListLayout = findViewById<LinearLayout>(R.id.scoreList)
        scoreListLayout.removeAllViews()

        val scores = HighScoreManager.getScores(this, mode).take(10)

        if (scores.isEmpty()) {
            val textView = TextView(this).apply {
                text = "No scores yet!"
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 50, 0, 0)
            }
            scoreListLayout.addView(textView)
        } else {
            scores.forEachIndexed { index, pair ->
                val (name, score) = pair
                val textView = TextView(this).apply {
                    text = "${index + 1}. $name - $score"
                    textSize = 22f
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(8, 24, 8, 24)
                }
                scoreListLayout.addView(textView)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundPool.isInitialized) {
            soundPool.release()
        }
    }
}
