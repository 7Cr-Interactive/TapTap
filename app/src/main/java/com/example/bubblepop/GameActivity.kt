package com.example.taptap

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import android.view.View
import android.graphics.Color
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var gameLayout: FrameLayout
    private lateinit var timerText: TextView
    private var score = 0
    private var missedBubbles = 0
    private var nailTapCount = 0
    private val maxMiss = 10
    private val maxNailTap = 3

    private var spawnInterval = 1000L
    private val minSpawnInterval = 300L
    private val intervalReduction = 100L
    private var elapsedTime = 0L
    private var timeInSeconds = 0

    private val random = Random(System.currentTimeMillis())
    private val handler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameLayout = findViewById(R.id.game_layout)
        timerText = findViewById(R.id.timer_text)

        startGameLoop()
        startTimer()

        val label = intent.getStringExtra("button_label")
        if (!label.isNullOrEmpty()) {
            Toast.makeText(this, "Game Started with: $label", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGameLoop() {
        handler.post(object : Runnable {
            override fun run() {
                spawnRandomItem()
                elapsedTime += spawnInterval

                if (elapsedTime >= 60000 && spawnInterval > minSpawnInterval) {
                    spawnInterval -= intervalReduction
                    elapsedTime = 0
                }

                handler.postDelayed(this, spawnInterval)
            }
        })
    }

    private fun startTimer() {
        timerHandler.post(object : Runnable {
            override fun run() {
                timeInSeconds++
                timerText.text = "Time: ${timeInSeconds}s"
                updateBackgroundColor()
                timerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun spawnRandomItem() {
        val item = if (random.nextBoolean()) createBubble() else createNail()
        gameLayout.addView(item)

        handler.postDelayed({
            if (gameLayout.indexOfChild(item) != -1) {
                gameLayout.removeView(item)
                if (item.tag == "bubble") {
                    missedBubbles++
                    if (missedBubbles >= maxMiss) endGame()
                }
            }
        }, 2000)
    }

    private fun createBubble(): View {
        val bubble = ImageView(this)
        bubble.setImageResource(R.drawable.bubble)
        bubble.tag = "bubble"
        bubble.layoutParams = generateRandomParams()
        bubble.setOnClickListener {
            score++
            missedBubbles = 0
            gameLayout.removeView(bubble)
        }
        return bubble
    }

    private fun createNail(): View {
        val nail = ImageView(this)
        nail.setImageResource(R.drawable.nail)
        nail.tag = "nail"
        nail.layoutParams = generateRandomParams()
        nail.setOnClickListener {
            score--
            nailTapCount++
            if (nailTapCount >= maxNailTap) endGame()
            gameLayout.removeView(nail)
        }
        return nail
    }

    private fun generateRandomParams(): FrameLayout.LayoutParams {
        val size = 150
        val x = random.nextInt(gameLayout.width - size)
        val y = random.nextInt(gameLayout.height - size)
        val params = FrameLayout.LayoutParams(size, size)
        params.leftMargin = x
        params.topMargin = y
        return params
    }

    private fun endGame() {
        handler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)

        val input = EditText(this)
        input.hint = "Enter Name"

        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("Your Score: $score")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString()
                saveScore(name, score)
                finish()
            }
            .show()
    }

    private fun saveScore(name: String, score: Int) {
        val prefs = getSharedPreferences("score_data", Context.MODE_PRIVATE)
        prefs.edit().putInt(name, score).apply()
    }

    private fun updateBackgroundColor() {
        val progress = ((1000L - spawnInterval).toFloat() / (1000L - minSpawnInterval)).coerceIn(0f, 1f)
        val red = (0 + (255 * progress)).toInt()
        val green = (255 - (255 * progress)).toInt()
        val color = Color.rgb(red, green, 0)
        gameLayout.setBackgroundColor(color)
    }
}
