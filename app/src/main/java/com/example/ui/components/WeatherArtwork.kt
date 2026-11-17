package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.WeatherCondition
import com.example.ui.theme.AtmosRainCyan
import com.example.ui.theme.AtmosStormViolet
import com.example.ui.theme.AtmosSunAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated custom-drawn weather hero illustration for the Home screen.
 */
@Composable
fun HeroWeatherIllustration(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp
) {
    val infinite = rememberInfiniteTransition(label = "heroArt")
    val bob by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f + bob

        when (condition) {
            WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR -> {
                if (isDay) drawSun(Offset(cx, cy), w * 0.25f, spin)
                else drawCrescentMoon(Offset(cx, cy), w * 0.25f)
            }
            WeatherCondition.PARTLY_CLOUDY -> {
                if (isDay) drawSun(Offset(cx + w * 0.14f, cy - h * 0.12f), w * 0.19f, spin)
                else drawCrescentMoon(Offset(cx + w * 0.14f, cy - h * 0.12f), w * 0.18f)
                drawCloud(Offset(cx - w * 0.04f, cy + h * 0.10f), w * 0.62f, Color.White)
            }
            WeatherCondition.OVERCAST -> {
                drawCloud(Offset(cx + w * 0.08f, cy - h * 0.05f), w * 0.56f, Color(0xFFD8E4F0))
                drawCloud(Offset(cx - w * 0.06f, cy + h * 0.08f), w * 0.66f, Color.White)
            }
            WeatherCondition.FOG -> {
                drawCloud(Offset(cx, cy - h * 0.04f), w * 0.60f, Color(0xFFE2E8F0))
                for (i in 0..2) {
                    val y = cy + h * (0.20f + i * 0.09f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f - i * 0.15f),
                        start = Offset(w * 0.20f, y),
                        end = Offset(w * 0.80f, y),
                        strokeWidth = 4.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> {
                drawCloud(Offset(cx, cy - h * 0.08f), w * 0.64f, Color(0xFFEAF2FA))
                val dropCount = if (condition == WeatherCondition.HEAVY_RAIN) 5 else 3
                for (i in 0 until dropCount) {
                    val dx = cx - w * 0.22f + i * (w * 0.44f / (dropCount - 1).coerceAtLeast(1))
                    drawLine(
                        color = AtmosRainCyan,
                        start = Offset(dx, cy + h * 0.16f),
                        end = Offset(dx - 5f, cy + h * 0.34f),
                        strokeWidth = 4.2f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherCondition.THUNDERSTORM -> {
                drawCloud(Offset(cx, cy - h * 0.10f), w * 0.66f, Color(0xFFCBD5E1))
                val bolt = Path().apply {
                    moveTo(cx + 2f, cy + h * 0.04f)
                    lineTo(cx - 10f, cy + h * 0.22f)
                    lineTo(cx + 2f, cy + h * 0.22f)
                    lineTo(cx - 6f, cy + h * 0.40f)
                    lineTo(cx + 14f, cy + h * 0.17f)
                    lineTo(cx + 2f, cy + h * 0.17f)
                    close()
                }
                drawPath(bolt, color = AtmosSunAmber)
            }
            WeatherCondition.SNOW -> {
                drawCloud(Offset(cx, cy - h * 0.08f), w * 0.62f, Color.White)
                for (i in 0..2) {
                    val sx = cx - w * 0.18f + i * w * 0.18f
                    drawCircle(Color(0xFFE0F2FE), radius = 4.5f, center = Offset(sx, cy + h * 0.24f))
                }
            }
            WeatherCondition.SQUALL -> {
                drawCloud(Offset(cx, cy - h * 0.08f), w * 0.58f, Color(0xFFE2E8F0))
                for (i in 0..1) {
                    val wy = cy + h * (0.18f + i * 0.12f)
                    drawLine(
                        color = Color(0xFF93C5FD),
                        start = Offset(w * 0.18f, wy),
                        end = Offset(w * 0.78f, wy),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSun(center: Offset, radius: Float, rotationDeg: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFDE047), AtmosSunAmber, Color(0xFFF59E0B)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    for (i in 0 until 8) {
        val angle = Math.toRadians((rotationDeg + i * 45.0))
        val r1 = radius * 1.25f
        val r2 = radius * 1.55f
        drawLine(
            color = Color(0xFFFDE047).copy(alpha = 0.85f),
            start = Offset(
                center.x + (cos(angle) * r1).toFloat(),
                center.y + (sin(angle) * r1).toFloat()
            ),
            end = Offset(
                center.x + (cos(angle) * r2).toFloat(),
                center.y + (sin(angle) * r2).toFloat()
            ),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawCrescentMoon(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF9C3), Color(0xFFFDE68A)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    // Soft glow ring
    drawCircle(
        color = Color(0x33FEF08A),
        radius = radius * 1.4f,
        center = center
    )
}

private fun DrawScope.drawCloud(center: Offset, width: Float, tint: Color) {
    val h = width * 0.46f
    drawRoundRect(
        color = tint.copy(alpha = 0.94f),
        topLeft = Offset(center.x - width / 2f, center.y - h * 0.25f),
        size = Size(width, h * 0.72f),
        cornerRadius = CornerRadius(h * 0.36f, h * 0.36f)
    )
    drawCircle(
        color = tint,
        radius = width * 0.23f,
        center = Offset(center.x - width * 0.14f, center.y - h * 0.26f)
    )
    drawCircle(
        color = tint,
        radius = width * 0.28f,
        center = Offset(center.x + width * 0.10f, center.y - h * 0.34f)
    )
}

@Composable
fun WeatherConditionBadgeIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val (icon, tint) = when (condition) {
        WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR ->
            if (isDay) Icons.Rounded.WbSunny to AtmosSunAmber
            else Icons.Rounded.NightsStay to Color(0xFFFDE68A)
        WeatherCondition.PARTLY_CLOUDY ->
            if (isDay) Icons.Rounded.WbCloudy to Color(0xFFFCD34D)
            else Icons.Rounded.Cloud to Color(0xFFCBD5E1)
        WeatherCondition.OVERCAST -> Icons.Rounded.Cloud to Color(0xFFE2E8F0)
        WeatherCondition.FOG -> Icons.Rounded.Grain to Color(0xFFCBD5E1)
        WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN ->
            Icons.Rounded.WaterDrop to AtmosRainCyan
        WeatherCondition.THUNDERSTORM -> Icons.Rounded.Thunderstorm to AtmosStormViolet
        WeatherCondition.SNOW -> Icons.Rounded.AcUnit to Color(0xFFBAE6FD)
        WeatherCondition.SQUALL -> Icons.Rounded.Air to Color(0xFF93C5FD)
    }

    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}
