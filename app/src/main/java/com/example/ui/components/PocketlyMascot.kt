package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.PocketlyTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class MascotMood {
    SLEEPY,
    PERK,
    PROUD,
    OOPS
}

/**
 * Original Pocketly sleepy coin-pouch mascot with a calm mint liquid shimmer inside its pouch.
 * Supports sleepy breathing + occasional blink, perk hop on unlock, proud nod, and oops shake.
 */
@Composable
fun PocketlyMascot(
    mood: MascotMood,
    modifier: Modifier = Modifier
) {
    val colors = PocketlyTheme.colors
    val reduceMotion = PocketlyTheme.reduceMotion

    val infinite = rememberInfiniteTransition(label = "mascot_idle")
    val breathScale by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = if (reduceMotion) 1.0f else 1.032f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_breath"
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = if (reduceMotion) 0f else (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot_liquid_phase"
    )

    // Random slow blink every 4-7 seconds
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(mood, reduceMotion) {
        if (reduceMotion || mood == MascotMood.OOPS) {
            isBlinking = false
            return@LaunchedEffect
        }
        while (true) {
            delay(Random.nextLong(4000L, 6800L))
            isBlinking = true
            delay(170L)
            isBlinking = false
        }
    }

    val hopOffset by animateFloatAsState(
        targetValue = when (mood) {
            MascotMood.PERK -> if (reduceMotion) 0f else -10f
            MascotMood.PROUD -> if (reduceMotion) 0f else -3f
            else -> 0f
        },
        animationSpec = spring(stiffness = 380f, dampingRatio = 0.55f),
        label = "mascot_hop"
    )

    val pouchBodyColor = if (colors.isDark) Color(0xFF282633) else Color(0xFFFFFFFF)
    val pouchStrokeColor = if (colors.isDark) Color(0xFF3E3B4E) else Color(0xFFE0DCEB)
    val cinchColor = Color(0xFFF5C16C)
    val cheekColor = Color(0xFFFF9A9A).copy(alpha = 0.55f)
    val inkColor = colors.ink
    val mintColor = colors.mint

    Canvas(
        modifier = modifier
            .graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
                translationY = hopOffset
            }
            .semantics {
                contentDescription = when (mood) {
                    MascotMood.SLEEPY -> "Pocketly coin pouch mascot resting"
                    MascotMood.PERK -> "Pocketly mascot happy and awake"
                    MascotMood.PROUD -> "Pocketly mascot proud of your budget"
                    MascotMood.OOPS -> "Pocketly mascot squinting after wrong PIN"
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Soft ground shadow
        drawOval(
            color = Color.Black.copy(alpha = if (colors.isDark) 0.25f else 0.08f),
            topLeft = Offset(w * 0.18f, h * 0.87f),
            size = Size(w * 0.64f, h * 0.10f)
        )

        // Pouch ears / gathered top ruffle (lifts slightly on PERK)
        val earLift = if (mood == MascotMood.PERK) -h * 0.04f else 0f
        val leftEar = Path().apply {
            moveTo(w * 0.36f, h * 0.27f)
            cubicTo(w * 0.24f, h * 0.10f + earLift, w * 0.42f, h * 0.07f + earLift, w * 0.48f, h * 0.25f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(w * 0.64f, h * 0.27f)
            cubicTo(w * 0.76f, h * 0.10f + earLift, w * 0.58f, h * 0.07f + earLift, w * 0.52f, h * 0.25f)
            close()
        }
        drawPath(leftEar, color = pouchBodyColor)
        drawPath(leftEar, color = pouchStrokeColor, style = Stroke(width = w * 0.025f))
        drawPath(rightEar, color = pouchBodyColor)
        drawPath(rightEar, color = pouchStrokeColor, style = Stroke(width = w * 0.025f))

        // Rounded plush coin-pouch body
        val bodyPath = Path().apply {
            moveTo(w * 0.33f, h * 0.27f)
            cubicTo(w * 0.12f, h * 0.38f, w * 0.10f, h * 0.74f, w * 0.29f, h * 0.85f)
            cubicTo(w * 0.42f, h * 0.91f, w * 0.58f, h * 0.91f, w * 0.71f, h * 0.85f)
            cubicTo(w * 0.90f, h * 0.74f, w * 0.88f, h * 0.38f, w * 0.67f, h * 0.27f)
            close()
        }
        drawPath(bodyPath, color = pouchBodyColor)

        // Signature mint liquid shimmering inside the lower half of the coin pouch
        clipPath(bodyPath) {
            val liquidTop = h * 0.65f
            val amp = h * 0.028f
            val wavePath = Path().apply {
                moveTo(0f, h)
                lineTo(0f, liquidTop)
                var x = 0f
                while (x <= w) {
                    val y = liquidTop + sin((x / w) * 2.2 * PI + wavePhase).toFloat() * amp
                    lineTo(x, y)
                    x += w / 24f
                }
                lineTo(w, h)
                close()
            }
            drawPath(
                path = wavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        mintColor.copy(alpha = 0.78f),
                        mintColor.copy(alpha = 0.96f)
                    ),
                    startY = liquidTop,
                    endY = h
                )
            )
            // Specular highlight bubble on the pouch
            drawOval(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(w * 0.23f, h * 0.39f),
                size = Size(w * 0.10f, h * 0.17f)
            )
        }
        drawPath(bodyPath, color = pouchStrokeColor, style = Stroke(width = w * 0.026f))

        // Drawstring cinch ribbon around pouch neck
        drawRoundRect(
            color = cinchColor,
            topLeft = Offset(w * 0.31f, h * 0.24f),
            size = Size(w * 0.38f, h * 0.068f),
            cornerRadius = CornerRadius(h * 0.034f, h * 0.034f)
        )
        // Little coin charm hanging from the cinch
        drawCircle(
            color = cinchColor,
            radius = w * 0.045f,
            center = Offset(w * 0.50f, h * 0.315f)
        )

        // Rosy cheeks
        drawOval(
            color = cheekColor,
            topLeft = Offset(w * 0.24f, h * 0.52f),
            size = Size(w * 0.11f, h * 0.06f)
        )
        drawOval(
            color = cheekColor,
            topLeft = Offset(w * 0.65f, h * 0.52f),
            size = Size(w * 0.11f, h * 0.06f)
        )

        // Expressive eyes
        val eyeY = h * 0.48f
        val leftEyeX = w * 0.38f
        val rightEyeX = w * 0.62f
        val stroke = Stroke(width = w * 0.034f, cap = StrokeCap.Round)

        when {
            mood == MascotMood.OOPS -> {
                // Squinted > < eyes
                val leftSquint = Path().apply {
                    moveTo(leftEyeX - w * 0.045f, eyeY - h * 0.03f)
                    lineTo(leftEyeX + w * 0.03f, eyeY)
                    lineTo(leftEyeX - w * 0.045f, eyeY + h * 0.03f)
                }
                val rightSquint = Path().apply {
                    moveTo(rightEyeX + w * 0.045f, eyeY - h * 0.03f)
                    lineTo(rightEyeX - w * 0.03f, eyeY)
                    lineTo(rightEyeX + w * 0.045f, eyeY + h * 0.03f)
                }
                drawPath(leftSquint, inkColor, style = stroke)
                drawPath(rightSquint, inkColor, style = stroke)
            }
            mood == MascotMood.SLEEPY || isBlinking -> {
                // Calm sleeping downward arcs
                drawArc(
                    color = inkColor,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(leftEyeX - w * 0.055f, eyeY - h * 0.025f),
                    size = Size(w * 0.11f, h * 0.06f),
                    style = stroke
                )
                drawArc(
                    color = inkColor,
                    startAngle = 15f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(rightEyeX - w * 0.055f, eyeY - h * 0.025f),
                    size = Size(w * 0.11f, h * 0.06f),
                    style = stroke
                )
            }
            mood == MascotMood.PROUD -> {
                // Happy smiling upward arcs
                drawArc(
                    color = inkColor,
                    startAngle = 195f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(leftEyeX - w * 0.055f, eyeY - h * 0.025f),
                    size = Size(w * 0.11f, h * 0.06f),
                    style = stroke
                )
                drawArc(
                    color = inkColor,
                    startAngle = 195f,
                    sweepAngle = 150f,
                    useCenter = false,
                    topLeft = Offset(rightEyeX - w * 0.055f, eyeY - h * 0.025f),
                    size = Size(w * 0.11f, h * 0.06f),
                    style = stroke
                )
            }
            else -> {
                // PERK: bright open eyes with sparkle
                drawCircle(color = inkColor, radius = w * 0.042f, center = Offset(leftEyeX, eyeY))
                drawCircle(color = inkColor, radius = w * 0.042f, center = Offset(rightEyeX, eyeY))
                drawCircle(color = Color.White, radius = w * 0.015f, center = Offset(leftEyeX - w * 0.014f, eyeY - h * 0.014f))
                drawCircle(color = Color.White, radius = w * 0.015f, center = Offset(rightEyeX - w * 0.014f, eyeY - h * 0.014f))
            }
        }

        // Tiny gentle smile
        val mouthWidth = w * 0.09f
        val mouthY = h * 0.545f
        if (mood == MascotMood.OOPS) {
            drawCircle(
                color = inkColor,
                radius = w * 0.022f,
                center = Offset(w * 0.50f, mouthY + h * 0.015f),
                style = Stroke(width = w * 0.025f)
            )
        } else {
            drawArc(
                color = inkColor,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(w * 0.50f - mouthWidth / 2f, mouthY - h * 0.01f),
                size = Size(mouthWidth, h * 0.05f),
                style = Stroke(width = w * 0.028f, cap = StrokeCap.Round)
            )
        }
    }
}
