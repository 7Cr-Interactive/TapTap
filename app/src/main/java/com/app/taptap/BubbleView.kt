package com.app.taptap

import android.content.Context
import android.graphics.*
import android.view.View

class BubbleView(context: Context) : View(context) {
    enum class BubbleType {
        NORMAL, FREEZE, RAINBOW, SHIELD, BLAST
    }

    var type: BubbleType = BubbleType.NORMAL

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Bubble properties
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = (minOf(w, h) / 2f) * 0.85f
        centerX = w / 2f
        centerY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Save Layout Layer for Blending
        // We isolate this rendering cycle so our rainbow spectrum clips perfectly inside the bubble skin
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 2. Original Edge Definition
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.parseColor("#88AADDFF")
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 3. Original "Soap Film" Base
        // Keeps your beautiful transparent center core and original dimensional tracking
        val soapGradient = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#2200E1FF"), Color.parseColor("#6655CCFF")),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        paint.shader = soapGradient
        canvas.drawCircle(centerX, centerY, radius, paint)
        paint.shader = null

        // 4. 🌈 THE CHROMATIC RAINBOW SPECTRUM OVERLAY
        // We project a subtle linear light spectrum diagonally across the bubble.
        // Using SRC_ATOP ensures it only applies to the existing transparent colors drawn in step 3.
        val rainbowColors = when(type) {
            BubbleType.FREEZE -> intArrayOf(
                Color.parseColor("#8800E5FF"), // Ice Cyan
                Color.parseColor("#66FFFFFF"), // White
                Color.parseColor("#8800B0FF")  // Deep Blue
            )
            BubbleType.SHIELD -> intArrayOf(
                Color.parseColor("#AAFFD700"), // Gold
                Color.parseColor("#88FFCC00"), // Yellow
                Color.parseColor("#AAFFD700")  // Gold
            )
            BubbleType.BLAST -> intArrayOf(
                Color.parseColor("#AAFF3D00"), // Bright Red
                Color.parseColor("#88FF9100"), // Orange
                Color.parseColor("#AAFF3D00")  // Bright Red
            )
            else -> intArrayOf(
                Color.parseColor("#33FF0000"), // Soft Red (20% Opacity)
                Color.parseColor("#22FFBB00"), // Soft Yellow (13% Opacity)
                Color.parseColor("#2200FF66"), // Soft Green (13% Opacity)
                Color.parseColor("#3300E1FF"), // Soft Cyan (20% Opacity)
                Color.parseColor("#330044FF"), // Soft Blue (20% Opacity)
                Color.parseColor("#44FF00CC")  // Soft Magenta (26% Opacity)
            )
        }

        // Diagonal coordinates from top-left to bottom-right mirror realistic refraction entry angles
        val spectrumShader = LinearGradient(
            centerX - radius, centerY - radius,
            centerX + radius, centerY + radius,
            rainbowColors,
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = spectrumShader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) // 🔑 Clips the rainbow inside the soap film!
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Clean up the paint pipeline configurations
        paint.shader = null
        paint.xfermode = null

        // Restore our layers to cleanly merge the background matrix back together
        canvas.restoreToCount(saveCount)

        // 5. Original Realistic Highlights (The "Sheen")
        // Rendered after the rainbow layer to ensure white reflections sit cleanly on top
        highlightPaint.color = Color.WHITE

        // Top-left main reflection
        highlightPaint.alpha = 180
        canvas.drawOval(
            centerX - radius * 0.6f, centerY - radius * 0.7f,
            centerX - radius * 0.2f, centerY - radius * 0.4f,
            highlightPaint
        )

        // Bottom-right subtle reflection
        highlightPaint.alpha = 80
        canvas.drawOval(
            centerX + radius * 0.3f, centerY + radius * 0.5f,
            centerX + radius * 0.5f, centerY + radius * 0.7f,
            highlightPaint
        )
    }
}