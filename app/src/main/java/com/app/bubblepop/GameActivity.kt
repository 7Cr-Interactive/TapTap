package com.app.taptap

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
import androidx.core.animation.doOnEnd
class GameActivity : AppCompatActivity() {

    private lateinit var gameLayout: FrameLayout
    private lateinit var scoreText: TextView
    private lateinit var comboPraiseText: TextView
    private lateinit var timeBar: ProgressBar

    private lateinit var gameMusic: MediaPlayer
    private lateinit var soundPool: SoundPool

    private var popSoundId = 0
    private var hitSoundId = 0

    private var score = 0
    private var combo = 0
    private var timeProgress = 100

    private val handler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())

    private var bubbleSpawner: Runnable? = null
    private var nailSpawner: Runnable? = null
    private lateinit var timerRunnable: Runnable

    private var screenWidth = 0
    private var screenHeight = 0

    // 🎯 Director state
    private var consecutiveNails = 0

    private val praises = listOf("NICE!", "AWESOME!", "SUPER!", "FANTASTIC!", "PERFECT!")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
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
        timeBar = findViewById(R.id.timeBar)

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

        // 🌟 Soft breathing scale
        val scaleX = ObjectAnimator.ofFloat(sun, View.SCALE_X, 1f, 1.08f)
        val scaleY = ObjectAnimator.ofFloat(sun, View.SCALE_Y, 1f, 1.08f)

        // 🌞 Gentle vertical float (very subtle)
        val floatY = ObjectAnimator.ofFloat(sun, View.TRANSLATION_Y, 0f, -20f)

        scaleX.repeatCount = ValueAnimator.INFINITE
        scaleX.repeatMode = ValueAnimator.REVERSE

        scaleY.repeatCount = ValueAnimator.INFINITE
        scaleY.repeatMode = ValueAnimator.REVERSE

        floatY.repeatCount = ValueAnimator.INFINITE
        floatY.repeatMode = ValueAnimator.REVERSE

        // 🎯 Combine animations
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, floatY)
            duration = 5000
            interpolator = android.view.animation.DecelerateInterpolator()
            start()
        }
    }
    // ---------------- DIRECTOR ----------------

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
        return when {
            score > 100 -> 4
            score > 50 -> 3
            else -> 2
        }
    }

    private fun countNails(): Int {
        var c = 0
        for (i in 0 until gameLayout.childCount) {
            if (gameLayout.getChildAt(i) is ImageView) c++
        }
        return c
    }
    private fun startRandomBirds() {

        val bird = findViewById<LottieAnimationView>(R.id.birdView)

        fun launchBird() {

            val flyLeftToRight = Random.nextBoolean()

            val startY = Random.nextInt(80, 350).toFloat()

            val startX: Float
            val endX: Float

            if (flyLeftToRight) {

                startX = -250f
                endX = screenWidth + 250f

                bird.scaleX = 1f

            } else {

                startX = screenWidth + 250f
                endX = -250f

                // mirror bird
                bird.scaleX = -1f
            }

            bird.apply {

                visibility = View.VISIBLE

                translationX = startX
                translationY = startY

                setAnimation("cute_bird.json")

                repeatCount = LottieDrawable.INFINITE

                speed = Random.nextFloat() * 0.4f + 0.8f

                playAnimation()
            }

            ObjectAnimator.ofFloat(
                bird,
                "translationX",
                startX,
                endX
            ).apply {

                duration = Random.nextLong(5000, 9000)

                start()

                doOnEnd {

                    bird.cancelAnimation()

                    bird.visibility = View.GONE

                    // random next appearance
                    handler.postDelayed(
                        { launchBird() },
                        Random.nextLong(8000, 20000)
                    )
                }
            }
        }

        // first appearance
        handler.postDelayed(
            { launchBird() },
            5000
        )
    }
    // ---------------- GAME START ----------------

    private fun startGame() {
        score = 0
        combo = 0
        timeProgress = 100
        consecutiveNails = 0

        updateScore()
        updateTimeBar()
        startTimer()

        startBubbleSpawner()
        startNailSpawner()
    }

    // ---------------- SPAWN SYSTEM ----------------

    private fun startBubbleSpawner() {
        bubbleSpawner = object : Runnable {
            override fun run() {

                spawnBubble()

                val progress = (score / 150f).coerceIn(0f, 1f)
                val eased = 1f - (1f - progress) * (1f - progress)

                val interval = (900 - (900 - 350) * eased).toLong()

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

                val interval = (1200L - score * 2L).coerceAtLeast(600L)

                handler.postDelayed(this, interval)
            }
        }
        handler.post(nailSpawner!!)
    }
    private fun spawnBubble() {

        val size = Random.nextInt(180, 280)
        val view = BubbleView(this)

        val params = FrameLayout.LayoutParams(size, size)
        params.leftMargin = Random.nextInt(0, (screenWidth - size).coerceAtLeast(1))
        params.topMargin = Random.nextInt(200, (screenHeight - size - 100).coerceAtLeast(1))
        view.layoutParams = params

        view.setOnClickListener {

            combo++
            score += if (combo > 10) 2 else 1
            updateScore()

            if (combo % 5 == 0) showPraise()

            val pitch = (1f + combo * 0.02f).coerceAtMost(1.5f)
            soundPool.play(popSoundId, 1f, 1f, 1, 0, pitch)

            showParticleBurst(view.x + size / 2, view.y + size / 2)
            playPopAnimation(view)
            val refill = when {
                score > 150 -> 6
                score > 100 -> 8
                score > 70 -> 10
                score > 40 -> 12
                else -> 15
            }
            val comboBonus = (combo / 10).coerceAtMost(5)

            timeProgress = (timeProgress + refill + comboBonus).coerceAtMost(100)
            animateTimeBar(timeProgress)

            consecutiveNails = 0
        }

        gameLayout.addView(view)

        handler.postDelayed({
            if (view.parent != null) {
                gameLayout.removeView(view)
                combo = 0
            }
        }, (1800L - score * 5L).coerceAtLeast(900))
    }

    private fun spawnNail() {

        // 🎯 Base size randomness
        val baseSize = Random.nextInt(200, 320)

        // 📈 Difficulty scaling (smaller nails at higher score)
        val sizeReduction = score / 20
        val size = (baseSize - sizeReduction).coerceAtLeast(140)

        val view = ImageView(this).apply {
            setImageResource(R.drawable.nail)

            // 🔥 Random rotation (adds visual variety)
            rotation = Random.nextFloat() * 360f

            // 🎯 Spawn pop-in animation
            scaleX = 0f
            scaleY = 0f
            animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }

        val params = FrameLayout.LayoutParams(size, size)
        params.leftMargin = Random.nextInt(0, (screenWidth - size).coerceAtLeast(1))
        params.topMargin = Random.nextInt(200, (screenHeight - size - 100).coerceAtLeast(1))
        view.layoutParams = params

        view.setOnClickListener {

            combo = 0
            score = (score - 2).coerceAtLeast(0)
            updateScore()

            soundPool.play(hitSoundId, 1f, 1f, 1, 0, 0.8f)

            // 💥 stronger feedback for mistake
            shakeScreen()

            // 🔥 small burst for impact (reuse your particle system)
            showParticleBurst(view.x + size / 2, view.y + size / 2)

            if (view.parent != null) gameLayout.removeView(view)
        }

        gameLayout.addView(view)

        // ⏱️ Despawn scales with difficulty
        val despawn = (1800L - score * 5L).coerceAtLeast(900L)

        handler.postDelayed({
            if (view.parent != null) {
                // 💡 Optional: fade out instead of instant remove
                view.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction { gameLayout.removeView(view) }
            }
        }, despawn)
    }

    // ---------------- TIMER ----------------

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {

                val drain = when {
                    score > 150 -> 14
                    score > 100 -> 12
                    score > 70 -> 10
                    score > 40 -> 8
                    else -> 5
                }

                timeProgress -= drain

                if (timeProgress <= 0) gameOver()
                else {
                    animateTimeBar(timeProgress)
                    timerHandler.postDelayed(this, 1000)
                }
            }
        }
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    // ---------------- UI ----------------

    private fun updateScore() {
        scoreText.text = "Score: $score"
        scoreText.setTextColor(if (combo >= 10) Color.YELLOW else Color.WHITE)
    }

    private fun setupComboTextView() {
        comboPraiseText = TextView(this).apply {
            textSize = 40f
            setTypeface(null, Typeface.BOLD_ITALIC)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        gameLayout.addView(comboPraiseText)
    }

    private fun showPraise() {

        comboPraiseText.text = "${praises.random()}\n$combo COMBO"

        // 🎨 Bright random colors (vibrant palette)
        val colors = listOf(
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#00E5FF"), // Cyan neon
            Color.parseColor("#FF4081"), // Pink
            Color.parseColor("#76FF03"), // Lime
            Color.parseColor("#FF9100"), // Orange
            Color.parseColor("#E040FB"), // Purple
            Color.parseColor("#00FFAA")  // Aqua green
        )

        val color = colors.random()

        comboPraiseText.setTextColor(color)

        // ✨ subtle glow effect (shadow)
        comboPraiseText.setShadowLayer(
            20f,
            0f,
            0f,
            color
        )

        comboPraiseText.visibility = View.VISIBLE
        comboPraiseText.alpha = 0f
        comboPraiseText.scaleX = 0.5f
        comboPraiseText.scaleY = 0.5f

        comboPraiseText.animate()
            .alpha(1f)
            .scaleX(1.3f)
            .scaleY(1.3f)
            .setDuration(250)
            .withEndAction {

                comboPraiseText.animate()
                    .alpha(0f)
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .setDuration(400)
                    .withEndAction {
                        comboPraiseText.visibility = View.GONE
                    }
            }
    }

    // ---------------- TIME BAR ----------------

    private fun animateTimeBar(value: Int) {
        ObjectAnimator.ofInt(timeBar, "progress", timeBar.progress, value).apply {
            duration = 900 // match ~1 second cycle
            interpolator = android.view.animation.LinearInterpolator()
            start()
        }
        updateTimeBarColor(value)
    }

    private fun updateTimeBar() {
        timeBar.progress = timeProgress
        updateTimeBarColor(timeProgress)
    }

    private fun updateTimeBarColor(value: Int) {
        when {
            value > 60 -> timeBar.progressTintList = ColorStateList.valueOf(Color.GREEN)
            value > 30 -> timeBar.progressTintList = ColorStateList.valueOf(Color.YELLOW)
            else -> {
                timeBar.progressTintList = ColorStateList.valueOf(Color.RED)
                if (value < 20) shakeScreen()
            }
        }
    }

    // ---------------- EFFECTS ----------------

    private fun playPopAnimation(view: View) {
        val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.25f)
        val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.25f)
        val snapX = ObjectAnimator.ofFloat(view, "scaleX", 1.25f, 0f)
        val snapY = ObjectAnimator.ofFloat(view, "scaleY", 1.25f, 0f)
        val fade = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        AnimatorSet().apply {
            playTogether(sx, sy)
            play(snapX).with(snapY).with(fade).after(50)
            duration = 150
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (view.parent != null) gameLayout.removeView(view)
                }
            })
            start()
        }
    }

    private fun showParticleBurst(x: Float, y: Float) {
        repeat(15) {
            val p = ImageView(this).apply {
                setImageResource(R.drawable.particle)
                val s = Random.nextInt(10, 25)
                layoutParams = FrameLayout.LayoutParams(s, s)
                this.x = x
                this.y = y
            }
            gameLayout.addView(p)

            val angle = Math.toRadians(Random.nextInt(360).toDouble())
            val dist = Random.nextInt(150, 450).toFloat()

            val tx = x + (Math.cos(angle) * dist).toFloat()
            val ty = y + (Math.sin(angle) * dist).toFloat()

            val ax = ObjectAnimator.ofFloat(p, "translationX", x, tx)
            val ay = ObjectAnimator.ofFloat(p, "translationY", y, ty)
            val fade = ObjectAnimator.ofFloat(p, "alpha", 1f, 0f)

            AnimatorSet().apply {
                duration = Random.nextLong(400, 700)
                playTogether(ax, ay, fade)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) {
                        gameLayout.removeView(p)
                    }
                })
                start()
            }
        }
    }

    private fun shakeScreen() {
        ObjectAnimator.ofFloat(gameLayout, "translationX", 0f, 25f, -25f, 0f)
            .setDuration(300).start()
    }

    // ---------------- GAME OVER ----------------

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
                HighScoreManager.saveScore(this,
                    input.text.toString().ifEmpty { "Player" }, score)
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
        val attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attr)
            .build()

        popSoundId = soundPool.load(this, R.raw.bubble_pop, 1)
        hitSoundId = soundPool.load(this, R.raw.bubble_pop, 1)
    }


    private fun startFlowerParticles() {
        val layer = findViewById<FrameLayout>(R.id.particleLayer)

        val handler = Handler(Looper.getMainLooper())

        val spawn = object : Runnable {
            override fun run() {
                spawnFlower(layer)
                handler.postDelayed(this, Random.nextLong(1200, 3000))
            }
        }

        handler.post(spawn)
    }

    private fun spawnFlower(parent: FrameLayout) {

        val size = Random.nextInt(6, 12)

        val flower = View(this).apply {
            background = getDrawable(R.drawable.flower_dot)
            alpha = Random.nextFloat() * 0.5f + 0.3f
            layoutParams = FrameLayout.LayoutParams(size, size)
        }

        parent.addView(flower)

        val startX = Random.nextInt(0, screenWidth)
        val startY = screenHeight + 50

        flower.x = startX.toFloat()
        flower.y = startY.toFloat()

        val driftX = Random.nextInt(-100, 100).toFloat()
        val driftY = -Random.nextInt(300, 600).toFloat()

        val moveX = ObjectAnimator.ofFloat(flower, "translationX", startX.toFloat(), startX + driftX)
        val moveY = ObjectAnimator.ofFloat(flower, "translationY", startY.toFloat(), startY + driftY)
        val fade = ObjectAnimator.ofFloat(flower, "alpha", flower.alpha, 0f)

        AnimatorSet().apply {
            duration = Random.nextLong(4000, 7000)
            playTogether(moveX, moveY, fade)
            interpolator = android.view.animation.LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    parent.removeView(flower)
                }
            })
            start()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        gameMusic.release()
        soundPool.release()
    }
}