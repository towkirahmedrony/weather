package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AirQualityData
import com.example.domain.model.CurrentWeather
import com.example.domain.model.DailyForecast
import com.example.domain.model.HourlyForecast
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherSnapshot
import com.example.domain.util.UnitFormatter
import com.example.ui.components.AqiScaleBar
import com.example.ui.components.AtmosCard
import com.example.ui.components.AtmosCardHeader
import com.example.ui.components.AtmosGlyph
import com.example.ui.components.AtmosSectionLabel
import com.example.ui.components.AtmosSmallTile
import com.example.ui.components.ChevronGlyph
import com.example.ui.components.GearGlyph
import com.example.ui.components.PinGlyph
import com.example.ui.components.WeatherLineIcon
import com.example.ui.theme.AtmosColors
import com.example.ui.theme.AtmosType
import kotlin.math.roundToInt

/**
 * Screen 1 - "Today". Everything critical fits in one 390 x 844 dp viewport:
 * top bar, hero, air quality summary, next five hours, the week as seven columns
 * and four quick metric tiles.
 */
@Composable
fun TodayContent(
    snapshot: WeatherSnapshot,
    prefs: UserPreferences,
    bannerError: String?,
    onOpenAir: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLocationSheet: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TodayTopBar(
            locationName = snapshot.location.name,
            onOpenLocationSheet = onOpenLocationSheet,
            onOpenSettings = onOpenSettings
        )

        TodayHero(
            snapshot = snapshot,
            prefs = prefs,
            bannerError = bannerError,
            onRetry = onRetry
        )

        snapshot.airQuality?.let { aqi ->
            Spacer(Modifier.height(12.dp))
            AirQualitySummaryCard(aqi = aqi, onOpenAir = onOpenAir)
        }

        Spacer(Modifier.height(12.dp))
        NextHoursCard(hourly = snapshot.hourly, prefs = prefs)

        Spacer(Modifier.height(12.dp))
        ThisWeekCard(daily = snapshot.daily, prefs = prefs)

        Spacer(Modifier.height(8.dp))
        TodayMetricTiles(current = snapshot.current, prefs = prefs)
    }
}

@Composable
private fun TodayTopBar(
    locationName: String,
    onOpenLocationSheet: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .height(44.dp)
                .clip(CircleShape)
                .background(AtmosColors.Card)
                .clickable(onClick = onOpenLocationSheet)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PinGlyph(size = 16.dp, tint = AtmosColors.Accent)
            Spacer(Modifier.width(8.dp))
            Text(
                text = locationName,
                style = AtmosType.Value.copy(fontWeight = FontWeight.SemiBold),
                color = AtmosColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AtmosColors.Card)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            GearGlyph(size = 20.dp, tint = AtmosColors.TextMuted)
        }
    }
}

@Composable
private fun TodayHero(
    snapshot: WeatherSnapshot,
    prefs: UserPreferences,
    bannerError: String?,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val current = snapshot.current
    val clock = UnitFormatter.formatLocalDayClock(snapshot.timezoneId, prefs.language)
    val updated = UnitFormatter.formatUpdatedAgo(snapshot.lastUpdatedEpochMillis, context)

    Column(modifier = Modifier.padding(top = 8.dp)) {
        val isStale = snapshot.isFromCache || bannerError != null
        if (isStale) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRetry),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listOf(
                        stringResource(R.string.status_offline_cache),
                        clock
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    style = AtmosType.Meta,
                    color = AtmosColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_retry),
                    style = AtmosType.Meta,
                    color = AtmosColors.Accent,
                    maxLines = 1
                )
            }
        } else {
            Text(
                text = listOf(clock, updated).filter { it.isNotBlank() }.joinToString(" · "),
                style = AtmosType.Meta,
                color = AtmosColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = UnitFormatter.formatTemp(current.tempC, prefs.temperatureUnit),
                style = AtmosType.HeroTemp,
                color = AtmosColors.TextPrimary,
                maxLines = 1
            )
            Spacer(Modifier.weight(1f))
            WeatherLineIcon(
                condition = current.condition,
                isDay = current.isDay,
                size = 76.dp,
                tint = AtmosColors.CloudStroke
            )
            Spacer(Modifier.width(2.dp))
        }

        Text(
            text = UnitFormatter.conditionLabel(current.condition, context),
            style = AtmosType.Condition,
            color = AtmosColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = stringResource(
                R.string.feels_like_fmt,
                UnitFormatter.formatTemp(current.feelsLikeC, prefs.temperatureUnit)
            ) + " · " + stringResource(
                R.string.high_low_short_fmt,
                UnitFormatter.formatTemp(current.todayHighC, prefs.temperatureUnit),
                UnitFormatter.formatTemp(current.todayLowC, prefs.temperatureUnit)
            ),
            style = AtmosType.Body,
            color = AtmosColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AirQualitySummaryCard(aqi: AirQualityData, onOpenAir: () -> Unit) {
    val context = LocalContext.current
    val category = aqi.category
    val color = AtmosColors.aqiColor(category)

    AtmosCard(onClick = onOpenAir) {
        AtmosCardHeader(
            label = stringResource(R.string.section_air_quality_aqi),
            trailing = { ChevronGlyph(size = 16.dp, tint = AtmosColors.TextMuted) }
        )

        Spacer(Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = aqi.usAqi.toString(),
                style = AtmosType.AqiNumberSmall,
                color = color,
                maxLines = 1
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = UnitFormatter.aqiCategoryLabel(category, context),
                style = AtmosType.CategoryLarge,
                color = AtmosColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(8.dp))

        AqiScaleBar(usAqi = aqi.usAqi)
    }
}

@Composable
private fun NextHoursCard(hourly: List<HourlyForecast>, prefs: UserPreferences) {
    val now = remember(hourly.firstOrNull()?.epochMillis) { System.currentTimeMillis() }
    val hours = selectNextHours(hourly, now, 5)

    AtmosCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        AtmosSectionLabel(text = stringResource(R.string.section_next_hours))

        Spacer(Modifier.height(9.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            hours.forEachIndexed { index, hour ->
                val isNow = index == 0
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isNow) stringResource(R.string.hourly_now)
                        else UnitFormatter.formatHourLabel(hour.isoTime, prefs.language),
                        style = AtmosType.Meta,
                        color = if (isNow) AtmosColors.Accent else AtmosColors.TextMuted,
                        fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(5.dp))
                    WeatherLineIcon(
                        condition = hour.condition,
                        isDay = hour.isDay,
                        size = 24.dp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = UnitFormatter.formatTemp(hour.tempC, prefs.temperatureUnit),
                        style = AtmosType.Value,
                        color = AtmosColors.TextPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ThisWeekCard(daily: List<DailyForecast>, prefs: UserPreferences) {
    val context = LocalContext.current
    val days = daily.take(7)

    AtmosCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)) {
        Box(modifier = Modifier.padding(horizontal = 2.dp)) {
            AtmosSectionLabel(text = stringResource(R.string.section_this_week))
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEachIndexed { index, day ->
                val isToday = index == 0
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = UnitFormatter.formatDayName(day.isoDate, index, context, prefs.language),
                        style = AtmosType.Meta,
                        color = if (isToday) AtmosColors.Accent else AtmosColors.TextMuted,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    WeatherLineIcon(
                        condition = day.condition,
                        isDay = true,
                        size = 22.dp
                    )
                    Spacer(Modifier.height(2.dp))
                    // Reserve the line even when there is no meaningful chance of rain,
                    // so the high/low rows stay aligned across all seven columns.
                    Box(
                        modifier = Modifier.height(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day.precipProbMaxPercent >= 20) {
                            Text(
                                text = "${day.precipProbMaxPercent}%",
                                style = AtmosType.Caption,
                                color = AtmosColors.Rain,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = UnitFormatter.formatTemp(day.maxTempC, prefs.temperatureUnit),
                        style = AtmosType.Value.copy(fontSize = 15.sp, lineHeight = 20.sp),
                        color = AtmosColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = UnitFormatter.formatTemp(day.minTempC, prefs.temperatureUnit),
                        style = AtmosType.Meta,
                        color = AtmosColors.TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayMetricTiles(current: CurrentWeather, prefs: UserPreferences) {
    val context = LocalContext.current
    val (windValue, windUnit) = UnitFormatter.windValueAndUnit(
        current.windSpeedKmh,
        prefs.windSpeedUnit
    )
    val (rainValue, rainUnit) = UnitFormatter.precipValueAndUnit(
        current.precipMm,
        prefs.precipitationUnit
    )

    val uvLabel = when {
        current.uvIndex < 3 -> stringResource(R.string.uv_low)
        current.uvIndex < 6 -> stringResource(R.string.uv_moderate)
        current.uvIndex < 8 -> stringResource(R.string.uv_high)
        current.uvIndex < 11 -> stringResource(R.string.uv_very_high)
        else -> stringResource(R.string.uv_extreme)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AtmosSmallTile(
            glyph = AtmosGlyph.WIND,
            value = windValue,
            unit = windUnit,
            label = stringResource(R.string.detail_wind),
            modifier = Modifier.weight(1f)
        )
        AtmosSmallTile(
            glyph = AtmosGlyph.HUMIDITY,
            value = "${current.humidityPercent}%",
            unit = null,
            label = stringResource(R.string.detail_humidity),
            modifier = Modifier.weight(1f)
        )
        AtmosSmallTile(
            glyph = AtmosGlyph.UV,
            value = current.uvIndex.roundToInt().toString(),
            unit = uvLabel,
            label = stringResource(R.string.detail_uv),
            modifier = Modifier.weight(1f)
        )
        AtmosSmallTile(
            glyph = AtmosGlyph.RAIN,
            value = rainValue,
            unit = rainUnit,
            label = stringResource(R.string.detail_rain),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * The [count] forecast hours starting with the hour that contains [nowMillis].
 * Shared by the "Next hours" strip and the compact rain chart on Details.
 */
internal fun selectNextHours(
    hourly: List<HourlyForecast>,
    nowMillis: Long,
    count: Int
): List<HourlyForecast> {
    if (hourly.isEmpty()) return emptyList()
    val start = hourly.indexOfFirst { it.epochMillis + 3_600_000L > nowMillis }
        .let { if (it < 0) 0 else it }
    return hourly.drop(start).take(count).ifEmpty { hourly.take(count) }
}
