// MainActivity.kt
package com.example.taptap

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var launchMusic: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start launch screen music
        launchMusic = MediaPlayer.create(this, R.raw.launch_music)
        launchMusic.isLooping = true
        launchMusic.start()

        val startButton: Button = findViewById(R.id.start_button)
        startButton.setOnClickListener {
            launchMusic.stop() // Stop launch music
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::launchMusic.isInitialized && launchMusic.isPlaying) {
            launchMusic.release()
        }
    }
}
