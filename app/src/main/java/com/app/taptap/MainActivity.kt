package com.app.taptap

import android.widget.TextView
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.app.bubblepop.GameActivity

class MainActivity : AppCompatActivity() {

    private lateinit var launchMusic: MediaPlayer
    private lateinit var soundPool: SoundPool
    private var bubbleSoundId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        enableImmersiveMode()
        setupSoundPool()

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
        val highscoreButton: Button = findViewById(R.id.highscoreButton)

        startButton.setOnClickListener {
            playClickSound()
            startGame("NORMAL")
        }

        zipModeButton.setOnClickListener {
            playClickSound()
            startGame("ZIP")
        }

        highscoreButton.setOnClickListener {
            playClickSound()
            val intent = Intent(this, HighScoreActivity::class.java)
            startActivity(intent)
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

    private fun startGame(mode: String) {
        if (launchMusic.isPlaying) {
            launchMusic.pause()
        }
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("GAME_MODE", mode)
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
        if (::launchMusic.isInitialized) {
            launchMusic.release()
        }
        soundPool.release()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    private fun enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Exit Game")
        builder.setMessage("Are you sure you want to exit?")

        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            finishAffinity() // Exit entire app
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.setCancelable(false)
        alert.show()
    }
}
