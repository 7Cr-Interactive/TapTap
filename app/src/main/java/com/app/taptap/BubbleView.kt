package com.app.taptap

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.random.Random

class BubbleView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Bubble properties
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    // Animation offsets for "liquid" feel
    private var wobbleOffset = 0f
    private var rainbowPhase = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        radius = (minOf(w, h) / 2f) * 0.85f
        centerX = w / 2f
        centerY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Edge Definition (Visibility Fix)
        // We use a thick stroke with a slight blue/purple tint to ensure it pops against white
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.parseColor("#88AADDFF") // Semi-transparent light blue
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 2. The "Soap Film" (Thin-film Interference)
        // A radial gradient that is darker at the edges and transparent in the middle
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

        // 3. Realistic Highlights (The "Sheen")
        highlightPaint.color = Color.WHITE
        highlightPaint.alpha = 180
        // Top-left main reflection
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