package com.app.taptap

import android.widget.TextView
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.bubblepop.GameActivity

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var launchMusic: MediaPlayer
    private lateinit var soundPool: SoundPool
    private var bubbleSoundId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.backgroundImage).parent as View
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
