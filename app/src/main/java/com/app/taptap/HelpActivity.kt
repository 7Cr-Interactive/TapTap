package com.app.taptap

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.app.taptap.ads.AdManager
import com.app.taptap.analytics.AnalyticsManager
import com.app.taptap.databinding.ActivityHelpBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HelpActivity : AppCompatActivity() {

    @Inject lateinit var analyticsManager: AnalyticsManager
    @Inject lateinit var adManager: AdManager

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        analyticsManager.logScreenView("Help & Support", "HelpActivity")

        // Load banner ad
        adManager.loadAdaptiveBanner(this, binding.bannerContainer)

        // Set help text content
        binding.helpText.text = """
            
            
❤️ SUPPORT INDEPENDENT DEVELOPMENT

TapTap is an offline reflex game created by 7Cr Interactive.

If you enjoy the game and would like to support future updates and independent game development, you can support us using the QR code above.

Every contribution — no matter how small — genuinely helps independent development continue ❤️

━━━━━━━━━━━━━━

🔹 WELCOME TO TAPTAP!

TapTap is a relaxing yet fast-paced reflex game designed to challenge your reaction speed, focus, and precision.

Stay calm, stay sharp, and keep tapping!

🎮 GAMEPLAY:
• Tap BUBBLES as quickly as possible.
• Avoid touching NAILS — they reduce your score and break combos.
• The game becomes faster as your score increases.
• Miss too many bubbles and the game ends.

🏆 SCORING:
• +1 point for every bubble tapped.
• Bonus combo praise appears for continuous successful taps.
• -2 points for tapping nails.
• Higher scores increase difficulty and spawn speed.

⏱️ TIMER & SURVIVAL:
• A countdown timer constantly runs during gameplay.
• Successfully tapping bubbles restores some time.
• As your score increases, time recovery becomes smaller.
• The game gets more intense the longer you survive.
• Missing too many bubbles in a row ends the game.

🔥 COMBOS:
• Consecutive bubble pops build combo streaks.
• Exciting praise messages appear for high combos.
• Maintain rhythm to maximize your score.

🌤️ ATMOSPHERE:
• Enjoy a calming sky environment with ambient animations.
• Watch tiny ants move across the world while birds occasionally fly by.
• Designed to feel peaceful while testing your reflexes.

📋 MODES:
• Default Mode – Endless reflex challenge.
• More game modes are planned in future updates.

💡 TIPS:
• Stay focused near the center of the screen.
• Don’t rush blindly — nails become more common later.
• Calm reactions score better than panic tapping.
• Rhythm matters more than random tapping.

📝 NOTE:
Some ambient animations use open-source Lottie assets created by their respective designers.

Thank you for playing TapTap ❤️
— 7Cr Interactive
""".trimIndent()

        binding.supportQr.setOnClickListener {
            analyticsManager.logSupportQrClicked()

            val upiUri = Uri.parse(
                "upi://pay?pa=kodabalprabhuraj@ybl&pn=7Cr Interactive&cu=INR"
            )

            val intent = Intent(Intent.ACTION_VIEW, upiUri)

            startActivity(
                Intent.createChooser(intent, "Support using")
            )
        }
        // Handle back navigation
        binding.backText.setOnClickListener {
            finish()
        }
    }
}
