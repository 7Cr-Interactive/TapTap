package com.app.bubblepop

import android.animation.*
import android.app.AlertDialog
import android.content.Context
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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import kotlin.math.max
import kotlin.random.Random
import android.animation.ObjectAnimator
import androidx.core.animation.doOnEnd

// Explicitly import components from the taptap package
import com.app.taptap.R
import com.app.taptap.BubbleView
import com.app.taptap.HighScoreManager
import com.app.taptap.HighScoreActivity
import com.app.taptap.ads.AdManager
import com.app.taptap.analytics.AnalyticsManager

import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.app.taptap.ui.GameViewModel
import com.app.taptap.util.HapticManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GameActivity : AppCompatActivity() {

    @Inject lateinit var hapticManager: HapticManager
    @Inject lateinit var analyticsManager: AnalyticsManager
    @Inject lateinit var adManager: AdManager
    private val viewModel: GameViewModel by viewModels()

    private lateinit var gameLayout: FrameLayout
    private lateinit var scoreText: TextView
    private lateinit var comboPraiseText: TextView
    private lateinit var timeBar: ProgressBar
    private lateinit var modeIndicatorText: TextView
    private lateinit var powerUpStatusText: TextView

    private lateinit var gameMusic: MediaPlayer
    private lateinit var soundPool: SoundPool

    private var popSoundId = 0
    private var hitSoundId = 0

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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        gameLayout = findViewById(R.id.gameLayout)
        ViewCompat.setOnApplyWindowInsetsListener(gameLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        // Preload Ads
        adManager.preloadInterstitialAd(this)
        adManager.preloadRewardedAd(this)
        adManager.preloadRewardedInterstitialAd(this)

        val gameMode = intent.getStringExtra("GAME_MODE") ?: "NORMAL"
        val startWithBooster = intent.getBooleanExtra("START_BOOSTER", false)
        viewModel.initGame(gameMode, startWithBooster)
        analyticsManager.logScreenView("Game Screen", "GameActivity")
        analyticsManager.logGameStart(gameMode)

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

        scoreText = findViewById(R.id.scoreText)
        
        modeIndicatorText = TextView(this).apply {
            text = if (gameMode == "ZIP") "1 MIN ZIP" else "NORMAL"
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
        timeBar.max = viewModel.maxTimeMs.toInt()

        powerUpStatusText = findViewById(R.id.powerUpStatus)

        setupComboTextView()
        setupSoundPool()
        setupObservers()

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
        val score = viewModel.score.value
        val gameMode = viewModel.gameMode
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

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.score.collect { score ->
                scoreText.text = "Score: $score"
                scoreText.setTextColor(if (viewModel.combo.value >= 10) Color.YELLOW else Color.WHITE)
            }
        }

        lifecycleScope.launch {
            viewModel.combo.collect { combo ->
                if (combo % 5 == 0 && combo > 0) {
                    analyticsManager.logComboMilestone(combo)
                    showPraise()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.timeRemainingMs.collect { timeMs ->
                updateTimeBarDirect(timeMs)
            }
        }

        lifecycleScope.launch {
            viewModel.isGameOver.collect { isOver ->
                if (isOver) gameOver()
            }
        }

        lifecycleScope.launch {
            viewModel.isShieldActive.collect { active ->
                if (active) {
                    analyticsManager.logPowerUpActivated("SHIELD")
                    showPowerUpStatus("SHIELD ACTIVE!", Color.YELLOW)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.freezeDurationLeftMs.collect { duration ->
                if (duration > 0) {
                    analyticsManager.logPowerUpActivated("FREEZE")
                    showPowerUpStatus("FREEZE ACTIVE!", Color.CYAN)
                } else if (duration == 0L && powerUpStatusText.text == "FREEZE ACTIVE!") {
                    powerUpStatusText.visibility = View.GONE
                }
            }
        }
    }

    private fun updateTimeBarDirect(timeMs: Double) {
        val targetProgress = timeMs.toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            timeBar.setProgress(targetProgress, false)
        } else {
            timeBar.progress = targetProgress
        }
        updateTimeBarColor(timeMs)
    }

    // ---------------- GAME START ----------------

    private fun startGame() {
        startTimer()
        startBubbleSpawner()
        startNailSpawner()
    }

    // ---------------- SPAWN ENGINE ----------------

    private fun startBubbleSpawner() {
        bubbleSpawner = object : Runnable {
            override fun run() {
                spawnBubble()
                if (!hasEnoughBubbles()) {
                    spawnBubble()
                }
                
                val interval = if (viewModel.gameMode == "ZIP") {
                    200L
                } else {
                    (800 - (viewModel.score.value * 2.0)).toLong().coerceAtLeast(300L)
                }
                handler.postDelayed(this, interval)
            }
        }
        handler.post(bubbleSpawner!!)
    }

    private fun hasEnoughBubbles(): Boolean {
        var count = 0
        for (i in 0 until gameLayout.childCount) {
            val child = gameLayout.getChildAt(i)
            if (child is BubbleView && child.tag != "particle") count++
        }
        return count >= 4
    }

    private fun startNailSpawner() {
        nailSpawner = object : Runnable {
            override fun run() {
                if (!shouldForceBubble() && countNails() < getMaxNails()) {
                    spawnNail()
                    consecutiveNails++
                }
                val interval = if (viewModel.gameMode == "ZIP") {
                    400L
                } else {
                    (1600L - (viewModel.score.value * 5L)).coerceAtLeast(500L)
                }
                handler.postDelayed(this, interval)
            }
        }
        handler.post(nailSpawner!!)
    }

    private fun spawnBubble() {
        val size = if (viewModel.gameMode == "ZIP") {
            Random.nextInt(350, 551)
        } else {
            (360 - (viewModel.score.value * 0.4f).toInt()).coerceAtLeast(150)
        }
        
        val view = BubbleView(this)
        view.setShrink(viewModel.getShrinkFactor())

        val roll = Random.nextInt(100)
        when {
            roll < 3 -> view.type = BubbleView.BubbleType.FREEZE
            roll < 6 -> view.type = BubbleView.BubbleType.SHIELD
            roll < 9 -> view.type = BubbleView.BubbleType.BLAST
            roll < 12 -> view.type = BubbleView.BubbleType.RAINBOW
            else -> view.type = BubbleView.BubbleType.NORMAL
        }

        val params = FrameLayout.LayoutParams(size, size)
        params.leftMargin = Random.nextInt(0, (screenWidth - size).coerceAtLeast(1))
        val topFloor = 200
        val topCeiling = (screenHeight - size - 150).coerceAtLeast(topFloor + 1)
        params.topMargin = Random.nextInt(topFloor, topCeiling)
        view.layoutParams = params

        val despawnRunnable = Runnable {
            if (view.parent != null) {
                gameLayout.removeView(view)
            }
        }

        view.setOnClickListener {
            view.setOnClickListener(null)
            hapticManager.vibratePop()
            handler.removeCallbacks(despawnRunnable)
            
            analyticsManager.logBubblePop(view.type.name, viewModel.combo.value)

            when(view.type) {
                BubbleView.BubbleType.FREEZE -> viewModel.activateFreeze()
                BubbleView.BubbleType.SHIELD -> viewModel.activateShield()
                BubbleView.BubbleType.BLAST -> popAllBubbles()
                BubbleView.BubbleType.RAINBOW -> viewModel.incrementScore("RAINBOW")
                else -> viewModel.incrementScore("NORMAL")
            }
            
            val pitch = (1f + viewModel.combo.value * 0.02f).coerceAtMost(1.5f)
            soundPool.play(popSoundId, 1f, 1f, 1, 0, pitch)

            showParticleBurst(view.x + size / 2, view.y + size / 2)
            playPopAnimation(view)
            viewModel.addTimeBonus()
            consecutiveNails = 0
        }

        gameLayout.addView(view)
        
        val lifetime = if (viewModel.gameMode == "ZIP") {
            800L
        } else {
            (3500L - (viewModel.score.value * 5L)).coerceAtLeast(1500L)
        }
        handler.postDelayed(despawnRunnable, lifetime)
    }

    private fun spawnNail() {
        val size = if (viewModel.gameMode == "ZIP") {
            280
        } else {
            (160 + (viewModel.score.value * 0.6f).toInt()).coerceAtMost(320)
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
            hapticManager.vibrateHit()
            handler.removeCallbacks(despawnRunnable)
            
            val isShielded = viewModel.isShieldActive.value
            analyticsManager.logNailHit(isShielded, viewModel.score.value, viewModel.combo.value)

            if (isShielded) {
                viewModel.hitNail()
                showPowerUpStatus("SHIELD BROKEN!", Color.WHITE)
                if (view.parent != null) gameLayout.removeView(view)
                return@setOnClickListener
            }
            
            viewModel.hitNail()
            soundPool.play(hitSoundId, 1f, 1f, 1, 0, 0.65f)
            shakeScreen()
            showParticleBurst(view.x + size / 2, view.y + size / 2)

            if (view.parent != null) gameLayout.removeView(view)
        }

        gameLayout.addView(view)

        val lifetime = if (viewModel.gameMode == "ZIP") {
            700L
        } else {
            (2400L - (viewModel.score.value * 4L)).coerceAtLeast(900L)
        }
        handler.postDelayed(despawnRunnable, lifetime)
    }

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                viewModel.updateTime(TIMER_TICK_MS)
                timerHandler.postDelayed(this, TIMER_TICK_MS)
            }
        }
        timerHandler.postDelayed(timerRunnable, TIMER_TICK_MS)
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
        val combo = viewModel.combo.value
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

    private fun updateTimeBarColor(valueMs: Double) {
        val percentage = (valueMs / viewModel.maxTimeMs) * 100.0
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
        analyticsManager.logGameOver(viewModel.gameMode, viewModel.score.value, viewModel.combo.value)
        if (::gameMusic.isInitialized && gameMusic.isPlaying) {
            gameMusic.pause()
        }
        handler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)

        showGameOverDialog()
    }

    private fun popAllBubbles() {
        val bubblesToPop = mutableListOf<BubbleView>()
        var blastCenter = Pair(screenWidth / 2f, screenHeight / 2f)
        
        for (i in 0 until gameLayout.childCount) {
            val child = gameLayout.getChildAt(i)
            if (child is BubbleView && child.tag != "particle") {
                bubblesToPop.add(child)
                if (child.type == BubbleView.BubbleType.BLAST) {
                    blastCenter = Pair(child.x + child.width / 2, child.y + child.height / 2)
                }
            }
        }
        
        showBubbleClusterBurst(blastCenter.first, blastCenter.second)
        
        bubblesToPop.forEach { bubble ->
            viewModel.incrementScore("BLAST_COLLATERAL")
            playPopAnimation(bubble)
            showParticleBurst(bubble.x + bubble.width/2, bubble.y + bubble.height/2)
        }
        showPowerUpStatus("BLAST!", Color.RED)
    }

    private fun showBubbleClusterBurst(x: Float, y: Float) {
        repeat(20) {
            val size = Random.nextInt(40, 100)
            val p = BubbleView(this).apply {
                tag = "particle"
                layoutParams = FrameLayout.LayoutParams(size, size)
                this.x = x - size / 2
                this.y = y - size / 2
                alpha = 0.8f
            }
            gameLayout.addView(p)

            val angle = Math.toRadians(Random.nextInt(360).toDouble())
            val dist = Random.nextInt(200, 600).toFloat()
            val tx = x + (Math.cos(angle) * dist).toFloat() - size / 2
            val ty = y + (Math.sin(angle) * dist).toFloat() - size / 2

            val ax = ObjectAnimator.ofFloat(p, "x", p.x, tx)
            val ay = ObjectAnimator.ofFloat(p, "y", p.y, ty)
            val scaleX = ObjectAnimator.ofFloat(p, "scaleX", 1f, 1.5f, 0f)
            val scaleY = ObjectAnimator.ofFloat(p, "scaleY", 1f, 1.5f, 0f)
            val fade = ObjectAnimator.ofFloat(p, "alpha", 0.8f, 0f)

            AnimatorSet().apply {
                duration = Random.nextLong(600, 1000)
                playTogether(ax, ay, scaleX, scaleY, fade)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) { gameLayout.removeView(p) }
                })
                start()
            }
        }
    }

    private fun showPowerUpStatus(text: String, color: Int) {
        powerUpStatusText.text = text
        powerUpStatusText.setTextColor(color)
        powerUpStatusText.visibility = View.VISIBLE
        
        if (!text.contains("FREEZE") && !text.contains("SHIELD ACTIVE")) {
            handler.postDelayed({
                if (powerUpStatusText.text == text) {
                    powerUpStatusText.visibility = View.GONE
                }
            }, 2000)
        }
    }

    private fun reviveAndResumeGame() {
        viewModel.reviveGame()
        // Clean active views from screen
        for (i in gameLayout.childCount - 1 downTo 0) {
            val child = gameLayout.getChildAt(i)
            if (child is BubbleView || child.tag == "nail") {
                gameLayout.removeView(child)
            }
        }
        if (::gameMusic.isInitialized) {
            gameMusic.start()
        }
        startGame()
    }

    private fun showGameOverDialog() {
        val input = EditText(this).apply { hint = "Player Name"; gravity = Gravity.CENTER }

        val builder = AlertDialog.Builder(this)
            .setTitle("Game Over! Score: ${viewModel.score.value}")
            .setMessage("Watch a short ad to Revive or Double your Score!")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Submit & Exit") { _, _ ->
                val name = input.text.toString().ifEmpty { "Player" }
                adManager.showInterstitialAd(this) {
                    viewModel.saveScore(name)
                    finish()
                }
            }

        if (viewModel.canRevive()) {
            builder.setNeutralButton("💚 Revive (+30s & Shield)") { _, _ ->
                adManager.showRewardedAd(
                    activity = this,
                    onUserEarnedReward = {
                        reviveAndResumeGame()
                    },
                    onAdClosed = {
                        if (!viewModel.isShieldActive.value && viewModel.isGameOver.value) {
                            showGameOverDialog()
                        }
                    }
                )
            }
        } else if (viewModel.canDoubleScore()) {
            builder.setNegativeButton("⭐ 2x Score") { _, _ ->
                adManager.showRewardedInterstitialAd(
                    activity = this,
                    onUserEarnedReward = {
                        viewModel.doubleScore()
                    },
                    onAdClosed = {
                        showGameOverDialog()
                    }
                )
            }
        }

        builder.show()
    }

    private fun showExitDialog() {
        analyticsManager.logGameExit(viewModel.gameMode, viewModel.score.value)
        AlertDialog.Builder(this)
            .setTitle("Exit?")
            .setMessage("Abandon score of ${viewModel.score.value}?")
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
        if (::gameMusic.isInitialized) {
            gameMusic.release()
        }
        soundPool.release()
    }
}
