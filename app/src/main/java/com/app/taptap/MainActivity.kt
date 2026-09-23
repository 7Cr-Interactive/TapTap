package com.app.taptap

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.widget.TextView
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.bubblepop.GameActivity
import com.app.taptap.ads.AdManager
import com.app.taptap.analytics.AnalyticsManager

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var analyticsManager: AnalyticsManager
    @Inject lateinit var adManager: AdManager

    private lateinit var launchMusic: MediaPlayer
    private lateinit var soundPool: SoundPool
    private var bubbleSoundId = 0

    private val bubbleHandler = Handler(Looper.getMainLooper())
    private var bubbleRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.cartoonSceneryView).parent as View
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
        analyticsManager.logScreenView("Main Menu", "MainActivity")

        // Gather GDPR/UMP Consent & Initialize Ads
        adManager.gatherConsentAndInitialize(this) {
            // Load Banner Ad
            val bannerContainer = findViewById<FrameLayout>(R.id.bannerContainer)
            if (bannerContainer != null) {
                adManager.loadAdaptiveBanner(this, bannerContainer)
            }
        }

        // Initialize launch music
        launchMusic = MediaPlayer.create(this, R.raw.launch_music).apply {
            isLooping = true
            start()
        }

        val helpTextView: TextView = findViewById(R.id.helpTextView)
        helpTextView.setOnClickListener {
            playClickSound()
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }

        val startButton: Button = findViewById(R.id.startButton)
        val zipModeButton: Button = findViewById(R.id.zipModeButton)
        val boosterStartButton: Button = findViewById(R.id.boosterStartButton)
        val highscoreButton: Button = findViewById(R.id.highscoreButton)

        startButton.setOnClickListener {
            playClickSound()
            startGame("NORMAL", withBooster = false)
        }

        zipModeButton.setOnClickListener {
            playClickSound()
            startGame("ZIP", withBooster = false)
        }

        boosterStartButton.setOnClickListener {
            playClickSound()
            if (adManager.isRewardedAdLoaded()) {
                adManager.showRewardedAd(
                    activity = this,
                    onUserEarnedReward = {
                        startGame("NORMAL", withBooster = true)
                    },
                    onAdClosed = {}
                )
            } else {
                adManager.preloadRewardedAd(this)
                android.widget.Toast.makeText(this, "Reward ad is loading, please try again in a moment!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        highscoreButton.setOnClickListener {
            playClickSound()
            val intent = Intent(this, HighScoreActivity::class.java)
            startActivity(intent)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitDialog()
            }
        })

        startAmbientBubbles()
    }

    private fun showExitDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit Game")
        builder.setMessage("Are you sure you want to exit?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            finishAffinity()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.setCancelable(false)
        alert.show()
    }

    private fun startAmbientBubbles() {
        val container = findViewById<FrameLayout>(R.id.bubbleContainer) ?: return
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        bubbleRunnable = object : Runnable {
            override fun run() {
                spawnAmbientBubble(container, width, height)
                bubbleHandler.postDelayed(this, Random.nextLong(1200, 2800))
            }
        }
        bubbleHandler.post(bubbleRunnable!!)
    }

    private fun spawnAmbientBubble(container: FrameLayout, screenW: Int, screenH: Int) {
        val size = Random.nextInt(120, 260)
        val bubble = BubbleView(this).apply {
            type = BubbleView.BubbleType.entries.random()
            alpha = 0.85f
        }
        val params = FrameLayout.LayoutParams(size, size)
        params.leftMargin = Random.nextInt(0, (screenW - size).coerceAtLeast(1))
        params.topMargin = screenH + 100
        bubble.layoutParams = params

        container.addView(bubble)

        val endY = -300f
        val duration = Random.nextLong(6000, 11000)

        val animY = ObjectAnimator.ofFloat(bubble, "translationY", 0f, endY - screenH - 100)
        val animX = ObjectAnimator.ofFloat(bubble, "translationX", 0f, Random.nextInt(-100, 100).toFloat())

        bubble.setOnClickListener {
            bubble.setOnClickListener(null)
            playClickSound()
            animY.cancel()
            animX.cancel()
            bubble.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .alpha(0f)
                .setDuration(150)
                .withEndAction { container.removeView(bubble) }
                .start()
        }

        AnimatorSet().apply {
            playTogether(animY, animX)
            this.duration = duration
            interpolator = android.view.animation.LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    container.removeView(bubble)
                }
            })
            start()
        }
    }

    private fun setupSoundPool() {
        val attr = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()
        soundPool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attr).build()
        bubbleSoundId = soundPool.load(this, R.raw.bubble_pop, 1)
    }

    private fun playClickSound() {
        soundPool.play(bubbleSoundId, 1f, 1f, 1, 0, 1f)
    }

    private fun startGame(mode: String, withBooster: Boolean) {
        if (launchMusic.isPlaying) {
            launchMusic.pause()
        }
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("GAME_MODE", mode)
        intent.putExtra("START_BOOSTER", withBooster)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (::launchMusic.isInitialized && !launchMusic.isPlaying) {
            launchMusic.start()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::launchMusic.isInitialized && launchMusic.isPlaying) {
            launchMusic.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleRunnable?.let { bubbleHandler.removeCallbacks(it) }
        if (::launchMusic.isInitialized) {
            launchMusic.release()
        }
        soundPool.release()
    }
}
