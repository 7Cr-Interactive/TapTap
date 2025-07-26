package com.example.taptap

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.*
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var gameLayout: FrameLayout
    private lateinit var scoreText: TextView
    private lateinit var timerText: TextView
    private lateinit var gameMusic: MediaPlayer
    private lateinit var popSound: MediaPlayer
    private lateinit var hitSound: MediaPlayer
    private var score = 0
    private val handler = Handler(Looper.getMainLooper())

    private var currentSpawnInterval = 700L
    private val minSpawnInterval = 300L
    private var spawnDecreaseStep = 20L
    private var lastScoreCheckpoint = 0
    private var spawnRunnable: Runnable? = null

    private var remainingTime = 10
    private var timerHandler = Handler(Looper.getMainLooper())
    private lateinit var timerRunnable: Runnable

    private var consecutiveNails = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        enableImmersiveMode()

        gameLayout = findViewById(R.id.gameLayout)
        scoreText = findViewById(R.id.scoreText)
        timerText = findViewById(R.id.timerText)

        gameMusic = MediaPlayer.create(this, R.raw.game_music).apply {
            isLooping = true
            start()
        }

        popSound = MediaPlayer.create(this, R.raw.bubble_pop)
        hitSound = MediaPlayer.create(this, R.raw.bubble_pop) // Replace with nail hit sound if needed

        startGame()
    }

    private fun startGame() {
        remainingTime = 10
        startTimer()

        spawnRunnable = object : Runnable {
            override fun run() {
                spawnRandomItem()
                handler.postDelayed(this, currentSpawnInterval)
            }
        }
        handler.post(spawnRunnable!!)
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                remainingTime--
                if (remainingTime <= 0) {
                    gameOver()
                } else {
                    timerText.text = "Time: ${remainingTime}s"
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerText.text = "Time: ${remainingTime}s"
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun resetTimer() {
        remainingTime = 10
        timerText.text = "Time: ${remainingTime}s"
    }

    private fun spawnRandomItem() {
        val imageView = ImageView(this)
        val nailChance = minOf(50, 30 + score / 5)  // max 50% nails
        val isBubble = Random.nextInt(100) >= nailChance
        imageView.alpha = 0.85f
        imageView.setBackgroundColor(Color.TRANSPARENT)
        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        val size = if (isBubble) {
            Random.nextInt(200, 350)
        } else {
            Random.nextInt(400, 500)
        }

        val params = FrameLayout.LayoutParams(size, size)
        val maxLeft = gameLayout.width - size
        val maxTop = gameLayout.height - size
        if (maxLeft <= 0 || maxTop <= 0) return  // Avoid crash before layout is ready

        params.leftMargin = Random.nextInt(0, maxLeft)
        params.topMargin = Random.nextInt(100, maxTop)
        imageView.layoutParams = params

        if (isBubble) {
            imageView.setImageResource(R.drawable.bubble)
            imageView.setOnClickListener {
                score++
                consecutiveNails = 0
                updateScore()

                playPopAnimation(imageView) {
                    gameLayout.removeView(imageView)
                }
                popSound.start()

                resetTimer()

                if (score % 10 == 0) {
                    remainingTime = (remainingTime - 1).coerceAtLeast(3)
                }

                if (score % 5 == 0 && score != lastScoreCheckpoint) {
                    lastScoreCheckpoint = score
                    if (currentSpawnInterval > minSpawnInterval) {
                        spawnDecreaseStep += 10
                        currentSpawnInterval =
                            (currentSpawnInterval - spawnDecreaseStep).coerceAtLeast(minSpawnInterval)
                    }
                }
            }
        } else {
            imageView.setImageResource(R.drawable.nail)
            imageView.setOnClickListener {
                score--
                updateScore()
                hitSound.start()
                gameLayout.removeView(imageView)
            }
        }

        runOnUiThread {
            gameLayout.addView(imageView)
        }

        handler.postDelayed({ gameLayout.removeView(imageView) }, 1500)
    }

    private fun playPopAnimation(view: ImageView, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                onEnd()
            }
            .start()
    }

    private fun updateScore() {
        scoreText.text = "Score: $score"
    }

    private fun gameOver() {
        gameMusic.stop()
        spawnRunnable?.let { handler.removeCallbacks(it) }
        timerHandler.removeCallbacks(timerRunnable)

        val input = EditText(this).apply {
            hint = "Your name"
            gravity = Gravity.CENTER
        }

        AlertDialog.Builder(this)
            .setTitle("Game Over! Score: $score")
            .setMessage("Enter your name:")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Submit") { _, _ ->
                val name = input.text.toString()
                HighScoreManager.saveScore(this, name, score)

                val intent = Intent(this, HighScoreActivity::class.java)
                intent.putExtra("SHOW_FOR_5_SEC", true)
                startActivity(intent)

                handler.postDelayed({
                    val backToMain = Intent(this, MainActivity::class.java)
                    backToMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(backToMain)
                    finish()
                }, 5000)
            }
            .show()
    }

    private fun enableImmersiveMode() {
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        gameMusic.release()
        popSound.release()
        hitSound.release()
        handler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)
    }
}
