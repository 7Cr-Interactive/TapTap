package com.example.taptap

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var launchMusic: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable immersive fullscreen mode
        enableImmersiveMode()

        // Start launch music
        launchMusic = MediaPlayer.create(this, R.raw.launch_music)
        launchMusic.isLooping = true
        launchMusic.start()

        val startButton: Button = findViewById(R.id.startButton)
        val highscoreButton: Button = findViewById(R.id.highscoreButton)

        startButton.setOnClickListener {
            launchMusic.stop()
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        highscoreButton.setOnClickListener {
            val intent = Intent(this, HighScoreActivity::class.java)
            intent.putExtra("SHOW_FOR_5_SEC", false)
            startActivity(intent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    private fun enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Older versions
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

    override fun onDestroy() {
        super.onDestroy()
        if (::launchMusic.isInitialized && launchMusic.isPlaying) {
            launchMusic.stop()
        }
        launchMusic.release()
    }
}
