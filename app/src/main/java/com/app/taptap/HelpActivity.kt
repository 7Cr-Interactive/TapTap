package com.example.taptap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.taptap.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set help text content
        binding.helpText.text = """
🔹 Welcome to TapTap!

TapTap is a fun and fast-paced reflex game designed to test your reaction speed and accuracy. Here's everything you need to know:

🎮 GAMEPLAY:
• Tap on BUBBLES as soon as they appear on the screen.
• Avoid tapping on NAILS – they reduce your score!
• A timer counts down – make sure you tap a bubble before time runs out.
• Each successful tap resets the timer slightly.

🏆 SCORING:
• +1 point for every bubble you tap.
• -1 point for every nail you tap.
• The game ends if you miss 5 bubbles in a row.

⏱️ TIMER:
• Timer starts at a few seconds.
• It resets every time you score, more youu score shoter time it resets to,
 <but can never go below 1 second.>
• If timer runs out without a tap, it counts as a missed bubble.

📉 GAME OVER:
• When the game ends, you'll be prompted to enter your name.
• Your score is saved if it's among the Top 10 highscores.
• After showing the highscore list, you'll be returned to the main screen.

📋 MODES (Coming Soon!):
* currently only Default Mode
• Time Attack – Get the highest score in 60 seconds.
• Endless – Survive as long as you can.

💡 TIPS:
• Focus your attention – bubbles and nails spawn randomly!
• Don’t panic – hitting nails too often will cost you points.
• Missing 5 bubbles in a row will end your run – stay sharp!

Have fun and challenge your friends to beat your score!

- Developers - 7Cr Interactive
""".trimIndent()


        // Handle back navigation
        binding.backText.setOnClickListener {
            finish()
        }
    }
}
