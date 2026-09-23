package com.app.taptap.ui.cartoon

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.AbstractComposeView
import kotlin.math.*

class CartoonSceneryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    @Composable
    override fun Content() {
        CartoonSceneryEnvironment()
    }
}

@Composable
fun CartoonSceneryEnvironment(modifier: Modifier = Modifier.fillMaxSize()) {
    val infiniteTransition = rememberInfiniteTransition(label = "scenery_anim")

    // Sunbeams rotation
    val sunbeamRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(45000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunbeams"
    )

    // Cloud drift offsets (0f..1f)
    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud1"
    )

    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud2"
    )

    // Dandelion seeds drift offset (0f..1f)
    val dandelionOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dandelion_drift"
    )

    // Butterfly wing flap (0.2f..1.0f)
    val butterflyFlap by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "butterfly_flap"
    )

    // Butterfly drift path phase
    val butterflyPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "butterfly_path"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. VIBRANT 90S CARTOON SKY GRADIENT
        val skyBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0077FF), // Deep Sky Blue
                Color(0xFF00C3FF), // Vibrant Cyan
                Color(0xFF99E8FF)  // Horizon Soft Cyan
            ),
            startY = 0f,
            endY = h * 0.75f
        )
        drawRect(brush = skyBrush)

        // 2. SUN & ROTATING SUNBEAMS
        val sunCenter = Offset(w * 0.18f, h * 0.14f)
        val sunRadius = min(w, h) * 0.12f

        // Sunbeams
        rotate(sunbeamRotation, pivot = sunCenter) {
            val beamCount = 12
            val beamAngle = 360f / beamCount
            val beamLength = max(w, h) * 1.2f
            for (i in 0 until beamCount) {
                val path = Path().apply {
                    moveTo(sunCenter.x, sunCenter.y)
                    val a1 = Math.toRadians((i * beamAngle - 8).toDouble())
                    val a2 = Math.toRadians((i * beamAngle + 8).toDouble())
                    lineTo(
                        sunCenter.x + beamLength * cos(a1).toFloat(),
                        sunCenter.y + beamLength * sin(a1).toFloat()
                    )
                    lineTo(
                        sunCenter.x + beamLength * cos(a2).toFloat(),
                        sunCenter.y + beamLength * sin(a2).toFloat()
                    )
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0x22FFF380) // Soft golden sunbeam
                )
            }
        }

        // Sun Outer Glow
        drawCircle(
            color = Color(0x33FFE600),
            radius = sunRadius * 1.8f,
            center = sunCenter
        )
        drawCircle(
            color = Color(0x55FFC800),
            radius = sunRadius * 1.35f,
            center = sunCenter
        )
        // Solid Glowing Sun Core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFB0), Color(0xFFFFCC00)),
                center = sunCenter,
                radius = sunRadius
            ),
            radius = sunRadius,
            center = sunCenter
        )

        // 3. DRIFTING PARALLAX CLOUDS (Layer 1 - Distant)
        drawCartoonCloud(
            x = ((cloudOffset1 * (w + 300f)) - 150f) % (w + 300f) - 150f,
            y = h * 0.08f,
            scale = 0.8f,
            alpha = 0.75f
        )
        drawCartoonCloud(
            x = (((cloudOffset1 + 0.5f) * (w + 300f)) - 150f) % (w + 300f) - 150f,
            y = h * 0.18f,
            scale = 0.65f,
            alpha = 0.65f
        )

        // 4. DISTANT EMERALD/BLUE HILLS
        val distantHillPath = Path().apply {
            moveTo(0f, h * 0.72f)
            cubicTo(
                w * 0.25f, h * 0.62f,
                w * 0.45f, h * 0.76f,
                w * 0.70f, h * 0.64f
            )
            cubicTo(
                w * 0.85f, h * 0.58f,
                w * 0.95f, h * 0.68f,
                w, h * 0.65f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = distantHillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
                startY = h * 0.58f,
                endY = h
            )
        )

        // 5. MIDGROUND EMERALD HILLS
        val midHillPath = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(
                w * 0.20f, h * 0.70f,
                w * 0.50f, h * 0.82f,
                w * 0.75f, h * 0.72f
            )
            cubicTo(
                w * 0.88f, h * 0.68f,
                w * 0.96f, h * 0.74f,
                w, h * 0.72f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = midHillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF10B981), Color(0xFF047857)),
                startY = h * 0.68f,
                endY = h
            )
        )

        // 6. DRIFTING PARALLAX CLOUDS (Layer 2 - Midground)
        drawCartoonCloud(
            x = ((cloudOffset2 * (w + 400f)) - 200f) % (w + 400f) - 200f,
            y = h * 0.12f,
            scale = 1.1f,
            alpha = 0.95f
        )
        drawCartoonCloud(
            x = (((cloudOffset2 + 0.45f) * (w + 400f)) - 200f) % (w + 400f) - 200f,
            y = h * 0.24f,
            scale = 0.9f,
            alpha = 0.90f
        )

        // 7. FOREGROUND LIME GREEN HILL & CARTOON GRASS
        val foreHillPath = Path().apply {
            moveTo(0f, h * 0.82f)
            cubicTo(
                w * 0.30f, h * 0.75f,
                w * 0.65f, h * 0.86f,
                w, h * 0.80f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = foreHillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)),
                startY = h * 0.75f,
                endY = h
            )
        )

        // Grass tufts and cute cartoon flowers
        drawGrassTuftsAndFlowers(w, h)

        // 8. FLYING DANDELION SEEDS
        val seedCount = 10
        for (i in 0 until seedCount) {
            val progress = (dandelionOffset + (i.toFloat() / seedCount)) % 1.0f
            val sx = ((progress * (w + 200f)) - 100f)
            val sy = h * (0.85f - (progress * 0.65f)) + sin((progress * 4 * PI + i).toDouble()).toFloat() * 25f
            val seedScale = 0.65f + (i % 3) * 0.25f
            val rotationAngle = (sin((progress * 2 * PI + i).toDouble()).toFloat() * 20f) - 15f

            rotate(rotationAngle, pivot = Offset(sx, sy)) {
                drawDandelionSeed(Offset(sx, sy), scale = seedScale)
            }
        }

        // 9. FLUTTERING BUTTERFLIES
        val b1X = w * 0.35f + sin(butterflyPhase) * 60f
        val b1Y = h * 0.55f + cos(butterflyPhase * 1.5f) * 30f
        drawCartoonButterfly(Offset(b1X, b1Y), wingScale = butterflyFlap, color = Color(0xFFFF007F))

        val b2X = w * 0.75f + cos(butterflyPhase * 0.8f) * 50f
        val b2Y = h * 0.42f + sin(butterflyPhase * 1.2f) * 25f
        drawCartoonButterfly(Offset(b2X, b2Y), wingScale = butterflyFlap, color = Color(0xFFFF9900))
    }
}

private fun DrawScope.drawDandelionSeed(center: Offset, scale: Float) {
    val stemLength = 22f * scale
    val tuftRadius = 12f * scale

    // Seed stem
    drawLine(
        color = Color(0xCCFFFFFF),
        start = center,
        end = Offset(center.x - stemLength * 0.4f, center.y + stemLength),
        strokeWidth = 1.8f * scale,
        cap = StrokeCap.Round
    )

    // Seed core head
    drawCircle(
        color = Color(0xEEFFFFFF),
        radius = 2.5f * scale,
        center = center
    )

    // Fluffy Pappus Filaments (Starburst radial filaments)
    val filamentCount = 12
    val angleStep = 360f / filamentCount
    for (f in 0 until filamentCount) {
        val angle = Math.toRadians((f * angleStep).toDouble())
        val fx = center.x + tuftRadius * cos(angle).toFloat()
        val fy = center.y + tuftRadius * sin(angle).toFloat()
        drawLine(
            color = Color(0xDDFFFFFF),
            start = center,
            end = Offset(fx, fy),
            strokeWidth = 1.2f * scale,
            cap = StrokeCap.Round
        )
        // Soft white seed tip dot
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = 1.0f * scale,
            center = Offset(fx, fy)
        )
    }
}

private fun DrawScope.drawCartoonCloud(x: Float, y: Float, scale: Float, alpha: Float) {
    val cloudColor = Color.White.copy(alpha = alpha)
    val shadowColor = Color(0xFFD0F0FF).copy(alpha = alpha)

    val baseR = 35f * scale

    // Cloud shadow
    drawCircle(color = shadowColor, radius = baseR * 1.1f, center = Offset(x + baseR * 0.8f, y + baseR * 0.5f))
    drawCircle(color = shadowColor, radius = baseR * 1.4f, center = Offset(x + baseR * 1.8f, y + baseR * 0.3f))
    drawCircle(color = shadowColor, radius = baseR * 1.0f, center = Offset(x + baseR * 2.8f, y + baseR * 0.5f))

    // Cloud white fluffy bumps
    drawCircle(color = cloudColor, radius = baseR * 0.95f, center = Offset(x + baseR * 0.7f, y + baseR * 0.3f))
    drawCircle(color = cloudColor, radius = baseR * 1.35f, center = Offset(x + baseR * 1.7f, y))
    drawCircle(color = cloudColor, radius = baseR * 0.90f, center = Offset(x + baseR * 2.7f, y + baseR * 0.3f))
    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(x, y + baseR * 0.2f),
        size = Size(baseR * 3.4f, baseR * 0.8f),
        cornerRadius = CornerRadius(baseR * 0.4f, baseR * 0.4f)
    )
}

private fun DrawScope.drawGrassTuftsAndFlowers(w: Float, h: Float) {
    val grassColor = Color(0xFF15803D)
    val grassBladeCount = 28
    for (i in 0..grassBladeCount) {
        val gx = (i.toFloat() / grassBladeCount) * w
        val gy = h * (0.81f + sin(i * 0.7f) * 0.02f)
        val path = Path().apply {
            moveTo(gx - 6f, gy)
            quadraticTo(gx - 12f, gy - 25f, gx - 18f, gy - 32f)
            quadraticTo(gx - 2f, gy - 18f, gx, gy)
            quadraticTo(gx + 12f, gy - 28f, gx + 16f, gy - 36f)
            quadraticTo(gx + 4f, gy - 18f, gx + 6f, gy)
            close()
        }
        drawPath(path = path, color = grassColor)

        // Cute flower head on every 4th tuft
        if (i % 4 == 0) {
            val flowerCenter = Offset(gx + 16f, gy - 36f)
            val petalColor = when (i % 3) {
                0 -> Color(0xFFFF3366) // Pink
                1 -> Color(0xFFFFD700) // Yellow
                else -> Color(0xFF00E5FF) // Cyan
            }
            // Petals
            for (p in 0 until 5) {
                val angle = Math.toRadians((p * 72).toDouble())
                drawCircle(
                    color = petalColor,
                    radius = 6f,
                    center = Offset(
                        flowerCenter.x + 7f * cos(angle).toFloat(),
                        flowerCenter.y + 7f * sin(angle).toFloat()
                    )
                )
            }
            // Center
            drawCircle(color = Color.White, radius = 4f, center = flowerCenter)
        }
    }
}

private fun DrawScope.drawCartoonButterfly(center: Offset, wingScale: Float, color: Color) {
    val wingW = 16f * wingScale
    val wingH = 20f

    // Left wings
    drawOval(
        color = color,
        topLeft = Offset(center.x - wingW - 2f, center.y - wingH),
        size = Size(wingW, wingH)
    )
    drawOval(
        color = color.copy(alpha = 0.8f),
        topLeft = Offset(center.x - wingW * 0.8f - 2f, center.y),
        size = Size(wingW * 0.8f, wingH * 0.7f)
    )

    // Right wings
    drawOval(
        color = color,
        topLeft = Offset(center.x + 2f, center.y - wingH),
        size = Size(wingW, wingH)
    )
    drawOval(
        color = color.copy(alpha = 0.8f),
        topLeft = Offset(center.x + 2f, center.y),
        size = Size(wingW * 0.8f, wingH * 0.7f)
    )

    // Body
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(center.x - 2f, center.y - 12f),
        size = Size(4f, 22f),
        cornerRadius = CornerRadius(2f, 2f)
    )
}
