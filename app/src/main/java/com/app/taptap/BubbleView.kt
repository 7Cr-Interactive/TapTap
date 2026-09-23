package com.app.taptap

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator

class BubbleView(context: Context) : View(context) {
    enum class BubbleType {
        NORMAL, FREEZE, RAINBOW, SHIELD, BLAST
    }

    var type: BubbleType = BubbleType.NORMAL

    private var shrinkFactor = 1f
    
    fun setShrink(factor: Float) {
        shrinkFactor = factor.coerceIn(0.1f, 1f)
        invalidate()
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Bubble properties
    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    private var floatAnimator: ValueAnimator? = null
    private var phase = 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (tag != "particle") {
            // Overshoot bounce entrance animation
            scaleX = 0f
            scaleY = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(280)
                .setInterpolator(OvershootInterpolator(2.2f))
                .start()

            // Continuous organic floating wobble animation
            val randomOffset = (Math.random() * 1000).toLong()
            floatAnimator = ValueAnimator.ofFloat(0f, (Math.PI * 2).toFloat()).apply {
                duration = (2000 + (Math.random() * 600)).toLong()
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                startDelay = randomOffset
                addUpdateListener { anim ->
                    phase = anim.animatedValue as Float
                    // Gentle sinusoidal floating motion and organic wobble
                    translationY = Math.sin(phase.toDouble()).toFloat() * 10f
                    translationX = Math.cos((phase * 0.7f).toDouble()).toFloat() * 6f
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        floatAnimator?.cancel()
        floatAnimator = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDimensions(w, h)
    }

    private fun updateDimensions(w: Int, h: Int) {
        radius = (minOf(w, h) / 2f) * 0.82f * shrinkFactor
        centerX = w / 2f
        centerY = h / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateDimensions(width, height)

        val pulseFactor = (1f + Math.sin(phase.toDouble() * 2).toFloat() * 0.12f)

        // 0. Draw special type-specific glowing outer aura/rings
        when (type) {
            BubbleType.FREEZE -> {
                auraPaint.style = Paint.Style.STROKE
                auraPaint.strokeWidth = 8f * pulseFactor
                auraPaint.color = Color.parseColor("#8800E5FF")
                canvas.drawCircle(centerX, centerY, radius + 4f, auraPaint)

                auraPaint.strokeWidth = 3f
                auraPaint.color = Color.parseColor("#DDFFFFFF")
                canvas.drawCircle(centerX, centerY, radius + 8f * pulseFactor, auraPaint)
            }
            BubbleType.SHIELD -> {
                auraPaint.style = Paint.Style.STROKE
                auraPaint.strokeWidth = 10f * pulseFactor
                auraPaint.color = Color.parseColor("#AAFFD700")
                canvas.drawCircle(centerX, centerY, radius + 5f, auraPaint)

                auraPaint.strokeWidth = 4f
                auraPaint.color = Color.parseColor("#FFFFE066")
                canvas.drawCircle(centerX, centerY, radius + 9f * pulseFactor, auraPaint)
            }
            BubbleType.BLAST -> {
                auraPaint.style = Paint.Style.STROKE
                auraPaint.strokeWidth = 12f * pulseFactor
                auraPaint.color = Color.parseColor("#AAFF3D00")
                canvas.drawCircle(centerX, centerY, radius + 6f, auraPaint)

                auraPaint.strokeWidth = 4f
                auraPaint.color = Color.parseColor("#FFFF9100")
                canvas.drawCircle(centerX, centerY, radius + 10f * pulseFactor, auraPaint)
            }
            BubbleType.RAINBOW -> {
                auraPaint.style = Paint.Style.STROKE
                auraPaint.strokeWidth = 6f * pulseFactor
                auraPaint.color = Color.parseColor("#AAFFFFFF")
                canvas.drawCircle(centerX, centerY, radius + 5f * pulseFactor, auraPaint)
            }
            else -> {
                auraPaint.style = Paint.Style.STROKE
                auraPaint.strokeWidth = 3f
                auraPaint.color = Color.parseColor("#44FFFFFF")
                canvas.drawCircle(centerX, centerY, radius + 2f, auraPaint)
            }
        }

        // 1. Save Layout Layer for Blending
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 2. Edge Definition
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.parseColor("#88AADDFF")
        canvas.drawCircle(centerX, centerY, radius, paint)

        // 3. Soap Film Base
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

        // 4. 🌈 CHROMATIC RAINBOW SPECTRUM OVERLAY
        val rainbowColors = when(type) {
            BubbleType.FREEZE -> intArrayOf(
                Color.parseColor("#AA00E5FF"), // Ice Cyan
                Color.parseColor("#88FFFFFF"), // White
                Color.parseColor("#AA00B0FF")  // Deep Blue
            )
            BubbleType.SHIELD -> intArrayOf(
                Color.parseColor("#CCFFD700"), // Gold
                Color.parseColor("#AAFFCC00"), // Yellow
                Color.parseColor("#CCFFD700")  // Gold
            )
            BubbleType.BLAST -> intArrayOf(
                Color.parseColor("#CCFF3D00"), // Bright Red
                Color.parseColor("#AAFF9100"), // Orange
                Color.parseColor("#CCFF3D00")  // Bright Red
            )
            else -> intArrayOf(
                Color.parseColor("#33FF0000"), // Soft Red
                Color.parseColor("#22FFBB00"), // Soft Yellow
                Color.parseColor("#2200FF66"), // Soft Green
                Color.parseColor("#3300E1FF"), // Soft Cyan
                Color.parseColor("#330044FF"), // Soft Blue
                Color.parseColor("#44FF00CC")  // Soft Magenta
            )
        }

        // Dynamic angle based on floating phase for shimmering rainbow effect
        val cosP = Math.cos(phase.toDouble()).toFloat()
        val sinP = Math.sin(phase.toDouble()).toFloat()
        val x1 = centerX - radius * cosP
        val y1 = centerY - radius * sinP
        val x2 = centerX + radius * cosP
        val y2 = centerY + radius * sinP

        val spectrumShader = LinearGradient(
            x1, y1, x2, y2,
            rainbowColors,
            null,
            Shader.TileMode.CLAMP
        )

        paint.shader = spectrumShader
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        canvas.drawCircle(centerX, centerY, radius, paint)

        paint.shader = null
        paint.xfermode = null

        canvas.restoreToCount(saveCount)

        // 5. Realistic Highlights
        highlightPaint.color = Color.WHITE

        // Top-left main reflection
        highlightPaint.alpha = 200
        canvas.drawOval(
            centerX - radius * 0.6f, centerY - radius * 0.7f,
            centerX - radius * 0.2f, centerY - radius * 0.4f,
            highlightPaint
        )

        // Bottom-right subtle reflection
        highlightPaint.alpha = 100
        canvas.drawOval(
            centerX + radius * 0.3f, centerY + radius * 0.5f,
            centerX + radius * 0.5f, centerY + radius * 0.7f,
            highlightPaint
        )
    }
}
