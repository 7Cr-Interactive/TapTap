package com.example.taptap

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HighScoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_highscore)

        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val scoreListLayout = findViewById<LinearLayout>(R.id.scoreList)

        // Get the top 10 scores
        val scores = HighScoreManager.getScores(this).take(10)

        // Add score items to layout
        scores.forEachIndexed { index, pair ->
            val (name, score) = pair
            val textView = TextView(this).apply {
                text = "${index + 1}. $name - $score"
                textSize = 20f
                gravity = Gravity.CENTER  // Center text horizontally
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 16, 8, 16)
            }
            scoreListLayout.addView(textView)
        }

        // Back button
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()  // Go back to previous screen
        }
    }
}
