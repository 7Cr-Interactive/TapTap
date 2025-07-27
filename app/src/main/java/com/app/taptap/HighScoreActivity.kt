package com.app.taptap

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class HighScoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_score)

        val scoreListLayout = findViewById<LinearLayout>(R.id.scoreList)

        // Get the top 10 scores
        val scores = HighScoreManager.getScores(this).take(10)

        // Add score items to layout
        scores.forEachIndexed { index, pair ->
            val (name, score) = pair
            val textView = TextView(this).apply {
                text = "${index + 1}. $name - $score"
                textSize = 20f
                gravity = Gravity.CENTER
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
            finish() // Return to MainActivity
        }

        // Reset Scores button
        val resetScoresButton = findViewById<Button>(R.id.resetScoresButton)
        resetScoresButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset Scores")
                .setMessage("Are you sure you want to delete all high scores?")
                .setPositiveButton("Yes") { _, _ ->
                    HighScoreManager.clearScores(this)
                    recreate() // Reload activity to reflect cleared scores
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
