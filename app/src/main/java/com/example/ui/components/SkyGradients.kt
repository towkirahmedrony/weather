package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.domain.model.WeatherCondition
import com.example.ui.theme.AtmosphericPalettes

/** Three-stop vertical gradient used by the saved-locations list. */
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
