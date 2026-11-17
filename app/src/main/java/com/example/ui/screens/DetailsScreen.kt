package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AdvisorySeverity
import com.example.domain.model.AppLanguage
import com.example.domain.model.BangladeshAdvisoryReport
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherSnapshot
import com.example.domain.util.SunEvent
import com.example.domain.util.SunPositionCalculator
import com.example.domain.util.UnitFormatter
import com.example.ui.components.AtmosCard
import com.example.ui.components.AtmosCardHeader
import com.example.ui.components.AtmosDetailTile
import com.example.ui.components.AtmosDivider
import com.example.ui.components.AtmosScreenHeader
import com.example.ui.components.MoonPhaseGlyph
import com.example.ui.components.StatusDot
import com.example.ui.components.SunArc
import com.example.ui.theme.AtmosColors
import com.example.ui.theme.AtmosType
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Screen 3 - "Details": sun & moon, extra atmospheric conditions, a compact rain
 * outlook (kept from the previous design) and the Bangladesh insights card.
 */
@Composable
fun DetailsContent(
    snapshot: WeatherSnapshot,
    prefs: UserPreferences,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBangla = prefs.language == AppLanguage.BANGLA
    val current = snapshot.current
    val astronomy = snapshot.astronomy
    val advisory = snapshot.bangladeshAdvisory

    val sunPosition = remember(astronomy.sunriseIso, astronomy.sunsetIso) {
        SunPositionCalculator.compute(
            nowMillis = System.currentTimeMillis(),
            sunriseIso = astronomy.sunriseIso,
            sunsetIso = astronomy.sunsetIso,
            timezoneId = snapshot.timezoneId
        )
    }

    val sunStatus = when {
        sunPosition.isDaytime -> stringResource(
            R.string.sun_daylight_left_fmt,
            durationLabel(sunPosition.millisFromEvent)
        )
        sunPosition.event == SunEvent.SUNRISE -> stringResource(
            R.string.sunrise_in_fmt,
            durationLabel(sunPosition.millisFromEvent)
        )
        else -> stringResource(
            R.string.sun_set_ago_fmt,
            durationLabel(sunPosition.millisFromEvent)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        AtmosScreenHeader(
            title = stringResource(R.string.title_details),
            locationName = snapshot.location.name,
            timeText = UnitFormatter.formatLocalClock(snapshot.timezoneId, prefs.language),
            onBack = onBack
        )

        Spacer(Modifier.height(20.dp))

        AtmosCard {
            AtmosCardHeader(
                label = stringResource(R.string.section_sun_moon),
                trailing = {
                    Text(
                        text = sunStatus,
                        style = AtmosType.Meta,
                        color = AtmosColors.TextMuted,
                        maxLines = 1
                    )
                }
            )

            Spacer(Modifier.height(8.dp))

            SunArc(
                progress = sunPosition.progress,
                nightDotAtStart = sunPosition.beforeSunrise
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.sunrise),
                        style = AtmosType.Meta,
                        color = AtmosColors.TextMuted,
                        maxLines = 1
                    )
                    Text(
                        text = UnitFormatter.formatTime12h(astronomy.sunriseIso),
                        style = AtmosType.Value.copy(fontSize = 15.sp),
                        color = AtmosColors.TextPrimary,
                        maxLines = 1
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.daylight_duration),
                        style = AtmosType.Meta,
                        color = AtmosColors.TextMuted,
                        maxLines = 1
                    )
                    Text(
                        text = UnitFormatter.formatDaylightDuration(
                            astronomy.daylightDurationSeconds,
                            isBangla
                        ),
                        style = AtmosType.Value.copy(fontSize = 15.sp),
                        color = AtmosColors.TextPrimary,
                        maxLines = 1
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.sunset),
                        style = AtmosType.Meta,
                        color = AtmosColors.TextMuted,
                        maxLines = 1
                    )
                    Text(
                        text = UnitFormatter.formatTime12h(astronomy.sunsetIso),
                        style = AtmosType.Value.copy(fontSize = 15.sp),
                        color = AtmosColors.TextPrimary,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            AtmosDivider()
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                MoonPhaseGlyph(
                    phase = astronomy.moonPhaseType,
                    illuminationPercent = astronomy.moonIlluminationPercent
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = UnitFormatter.moonPhaseLabel(astronomy.moonPhaseType, context),
                        style = AtmosType.Value.copy(fontSize = 15.sp),
                        color = AtmosColors.TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(
                            R.string.illumination_fmt,
                            astronomy.moonIlluminationPercent
                        ),
                        style = AtmosType.Meta,
                        color = AtmosColors.TextMuted,
                        maxLines = 1
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stringResource(R.string.moonrise)} ${
                            UnitFormatter.formatTime12h(astronomy.moonriseTime)
                        }",
                        style = AtmosType.Caption,
                        color = AtmosColors.TextMuted,
                        maxLines = 1
                    )
                    Text(
                        text = "${stringResource(R.string.moonset)} ${
                            UnitFormatter.formatTime12h(astronomy.moonsetTime)
                        }",
                        style = AtmosType.Caption,
                        color = AtmosColors.TextMuted,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AtmosDetailTile(
                label = stringResource(R.string.detail_pressure),
                value = current.pressureHpa.roundToInt().toString(),
                unit = stringResource(R.string.unit_hpa),
                caption = UnitFormatter.pressureTrendLabel(current.pressureTrend, context),
                captionColor = AtmosColors.Accent,
                modifier = Modifier.weight(1f)
            )
            AtmosDetailTile(
                label = stringResource(R.string.detail_visibility),
                value = "%.1f".format(Locale.US, current.visibilityKm),
                unit = stringResource(R.string.unit_km),
                caption = visibilityWord(current.visibilityKm),
                modifier = Modifier.weight(1f)
            )
            AtmosDetailTile(
                label = stringResource(R.string.detail_cloud_cover),
                value = current.cloudCoverPercent.toString(),
                unit = "%",
                caption = cloudWord(current.cloudCoverPercent),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AtmosDetailTile(
                label = stringResource(R.string.detail_dew_point),
                value = UnitFormatter.formatTemp(current.dewPointC, prefs.temperatureUnit),
                unit = null,
                caption = dewPointWord(current.dewPointC),
                modifier = Modifier.weight(1f)
            )

            val (gustValue, gustUnit) = UnitFormatter.windValueAndUnit(
                current.windGustsKmh,
                prefs.windSpeedUnit
            )
            AtmosDetailTile(
                label = stringResource(R.string.detail_gusts),
                value = gustValue,
                unit = gustUnit,
                caption = stringResource(
                    R.string.gusts_from_fmt,
                    UnitFormatter.compassDirection(current.windDirectionDeg, isBangla)
                ),
                modifier = Modifier.weight(1f)
            )

            AtmosDetailTile(
                label = stringResource(R.string.detail_feels_like),
                value = UnitFormatter.formatTemp(current.feelsLikeC, prefs.temperatureUnit),
                unit = null,
                caption = stringResource(
                    R.string.actual_temp_fmt,
                    UnitFormatter.formatTemp(current.tempC, prefs.temperatureUnit)
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        RainOutlookCard(snapshot = snapshot, prefs = prefs)

        Spacer(Modifier.height(12.dp))

        BangladeshInsightsCard(report = advisory, prefs = prefs)

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.bd_source_short),
            style = AtmosType.Disclaimer,
            color = AtmosColors.TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun durationLabel(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(1L)
    return if (totalMinutes >= 60L) {
        stringResource(R.string.duration_hours_fmt, (totalMinutes / 60L).toInt())
    } else {
        stringResource(R.string.duration_minutes_fmt, totalMinutes.toInt())
    }
}

@Composable
private fun visibilityWord(km: Double): String = when {
    km >= 10.0 -> stringResource(R.string.visibility_w_clear)
    km >= 4.0 -> stringResource(R.string.visibility_w_good)
    km >= 1.0 -> stringResource(R.string.visibility_w_hazy)
    else -> stringResource(R.string.visibility_w_fog)
}

@Composable
private fun cloudWord(percent: Int): String = when {
    percent <= 20 -> stringResource(R.string.visibility_w_clear)
    percent <= 55 -> stringResource(R.string.cloud_w_partly)
    percent <= 85 -> stringResource(R.string.cloud_w_mostly)
    else -> stringResource(R.string.cond_overcast)
}

@Composable
private fun dewPointWord(dewC: Double): String = when {
    dewC < 10.0 -> stringResource(R.string.dew_w_dry)
    dewC < 16.0 -> stringResource(R.string.dew_w_pleasant)
    dewC < 20.0 -> stringResource(R.string.dew_w_comfortable)
    dewC < 24.0 -> stringResource(R.string.dew_w_humid)
    else -> stringResource(R.string.dew_w_sticky)
}

@Composable
private fun RainOutlookCard(snapshot: WeatherSnapshot, prefs: UserPreferences) {
    val now = remember(snapshot.lastUpdatedEpochMillis) { System.currentTimeMillis() }
    val hours = selectNextHours(snapshot.hourly, now, 12)
    val peakMm = hours.maxOfOrNull { it.precipMm } ?: 0.0
    val peakProb = hours.maxOfOrNull { it.precipProbPercent } ?: 0

    AtmosCard {
        AtmosCardHeader(
            label = stringResource(R.string.section_rain),
            trailing = {
                Text(
                    text = stringResource(R.string.rain_next_12h),
                    style = AtmosType.Caption,
                    color = AtmosColors.TextMuted,
                    maxLines = 1
                )
            }
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            hours.forEach { hour ->
                val fraction = if (peakMm > 0.0) {
                    (hour.precipMm / peakMm).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height((44.dp * fraction).coerceAtLeast(3.dp))
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (fraction > 0f) AtmosColors.Rain else AtmosColors.Divider)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.rain_expected_24h)}: ${
                    UnitFormatter.formatPrecip(
                        snapshot.bangladeshAdvisory.next24hRainTotalMm,
                        prefs.precipitationUnit
                    )
                }",
                style = AtmosType.Caption,
                color = AtmosColors.TextMuted,
                maxLines = 1
            )
            Text(
                text = "${stringResource(R.string.rain_peak_prob)}: $peakProb%",
                style = AtmosType.Caption,
                color = AtmosColors.TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BangladeshInsightsCard(report: BangladeshAdvisoryReport, prefs: UserPreferences) {
    val severities = listOf(
        report.heatSeverity,
        report.humiditySeverity,
        report.stormSeverity,
        report.heavyRainSeverity,
        report.coastalWindSeverity
    )
    val worst = severities.maxByOrNull { it.ordinal } ?: AdvisorySeverity.NORMAL
    val statusColor = severityColor(worst)
    val statusLabel = when (worst) {
        AdvisorySeverity.NORMAL -> stringResource(R.string.bd_status_all_normal)
        AdvisorySeverity.ADVISORY -> stringResource(R.string.bd_status_watch)
        AdvisorySeverity.WARNING -> stringResource(R.string.bd_status_elevated)
        AdvisorySeverity.SEVERE -> stringResource(R.string.bd_status_severe)
    }

    AtmosCard {
        AtmosCardHeader(
            label = stringResource(R.string.section_bd_insights),
            trailing = {
                Text(
                    text = statusLabel,
                    style = AtmosType.Meta,
                    color = statusColor,
                    maxLines = 1
                )
            }
        )

        Spacer(Modifier.height(6.dp))

        InsightRow(
            color = severityColor(report.heatSeverity),
            name = stringResource(R.string.bd_heat_index_short),
            value = UnitFormatter.formatTemp(report.heatIndexC, prefs.temperatureUnit, true)
        )
        InsightDivider()
        InsightRow(
            color = severityColor(report.humiditySeverity),
            name = stringResource(R.string.bd_humidity_short),
            value = humidityWord(report.humiditySeverity)
        )
        InsightDivider()
        InsightRow(
            color = severityColor(report.stormSeverity),
            name = stringResource(R.string.bd_storm_short),
            value = "${report.maxCapeNext24h.roundToInt()} J/kg"
        )
        InsightDivider()
        InsightRow(
            color = severityColor(report.heavyRainSeverity),
            name = stringResource(R.string.bd_rain_short),
            value = UnitFormatter.formatPrecip(
                report.next24hRainTotalMm,
                prefs.precipitationUnit
            )
        )
        InsightDivider()
        InsightRow(
            color = severityColor(report.coastalWindSeverity),
            name = stringResource(R.string.bd_wind_short),
            value = UnitFormatter.formatWind(report.peakWindGustKmh, prefs.windSpeedUnit)
        )
    }
}

@Composable
private fun InsightRow(color: Color, name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(color = color)
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = AtmosType.Body,
            color = AtmosColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = AtmosType.Body.copy(fontWeight = FontWeight.SemiBold),
            color = AtmosColors.TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
private fun InsightDivider() {
    AtmosDivider(modifier = Modifier.padding(start = 20.dp))
}

private fun severityColor(severity: AdvisorySeverity): Color = when (severity) {
    AdvisorySeverity.NORMAL -> AtmosColors.StatusGreen
    AdvisorySeverity.ADVISORY -> AtmosColors.AqiModerate
    AdvisorySeverity.WARNING -> AtmosColors.AqiSensitive
    AdvisorySeverity.SEVERE -> AtmosColors.AqiUnhealthy
}

@Composable
private fun humidityWord(severity: AdvisorySeverity): String = when (severity) {
    AdvisorySeverity.NORMAL -> stringResource(R.string.bd_humidity_comfortable)
    AdvisorySeverity.ADVISORY -> stringResource(R.string.bd_humidity_muggy)
    AdvisorySeverity.WARNING -> stringResource(R.string.bd_humidity_oppressive)
    AdvisorySeverity.SEVERE -> stringResource(R.string.bd_humidity_extreme)
}
