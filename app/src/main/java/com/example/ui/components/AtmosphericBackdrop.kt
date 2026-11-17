package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.example.domain.model.WeatherCondition
import com.example.ui.theme.AtmosphericPalettes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class SkyGradientColors(
    val top: Color,
    val mid: Color,
    val bottom: Color
)

fun skyColorsFor(condition: WeatherCondition, isDay: Boolean): SkyGradientColors = when (condition) {
    WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR -> if (isDay) {
        SkyGradientColors(
            AtmosphericPalettes.ClearDayTop,
            AtmosphericPalettes.ClearDayMid,
            AtmosphericPalettes.ClearDayBottom
        )
    } else {
        SkyGradientColors(
            AtmosphericPalettes.ClearNightTop,
            AtmosphericPalettes.ClearNightMid,
            AtmosphericPalettes.ClearNightBottom
        )
    }
    WeatherCondition.PARTLY_CLOUDY -> if (isDay) {
        SkyGradientColors(
            AtmosphericPalettes.PartlyCloudyDayTop,
            AtmosphericPalettes.PartlyCloudyDayMid,
            AtmosphericPalettes.PartlyCloudyDayBottom
        )
    } else {
        SkyGradientColors(
            Color(0xFF0A1426),
            Color(0xFF172B47),
            Color(0xFF284366)
        )
    }
    WeatherCondition.OVERCAST -> if (isDay) {
        SkyGradientColors(
            AtmosphericPalettes.CloudyTop,
            AtmosphericPalettes.CloudyMid,
            AtmosphericPalettes.CloudyBottom
        )
    } else {
        SkyGradientColors(Color(0xFF141C27), Color(0xFF222E3D), Color(0xFF334154))
    }
    WeatherCondition.FOG -> SkyGradientColors(
        AtmosphericPalettes.FogTop,
        AtmosphericPalettes.FogMid,
        AtmosphericPalettes.FogBottom
    )
    WeatherCondition.DRIZZLE, WeatherCondition.RAIN -> SkyGradientColors(
        AtmosphericPalettes.RainTop,
        AtmosphericPalettes.RainMid,
        AtmosphericPalettes.RainBottom
    )
    WeatherCondition.HEAVY_RAIN, WeatherCondition.SQUALL -> SkyGradientColors(
        AtmosphericPalettes.HeavyRainTop,
        AtmosphericPalettes.HeavyRainMid,
        AtmosphericPalettes.HeavyRainBottom
    )
    WeatherCondition.THUNDERSTORM -> SkyGradientColors(
        AtmosphericPalettes.ThunderstormTop,
        AtmosphericPalettes.ThunderstormMid,
        AtmosphericPalettes.ThunderstormBottom
    )
    WeatherCondition.SNOW -> SkyGradientColors(
        AtmosphericPalettes.SnowTop,
        AtmosphericPalettes.SnowMid,
        AtmosphericPalettes.SnowBottom
    )
}

@Composable
fun AtmosphericBackdrop(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val target = skyColorsFor(condition, isDay)
    val top by animateColorAsState(target.top, tween(900), label = "skyTop")
    val mid by animateColorAsState(target.mid, tween(900), label = "skyMid")
    val bottom by animateColorAsState(target.bottom, tween(900), label = "skyBottom")

    val infinite = rememberInfiniteTransition(label = "atmosLoop")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "atmosPhase"
    )
    val fastPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainPhase"
    )

    // Precompute deterministic star/particle seeds to avoid allocation during draw
    val starSeeds = remember {
        List(42) { idx ->
            Triple(
                ((idx * 73 + 19) % 100) / 100f,
                ((idx * 41 + 7) % 55) / 100f,
                1.2f + (idx % 3) * 0.9f
            )
        }
    }
    val rainSeeds = remember {
        List(48) { idx ->
            Triple(
                ((idx * 53 + 11) % 100) / 100f,
                ((idx * 29 + 3) % 100) / 100f,
                0.7f + (idx % 4) * 0.25f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, mid, bottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Clear or Partly Cloudy Daytime Solar Halo
            if (isDay && (condition == WeatherCondition.CLEAR ||
                    condition == WeatherCondition.MOSTLY_CLEAR ||
                    condition == WeatherCondition.PARTLY_CLOUDY)
            ) {
                val pulse = 0.94f + 0.06f * sin(phase * 2f * PI.toFloat())
                val sunCenter = Offset(w * 0.82f, h * 0.14f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x66FDE68A),
                            Color(0x22FBBF24),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = w * 0.55f * pulse
                    ),
                    radius = w * 0.55f * pulse,
                    center = sunCenter
                )
            }

            // 2. Nighttime Twinkling Starfield
            if (!isDay && (condition == WeatherCondition.CLEAR ||
                    condition == WeatherCondition.MOSTLY_CLEAR ||
                    condition == WeatherCondition.PARTLY_CLOUDY)
            ) {
                starSeeds.forEachIndexed { idx, (sx, sy, radius) ->
                    val twinkle = 0.35f + 0.65f * ((sin((phase * 2f * PI + idx * 0.7).toDouble()).toFloat() + 1f) * 0.5f)
                    drawCircle(
                        color = Color.White.copy(alpha = twinkle * 0.75f),
                        radius = radius,
                        center = Offset(sx * w, sy * h)
                    )
                }
            }

            // 3. Drifting Atmospheric Clouds / Fog Bands
            if (condition == WeatherCondition.PARTLY_CLOUDY ||
                condition == WeatherCondition.OVERCAST ||
                condition == WeatherCondition.FOG
            ) {
                val cloudAlpha = when (condition) {
                    WeatherCondition.FOG -> 0.18f
                    WeatherCondition.OVERCAST -> 0.14f
                    else -> 0.10f
                }
                for (i in 0 until 4) {
                    val shift = ((phase + i * 0.25f) % 1f) * (w * 1.4f) - w * 0.2f
                    val cy = h * (0.12f + i * 0.13f)
                    val r = w * (0.34f + (i % 2) * 0.12f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = cloudAlpha),
                                Color.Transparent
                            ),
                            center = Offset(shift, cy),
                            radius = r
                        ),
                        radius = r,
                        center = Offset(shift, cy)
                    )
                }
            }

            // 4. Rain / Heavy Rain / Thunderstorm Streaks
            if (condition == WeatherCondition.DRIZZLE ||
                condition == WeatherCondition.RAIN ||
                condition == WeatherCondition.HEAVY_RAIN ||
                condition == WeatherCondition.THUNDERSTORM ||
                condition == WeatherCondition.SQUALL
            ) {
                val count = when (condition) {
                    WeatherCondition.DRIZZLE -> 22
                    WeatherCondition.RAIN -> 34
                    else -> 48
                }
                val streakLen = if (condition == WeatherCondition.DRIZZLE) 18f else 34f
                val alpha = if (condition == WeatherCondition.DRIZZLE) 0.20f else 0.28f

                for (i in 0 until count) {
                    val (rx, ry0, speed) = rainSeeds[i]
                    val yNorm = (ry0 + fastPhase * speed) % 1f
                    val x = rx * w
                    val y = yNorm * h
                    drawLine(
                        color = Color(0xFFA5F3FC).copy(alpha = alpha),
                        start = Offset(x, y),
                        end = Offset(x - 6f, y + streakLen),
                        strokeWidth = if (condition == WeatherCondition.HEAVY_RAIN) 2.2f else 1.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Subtle distant lightning luminescence during thunderstorms
                if (condition == WeatherCondition.THUNDERSTORM) {
                    val flashCycle = (phase * 3f) % 1f
                    val flashAlpha = if (flashCycle in 0.88f..0.94f) 0.14f else 0f
                    if (flashAlpha > 0f) {
                        drawRect(color = Color(0xFFE0E7FF).copy(alpha = flashAlpha))
                    }
                }
            }

            // 5. Gentle Snowflakes
            if (condition == WeatherCondition.SNOW) {
                rainSeeds.take(36).forEachIndexed { idx, (rx, ry0, speed) ->
                    val yNorm = (ry0 + phase * (speed * 1.4f)) % 1f
                    val sway = sin((phase * 2f * PI + idx).toDouble()).toFloat() * 14f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = 2.5f + (idx % 3) * 1.1f,
                        center = Offset(rx * w + sway, yNorm * h)
                    )
                }
            }
        }
    }
}
