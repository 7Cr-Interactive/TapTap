package com.app.bubblepop

import android.animation.*
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.*
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.RenderMode
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.random.Random
import android.animation.ObjectAnimator
import androidx.core.animation.doOnEnd

// Explicitly import components from the taptap package
import com.app.taptap.R
import com.app.taptap.BubbleView
import com.app.taptap.HighScoreManager
import com.app.taptap.HighScoreActivity

class GameActivity : AppCompatActivity() {

    private lateinit var gameLayout: FrameLayout
    private lateinit var scoreText: TextView
    private lateinit var comboPraiseText: TextView
    private lateinit var timeBar: ProgressBar
    private lateinit var modeIndicatorText: TextView

    private lateinit var gameMusic: MediaPlayer
    private lateinit var soundPool: SoundPool

    private var popSoundId = 0
    private var hitSoundId = 0

    private var score = 0
    private var combo = 0
    private var gameMode = "NORMAL"

    // ⏱️ Millisecond Core Engine Constants
    private var MAX_TIME_MS = 100000.0
    private var timeProgressMs = MAX_TIME_MS

    private val handler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())

    private var bubbleSpawner: Runnable? = null
    private var nailSpawner: Runnable? = null
    private lateinit var timerRunnable: Runnable

    private val TIMER_TICK_MS = 50L

    private var screenWidth = 0
    private var screenHeight = 0

    private var consecutiveNails = 0
    private var isDyingShakeActive = false
    private val praises = listOf("NICE!", "AWESOME!", "SUPER!", "FANTASTIC!", "PERFECT!")

    private var positiveTimeAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameMode = intent.getStringExtra("GAME_MODE") ?: "NORMAL"
        
        if (gameMode == "ZIP") {
            MAX_TIME_MS = 180000.0 // 3 minutes
        } else {
            MAX_TIME_MS = 100000.0
        }

        val bgAnim = findViewById<LottieAnimationView>(R.id.bgAnimation)
        bgAnim.scaleX = 1f
        bgAnim.scaleY = 1f
        startRandomBirds()
        bgAnim.setRenderMode(RenderMode.HARDWARE)
        bgAnim.repeatCount = LottieDrawable.INFINITE
        bgAnim.speed = 1f

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        gameLayout = findViewById(R.id.gameLayout)
        scoreText = findViewById(R.id.scoreText)
        
        modeIndicatorText = TextView(this).apply {
            text = if (gameMode == "ZIP") "3 MIN ZIP" else "NORMAL"
            textSize = 14f
            setTextColor(Color.WHITE)
            alpha = 0.5f
            gravity = Gravity.END
            val p = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            p.gravity = Gravity.TOP or Gravity.END
            p.setMargins(0, 40, 40, 0)
            layoutParams = p
        }
        gameLayout.addView(modeIndicatorText)

        timeBar = findViewById(R.id.timeBar)
        timeBar.max = MAX_TIME_MS.toInt()

        setupComboTextView()
        setupSoundPool()

        gameMusic = MediaPlayer.create(this, R.raw.game_music).apply {
            isLooping = true
            start()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { showExitDialog() }
        })

        animateSun()
        startFlowerParticles()
        startGame()
    }

    private fun animateSun() {
        val sun = findViewById<View>(R.id.sunView)
        val scaleX = ObjectAnimator.ofFloat(sun, View.SCALE_X, 1f, 1.08f)
        val scaleY = ObjectAnimator.ofFloat(sun, View.SCALE_Y, 1f, 1.08f)
        val floatY = ObjectAnimator.ofFloat(sun, View.TRANSLATION_Y, 0f, -20f)

        listOf(scaleX, scaleY, floatY).forEach {
            it.repeatCount = ValueAnimator.INFINITE
            it.repeatMode = ValueAnimator.REVERSE
        }

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, floatY)
            duration = 5000
            interpolator = android.view.animation.DecelerateInterpolator()
            start()
        }
    }

    // ---------------- DIRECTING STATE ----------------

    private fun shouldForceBubble(): Boolean {
        if (!hasBubbleOnScreen()) return true
        if (consecutiveNails >= 2) return true
        return false
    }

    private fun hasBubbleOnScreen(): Boolean {
        for (i in 0 until gameLayout.childCount) {
            if (gameLayout.getChildAt(i) is BubbleView) return true
        }
        return false
    }

    private fun getMaxNails(): Int {
        if (gameMode == "ZIP") return 5
        return when {
            score > 150 -> 5
            score > 90  -> 4
            score > 40  -> 3
            else -> 2
        }
    }

    private fun countNails(): Int {
        var c = 0
        for (i in 0 until gameLayout.childCount) {
            if (gameLayout.getChildAt(i).tag == "nail") c++
        }
        return c
    }

    private fun startRandomBirds() {
        val bird = findViewById<LottieAnimationView>(R.id.birdView)
        fun launchBird() {
            val flyLeftToRight = Random.nextBoolean()
            val startY = Random.nextInt(80, 350).toFloat()
            val startX = if (flyLeftToRight) -250f else screenWidth + 250f
            val endX = if (flyLeftToRight) screenWidth + 250f else -250f

            bird.scaleX = if (flyLeftToRight) 1f else -1f
            bird.apply {
                visibility = View.VISIBLE
                translationX = startX
                translationY = startY
                setAnimation("cute_bird.json")
                repeatCount = LottieDrawable.INFINITE
                speed = Random.nextFloat() * 0.4f + 0.8f
                playAnimation()
            }

            ObjectAnimator.ofFloat(bird, "translationX", startX, endX).apply {
                duration = Random.nextLong(5000, 9000)
                start()
                doOnEnd {
                    bird.cancelAnimation()
                    bird.visibility = View.GONE
                    handler.postDelayed({ launchBird() }, Random.nextLong(8000, 20000))
                }
            }
        }
        handler.postDelayed({ launchBird() }, 5000)
    }

    // ---------------- GAME START ----------------

    private fun startGame() {
        score = 0
        combo = 0
        timeProgressMs = MAX_TIME_MS
        consecutiveNails = 0
        isDyingShakeActive = false

        updateScore()
        updateTimeBarDirect()
        startTimer()

        startBubbleSpawner()
        startNailSpawner()
    }

    // ---------------- SPAWN ENGINE ----------------

    private fun startBubbleSpawner() {
        bubbleSpawner = object : Runnable {
            override fun run() {
                spawnBubble()
                val interval = if (gameMode == "ZIP") {
                    400L
                } else {
                    (1100 - (score * 2.5)).toLong().coerceAtLeast(450L)
                }
                handler.postDelayed(this, interval)
            }
        }
        handler.post(bubbleSpawner!!)
    }

    private fun startNailSpawner() {
        nailSpawner = object : Runnable {
            override fun run() {
                if (!shouldForceBubble() && countNails() < getMaxNails()) {
                    spawnNail()
                    consecutiveNails++
                }
                val interval = if (gameMode == "ZIP") {
                    650L
                } else {
                    (1600L - (score * 3L)).coerceAtLeast(700L)
                }
                handler.postDelayed(this, interval)
            }
        }
        handler.post(nailSpawner!!)
    }

    private fun spawnBubble() {
        // ZIP Mode bubbles: random large size (350-550) and no shrinking.
        // NORMAL mode: standard shrinking mechanic (down to 100).
        val size = if (gameMode == "ZIP") {
            Random.nextInt(350, 551)
        } else {
            (240 - (score * 0.4f).toInt()).coerceAtLeast(100)
        }
        
        val view = BubbleView(this)

        val params = FrameLayout.LayoutParams(size, size)
        params.leftMargin = Random.nextInt(0, (screenWidth - size).coerceAtLeast(1))
        // Position buffer to keep bubbles within viewable area
        val topFloor = 200
        val topCeiling = (screenHeight - size - 150).coerceAtLeast(topFloor + 1)
        params.topMargin = Random.nextInt(topFloor, topCeiling)
        view.layoutParams = params

        val despawnRunnable = Runnable {
            if (view.parent != null) {
                gameLayout.removeView(view)
                combo = 0
            }
        }

        view.setOnClickListener {
            handler.removeCallbacks(despawnRunnable)
            combo++
            score += if (combo > 10) 2 else 1
            updateScore()

            if (combo % 5 == 0) showPraise()

            val pitch = (1f + combo * 0.02f).coerceAtMost(1.5f)
            soundPool.play(popSoundId, 1f, 1f, 1, 0, pitch)

            showParticleBurst(view.x + size / 2, view.y + size / 2)
            playPopAnimation(view)

            if (gameMode == "NORMAL") {
                val refillMs = (8000.0 - (score * 15.0)).coerceAtLeast(4000.0)
                val comboBonusMs = (combo * 150.0).coerceAtMost(3000.0)

                positiveTimeAnimator?.cancel()
                val oldTime = timeProgressMs
                timeProgressMs = (timeProgressMs + refillMs + comboBonusMs).coerceAtMost(MAX_TIME_MS)
                animatePositiveTimeGain(oldTime.toInt(), timeProgressMs.toInt())
            }

            consecutiveNails = 0
        }

        gameLayout.addView(view)

        val lifetime = if (gameMode == "ZIP") {
            600L
        } else {
            (2200L - (score * 5L)).coerceAtLeast(600L)
        }
        handler.postDelayed(despawnRunnable, lifetime)
    }

    private fun spawnNail() {
        val size = if (gameMode == "ZIP") {
            280
        } else {
            (160 + (score * 0.6f).toInt()).coerceAtMost(320)
        }

        val view = ImageView(this).apply {
            setImageResource(R.drawable.nail)
            tag = "nail"
            rotation = Random.nextFloat() * 360f
            scaleX = 0f
            scaleY = 0f
            animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }

        val params = FrameLayout.LayoutParams(size, size)
        params.leftMargin = Random.nextInt(0, (screenWidth - size).coerceAtLeast(1))
        val topFloor = 200
        val topCeiling = (screenHeight - size - 150).coerceAtLeast(topFloor + 1)
        params.topMargin = Random.nextInt(topFloor, topCeiling)
        view.layoutParams = params

        val despawnRunnable = Runnable {
            if (view.parent != null) {
                view.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction { gameLayout.removeView(view) }
                    .start()
            }
        }

        view.setOnClickListener {
            handler.removeCallbacks(despawnRunnable)
            combo = 0
            score = (score - 3).coerceAtLeast(0)
            updateScore()

            soundPool.play(hitSoundId, 1f, 1f, 1, 0, 0.65f)
            shakeScreen()
            showParticleBurst(view.x + size / 2, view.y + size / 2)

            if (gameMode == "NORMAL") {
                positiveTimeAnimator?.cancel()
                timeProgressMs = (timeProgressMs - 10000.0).coerceAtLeast(0.0)
                updateTimeBarDirect()
            }

            if (view.parent != null) gameLayout.removeView(view)
        }

        gameLayout.addView(view)

        val lifetime = if (gameMode == "ZIP") {
            800L
        } else {
            (2400L - (score * 4L)).coerceAtLeast(900L)
        }
        handler.postDelayed(despawnRunnable, lifetime)
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                val isAnimatingGain = positiveTimeAnimator?.isRunning == true

                val drainPerSec = if (gameMode == "ZIP") {
                    1000.0
                } else {
                    3500.0 + (score * 30.0)
                }

                val frameDrain = drainPerSec * (TIMER_TICK_MS / 1000.0)
                timeProgressMs -= frameDrain

                if (timeProgressMs <= 0.0) {
                    timeProgressMs = 0.0
                    positiveTimeAnimator?.cancel()
                    updateTimeBarDirect()
                    gameOver()
                } else {
                    if (!isAnimatingGain) {
                        updateTimeBarDirect()
                    }
                    timerHandler.postDelayed(this, TIMER_TICK_MS)
                }
            }
        }
        timerHandler.postDelayed(timerRunnable, TIMER_TICK_MS)
    }

    private fun updateScore() {
        scoreText.text = "Score: $score"
        scoreText.setTextColor(if (combo >= 10) Color.YELLOW else Color.WHITE)
    }

    private fun setupComboTextView() {
        comboPraiseText = TextView(this).apply {
            textSize = 36f
            setTypeface(null, Typeface.BOLD_ITALIC)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        gameLayout.addView(comboPraiseText)
    }

    private fun showPraise() {
        comboPraiseText.text = "${praises.random()}\n$combo COMBO"
        val colors = listOf(
            Color.parseColor("#FFEB3B"), Color.parseColor("#00E5FF"),
            Color.parseColor("#FF4081"), Color.parseColor("#76FF03"),
            Color.parseColor("#FF9100"), Color.parseColor("#00FFAA")
        )
        val color = colors.random()
        comboPraiseText.setTextColor(color)
        comboPraiseText.setShadowLayer(15f, 0f, 0f, color)

        comboPraiseText.visibility = View.VISIBLE
        comboPraiseText.alpha = 0f
        comboPraiseText.scaleX = 0.5f
        comboPraiseText.scaleY = 0.5f

        comboPraiseText.animate()
            .alpha(1f)
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction {
                comboPraiseText.animate()
                    .alpha(0f)
                    .scaleX(0.8f)
                    .scaleY(0.8f)
                    .setDuration(300)
                    .withEndAction { comboPraiseText.visibility = View.GONE }
            }
    }

    private fun updateTimeBarDirect() {
        val targetProgress = timeProgressMs.toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            timeBar.setProgress(targetProgress, false)
        } else {
            timeBar.progress = targetProgress
        }

        updateTimeBarColor(timeProgressMs)
    }

    private fun animatePositiveTimeGain(startVal: Int, endVal: Int) {
        positiveTimeAnimator = ValueAnimator.ofInt(startVal, endVal).apply {
            duration = 120
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { animator ->
                val animVal = animator.animatedValue as Int
                if (animVal > timeProgressMs) {
                    timeBar.progress = animVal
                    updateTimeBarColor(animVal.toDouble())
                }
            }
            start()
        }
    }

    private fun updateTimeBarColor(valueMs: Double) {
        val percentage = (valueMs / MAX_TIME_MS) * 100.0
        when {
            percentage > 55.0 -> {
                timeBar.progressTintList = ColorStateList.valueOf(Color.GREEN)
                isDyingShakeActive = false
            }
            percentage > 25.0 -> {
                timeBar.progressTintList = ColorStateList.valueOf(Color.YELLOW)
                isDyingShakeActive = false
            }
            else -> {
                timeBar.progressTintList = ColorStateList.valueOf(Color.RED)
                if (percentage < 12.0 && !isDyingShakeActive) {
                    isDyingShakeActive = true
                    shakeScreen()
                }
            }
        }
    }

    private fun playPopAnimation(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.2f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.2f)
        val snapX = ObjectAnimator.ofFloat(view, "scaleX", 1.2f, 0f)
        val snapY = ObjectAnimator.ofFloat(view, "scaleY", 1.2f, 0f)
        val fade = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        AnimatorSet().apply {
            playTogether(sx, sy)
            play(snapX).with(snapY).with(fade).after(40)
            duration = 120
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (view.parent != null) gameLayout.removeView(view)
                }
            })
            start()
        }
    }

    private fun showParticleBurst(x: Float, y: Float) {
        repeat(12) {
            val p = ImageView(this).apply {
                setImageResource(R.drawable.particle)
                val s = Random.nextInt(12, 24)
                layoutParams = FrameLayout.LayoutParams(s, s)
                this.x = x
                this.y = y
            }
            gameLayout.addView(p)

            val angle = Math.toRadians(Random.nextInt(360).toDouble())
            val dist = Random.nextInt(100, 350).toFloat()
            val tx = x + (Math.cos(angle) * dist).toFloat()
            val ty = y + (Math.sin(angle) * dist).toFloat()

            val ax = ObjectAnimator.ofFloat(p, "translationX", x, tx)
            val ay = ObjectAnimator.ofFloat(p, "translationY", y, ty)
            val fade = ObjectAnimator.ofFloat(p, "alpha", 1f, 0f)

            AnimatorSet().apply {
                duration = Random.nextLong(350, 600)
                playTogether(ax, ay, fade)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) { gameLayout.removeView(p) }
                })
                start()
            }
        }
    }

    private fun shakeScreen() {
        ObjectAnimator.ofFloat(gameLayout, "translationX", 0f, 20f, -20f, 10f, -10f, 0f)
            .setDuration(400).start()
    }

    private fun gameOver() {
        gameMusic.stop()
        handler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)

        val input = EditText(this).apply { hint = "Name"; gravity = Gravity.CENTER }

        AlertDialog.Builder(this)
            .setTitle("Game Over! Score: $score")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Submit") { _, _ ->
                HighScoreManager.saveScore(this, input.text.toString().ifEmpty { "Player" }, score, gameMode)
                startActivity(Intent(this, HighScoreActivity::class.java))
                finish()
            }.show()
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit?")
            .setMessage("Abandon score of $score?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun setupSoundPool() {
        val attr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()
        soundPool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attr).build()
        popSoundId = soundPool.load(this, R.raw.bubble_pop, 1)
        hitSoundId = soundPool.load(this, R.raw.bubble_pop, 1)
    }

    private fun startFlowerParticles() {
        val layer = findViewById<FrameLayout>(R.id.particleLayer)
        val particleHandler = Handler(Looper.getMainLooper())
        val spawn = object : Runnable {
            override fun run() {
                spawnFlower(layer)
                particleHandler.postDelayed(this, Random.nextLong(1500, 3500))
            }
        }
        particleHandler.post(spawn)
    }

    private fun spawnFlower(parent: FrameLayout) {
        val size = Random.nextInt(8, 16)
        val flower = View(this).apply {
            background = getDrawable(R.drawable.flower_dot)
            alpha = Random.nextFloat() * 0.4f + 0.3f
            layoutParams = FrameLayout.LayoutParams(size, size)
        }
        parent.addView(flower)

        val startX = Random.nextInt(0, screenWidth)
        val startY = screenHeight + 50
        flower.x = startX.toFloat()
        flower.y = startY.toFloat()

        val driftX = Random.nextInt(-80, 80).toFloat()
        val driftY = -Random.nextInt(400, 700).toFloat()

        val moveX = ObjectAnimator.ofFloat(flower, "translationX", startX.toFloat(), startX + driftX)
        val moveY = ObjectAnimator.ofFloat(flower, "translationY", startY.toFloat(), startY + driftY)
        val fade = ObjectAnimator.ofFloat(flower, "alpha", flower.alpha, 0f)

        AnimatorSet().apply {
            duration = Random.nextLong(5000, 8000)
            playTogether(moveX, moveY, fade)
            interpolator = android.view.animation.LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { parent.removeView(flower) }
            })
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)
        positiveTimeAnimator?.cancel()
        gameMusic.release()
        soundPool.release()
    }
}
