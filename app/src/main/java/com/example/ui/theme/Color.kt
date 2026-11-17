package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.domain.model.AqiCategory

// Core Brand & M3 Neutrals
val AtmosSkyBlue = Color(0xFF38BDF8)
val AtmosDeepNavy = Color(0xFF0B1628)
val AtmosSlateSurface = Color(0xFF132238)
val AtmosLightSurface = Color(0xFFF3F7FC)
val AtmosLightCard = Color(0xFFFFFFFF)
val AtmosInkDark = Color(0xFF0E1726)
val AtmosInkMutedLight = Color(0xFF52647C)
val AtmosInkMutedDark = Color(0xFFB3C5DE)

// Semantic Weather Status Tokens
val AtmosSunAmber = Color(0xFFFBBF24)
val AtmosRainCyan = Color(0xFF22D3EE)
val AtmosStormViolet = Color(0xFFA78BFA)
val AtmosAlertCoral = Color(0xFFFB7185)
val AtmosGoodEmerald = Color(0xFF34D399)
val AtmosWarnOrange = Color(0xFFFB923C)

/**
 * Redesign tokens. These come straight from the three reference exports in design_refs/
 * (Today@2x, Air@2x, Details@2x) and are the single source of truth for the
 * Today / Air / Details screens.
 */
object AtmosColors {
    val Background = Color(0xFF0E1418)
    val Card = Color(0xFF172026)

    // Nav bar top border & in-card dividers
    val NavBorder = Color(0xFF1F2A31)
    val Divider = Color(0xFF222F37)

    val TextPrimary = Color(0xFFEAF0F3)
    val TextMuted = Color(0xFF93A4AE)

    // Selected tab, "Now" label, location pin
    val Accent = Color(0xFF5CC8D7)
    // Rain / precipitation probability
    val Rain = Color(0xFF7FB2FF)
    // Outline tint for weather line icons
    val CloudStroke = Color(0xFFC5D2D9)
    // "All normal" status dots
    val StatusGreen = Color(0xFF6FD6A0)

    // AQI category text color (matches the Very Unhealthy headline in Air@2x)
    val AqiVeryUnhealthyText = Color(0xFFBC9CF2)

    // AQI scale segments, in order
    val AqiGood = Color(0xFF4CAF7A)
    val AqiModerate = Color(0xFFE3C94F)
    val AqiSensitive = Color(0xFFF29B4B)
    val AqiUnhealthy = Color(0xFFE5594F)
    val AqiVeryUnhealthy = Color(0xFFA97CE8)
    val AqiHazardous = Color(0xFF7A2F4B)

    // Warm dot used for the sun on the daylight arc
    val SunDot = Color(0xFFF3C46A)

    val AqiScaleColors = listOf(
        AqiGood,
        AqiModerate,
        AqiSensitive,
        AqiUnhealthy,
        AqiVeryUnhealthy,
        AqiHazardous
    )

    /**
     * Segment flex weights of the scale bar. Together they add up to 350, which is
     * also the AQI range the marker is normalised against (aqi / 350).
     */
    val AqiScaleWeights = listOf(50f, 50f, 50f, 50f, 100f, 50f)
    const val AQI_SCALE_MAX = 350f

    fun aqiColor(category: AqiCategory): Color = when (category) {
        AqiCategory.GOOD -> AqiGood
        AqiCategory.MODERATE -> AqiModerate
        AqiCategory.SENSITIVE -> AqiSensitive
        AqiCategory.UNHEALTHY -> AqiUnhealthy
        AqiCategory.VERY_UNHEALTHY -> AqiVeryUnhealthyText
        AqiCategory.HAZARDOUS -> AqiHazardous
    }
}

// Atmospheric Sky Palettes (Top, Mid, Bottom)
object AtmosphericPalettes {
    val ClearDayTop = Color(0xFF1D6FA5)
    val ClearDayMid = Color(0xFF3D9BE9)
    val ClearDayBottom = Color(0xFF7EC8F8)

    val ClearNightTop = Color(0xFF060B18)
    val ClearNightMid = Color(0xFF0F1D3A)
    val ClearNightBottom = Color(0xFF1A2F5A)

    val PartlyCloudyDayTop = Color(0xFF2C6693)
    val PartlyCloudyDayMid = Color(0xFF518BB5)
    val PartlyCloudyDayBottom = Color(0xFF8DB5CE)

    val CloudyTop = Color(0xFF3A4A5E)
    val CloudyMid = Color(0xFF53667D)
    val CloudyBottom = Color(0xFF72869E)

    val RainTop = Color(0xFF192A3E)
    val RainMid = Color(0xFF28415C)
    val RainBottom = Color(0xFF3B5B7A)

    val HeavyRainTop = Color(0xFF101C2B)
    val HeavyRainMid = Color(0xFF1D3148)
    val HeavyRainBottom = Color(0xFF2C4562)

    val ThunderstormTop = Color(0xFF0E1124)
    val ThunderstormMid = Color(0xFF1D2140)
    val ThunderstormBottom = Color(0xFF323159)

    val FogTop = Color(0xFF475766)
    val FogMid = Color(0xFF677887)
    val FogBottom = Color(0xFF8C9BA8)

    val SnowTop = Color(0xFF3E6182)
    val SnowMid = Color(0xFF678EB0)
    val SnowBottom = Color(0xFFA3C1D9)
}
