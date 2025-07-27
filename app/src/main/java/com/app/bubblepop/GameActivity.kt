package com.app.taptap

import android.animation.Animator
import android.animation.AnimatorSet
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
import android.animation.AnimatorListenerAdapter
import kotlin.random.Random
import android.animation.ObjectAnimator
import androidx.activity.OnBackPressedCallback
import android.os.Bundle


class GameActivity : AppCompatActivity() {
    private var missedBubbles = 0
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

    private var remainingTime = 5
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
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
        startGame()

    }

    private fun startGame() {
        remainingTime = 6 //start of counter
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
        remainingTime = (10 - (score / 10)).coerceAtLeast(1)
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
            Random.nextInt(150, 350)
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
                missedBubbles = 0 // Reset missed streak
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
                // Trigger particle burst at bubble location
                showParticleBurst(imageView.x + imageView.width / 2, imageView.y + imageView.height / 2)
                Handler(Looper.getMainLooper()).postDelayed({
                    if (gameLayout.indexOfChild(imageView) != -1) {
                        // Missed bubble
                        gameLayout.removeView(imageView)
                        missedBubbles++
                        if (missedBubbles >= 5) {
                            gameOver()
                        }
                    }
                }, 2000) // 2 seconds to respond
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

        // Prevent dialog crash on activity finish
        if (isFinishing || isDestroyed) return

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
    private fun showParticleBurst(x: Float, y: Float) {
        val particleCount = 10

        for (i in 0 until particleCount) {
            val particle = ImageView(this)
            particle.setImageResource(R.drawable.particle)

            val size = Random.nextInt(10, 30)
            val params = FrameLayout.LayoutParams(size, size)
            particle.layoutParams = params

            particle.x = x
            particle.y = y

            gameLayout.addView(particle)

            val animX = ObjectAnimator.ofFloat(particle, "translationX", x, x + Random.nextInt(-200, 200))
            val animY = ObjectAnimator.ofFloat(particle, "translationY", y, y + Random.nextInt(-200, 200))
            val fadeOut = ObjectAnimator.ofFloat(particle, "alpha", 1f, 0f)

            AnimatorSet().apply {
                duration = 500
                playTogether(animX as Animator, animY as Animator, fadeOut as Animator)
                start()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        gameLayout.removeView(particle)
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameMusic.release()
        popSound.release()
        hitSound.release()
        handler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)
    }
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit Game")
            .setMessage("Stop Playing and miss a chance to be top 10??")
            .setPositiveButton("Yes") { _, _ ->
                gameMusic.stop()
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }




}
