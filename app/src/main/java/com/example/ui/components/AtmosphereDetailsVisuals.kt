package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AdvisorySeverity
import com.example.domain.model.AirQualityData
import com.example.domain.model.AppLanguage
import com.example.domain.model.AqiCategory
import com.example.domain.model.AstronomyData
import com.example.domain.model.BangladeshAdvisoryReport
import com.example.domain.model.CurrentWeather
import com.example.domain.model.MoonPhaseType
import com.example.domain.model.UserPreferences
import com.example.domain.util.UnitFormatter
import com.example.ui.theme.AtmosAlertCoral
import com.example.ui.theme.AtmosGoodEmerald
import com.example.ui.theme.AtmosRainCyan
import com.example.ui.theme.AtmosSkyBlue
import com.example.ui.theme.AtmosStormViolet
import com.example.ui.theme.AtmosSunAmber
import com.example.ui.theme.AtmosWarnOrange
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BangladeshAdvisorySection(
    report: BangladeshAdvisoryReport,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    val isBn = prefs.language == AppLanguage.BANGLA

    AtmosGlassSection(
        title = stringResource(R.string.section_bangladesh),
        modifier = modifier.testTag("bangladesh_advisory_section"),
        trailing = {
            if (report.isMonsoonSeason) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AtmosRainCyan.copy(alpha = 0.22f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isBn) "বর্ষা মৌসুম" else "Monsoon Season",
                        style = MaterialTheme.typography.labelSmall,
                        color = AtmosRainCyan
                    )
                }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AdvisoryRowItem(
                icon = Icons.Rounded.Thermostat,
                title = stringResource(R.string.bd_heat_index),
                value = UnitFormatter.formatTemp(report.heatIndexC, prefs.temperatureUnit, true),
                severity = report.heatSeverity,
                detail = heatStatusText(report.heatSeverity, isBn)
            )
            AdvisoryRowItem(
                icon = Icons.Rounded.WaterDrop,
                title = stringResource(R.string.bd_humidity_load),
                value = severityLabel(report.humiditySeverity, isBn),
                severity = report.humiditySeverity,
                detail = if (isBn) "উচ্চ আর্দ্রতা ও তাপ একসাথে গুমোট অস্বস্তি তৈরি করতে পারে।" else "Combined dew point and temperature comfort assessment."
            )
            AdvisoryRowItem(
                icon = Icons.Rounded.Bolt,
                title = stringResource(R.string.bd_storm_cape),
                value = "${report.maxCapeNext24h.roundToInt()} J/kg",
                severity = report.stormSeverity,
                detail = stormStatusText(report.stormSeverity, isBn)
            )
            AdvisoryRowItem(
                icon = Icons.Rounded.WaterDrop,
                title = stringResource(R.string.bd_monsoon_rain),
                value = UnitFormatter.formatPrecip(report.next24hRainTotalMm, prefs.precipitationUnit),
                severity = report.heavyRainSeverity,
                detail = rainStatusText(report.heavyRainSeverity, isBn)
            )
            AdvisoryRowItem(
                icon = Icons.Rounded.Air,
                title = stringResource(R.string.bd_cyclone_wind),
                value = UnitFormatter.formatWind(report.peakWindGustKmh, prefs.windSpeedUnit),
                severity = report.coastalWindSeverity,
                detail = windStatusText(report.coastalWindSeverity, isBn)
            )

            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.bd_source_disclaimer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.76f)
                )
            }
        }
    }
}

@Composable
private fun AdvisoryRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    severity: AdvisorySeverity,
    detail: String
) {
    val badgeColor = when (severity) {
        AdvisorySeverity.NORMAL -> AtmosGoodEmerald
        AdvisorySeverity.ADVISORY -> AtmosSunAmber
        AdvisorySeverity.WARNING -> AtmosWarnOrange
        AdvisorySeverity.SEVERE -> AtmosAlertCoral
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeColor
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

private fun severityLabel(s: AdvisorySeverity, isBn: Boolean): String = when (s) {
    AdvisorySeverity.NORMAL -> if (isBn) "স্বাভাবিক" else "Comfortable"
    AdvisorySeverity.ADVISORY -> if (isBn) "সতর্কতা" else "Muggy"
    AdvisorySeverity.WARNING -> if (isBn) "উচ্চ" else "Oppressive"
    AdvisorySeverity.SEVERE -> if (isBn) "তীব্র" else "Extreme"
}

private fun heatStatusText(s: AdvisorySeverity, isBn: Boolean): String = when (s) {
    AdvisorySeverity.NORMAL -> if (isBn) "তাপজনিত চাপ কম।" else "Low heat stress risk."
    AdvisorySeverity.ADVISORY -> if (isBn) "সাবধানতা: পর্যাপ্ত পানি পান করুন।" else "Caution: stay hydrated during midday sun."
    AdvisorySeverity.WARNING -> if (isBn) "উচ্চ তাপ সূচক: দুপুরে রোদ এড়িয়ে চলুন।" else "High heat index: limit strenuous outdoor exposure."
    AdvisorySeverity.SEVERE -> if (isBn) "চরম তাপপ্রবাহ: হিট স্ট্রোকের ঝুঁকি রয়েছে।" else "Extreme heat danger: avoid midday heat and hydrate."
}

private fun stormStatusText(s: AdvisorySeverity, isBn: Boolean): String = when (s) {
    AdvisorySeverity.NORMAL -> if (isBn) "বজ্রপাতের ঝুঁকি কম।" else "Low convective thunderstorm potential."
    AdvisorySeverity.ADVISORY -> if (isBn) "মাঝারি বজ্রমেঘের সম্ভাবনা রয়েছে।" else "Moderate convective energy; watch afternoon skies."
    AdvisorySeverity.WARNING -> if (isBn) "বজ্রপাত ও ঝোড়ো হাওয়ার সম্ভাবনা। নিরাপদ আশ্রয়ে থাকুন।" else "Thunderstorm & lightning risk; seek shelter if thunder sounds."
    AdvisorySeverity.SEVERE -> if (isBn) "তীব্র কালবৈশাখী ও বজ্রপাত সতর্কতা।" else "High convective / Kalboishakhi lightning risk."
}

private fun rainStatusText(s: AdvisorySeverity, isBn: Boolean): String = when (s) {
    AdvisorySeverity.NORMAL -> if (isBn) "স্বাভাবিক বৃষ্টিপাত বা শুষ্ক।" else "Light or minimal 24h accumulation."
    AdvisorySeverity.ADVISORY -> if (isBn) "মাঝারি থেকে ভারী বৃষ্টি হতে পারে (২২–৪৩ মিমি)।" else "Moderate-heavy rainfall outlook (22–43 mm/24h)."
    AdvisorySeverity.WARNING -> if (isBn) "ভারী বর্ষণ (৪৪–৮৮ মিমি): জলাবদ্ধতা হতে পারে।" else "Heavy rain watch (44–88 mm/24h): localized waterlogging possible."
    AdvisorySeverity.SEVERE -> if (isBn) "অতি ভারী বর্ষণ (৮৯+ মিমি): বন্যার সতর্কতা রাখুন।" else "Very heavy rain (89+ mm/24h): monitor flood advisories."
}

private fun windStatusText(s: AdvisorySeverity, isBn: Boolean): String = when (s) {
    AdvisorySeverity.NORMAL -> if (isBn) "স্বাভাবিক বাতাসের বেগ।" else "Surface winds within normal range."
    AdvisorySeverity.ADVISORY -> if (isBn) "দমকা হাওয়া (৪০+ কিমি/ঘণ্টা)।" else "Breezy gusts (40+ km/h) expected."
    AdvisorySeverity.WARNING -> if (isBn) "ঝোড়ো হাওয়া: নৌযান ও উপকূলে সতর্কতা।" else "Strong gusts (55+ km/h): caution near coast & rivers."
    AdvisorySeverity.SEVERE -> if (isBn) "তীব্র দমকা ঝড়: BMD ঘূর্ণিঝড় সংকেত অনুসরণ করুন।" else "Gale-force gusts (75+ km/h): follow official BMD bulletins."
}

@Composable
fun AtmosphericDetailsSection(
    current: CurrentWeather,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBn = prefs.language == AppLanguage.BANGLA

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_details_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.section_details),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Row 1: Wind Compass + UV Index Arc
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailInstrumentTile(
                title = stringResource(R.string.detail_wind),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = UnitFormatter.formatWind(current.windSpeedKmh, prefs.windSpeedUnit),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "${UnitFormatter.compassDirection(current.windDirectionDeg, isBn)} (${current.windDirectionDeg}°)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtmosSkyBlue
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.detail_gusts)}: ${
                                UnitFormatter.formatWind(current.windGustsKmh, prefs.windSpeedUnit)
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }
                    WindCompassDial(directionDeg = current.windDirectionDeg)
                }
            }

            DetailInstrumentTile(
                title = stringResource(R.string.detail_uv_index),
                modifier = Modifier.weight(1f)
            ) {
                val uv = current.uvIndex
                val uvLabel = when {
                    uv < 3 -> stringResource(R.string.uv_low)
                    uv < 6 -> stringResource(R.string.uv_moderate)
                    uv < 8 -> stringResource(R.string.uv_high)
                    uv < 11 -> stringResource(R.string.uv_very_high)
                    else -> stringResource(R.string.uv_extreme)
                }
                Text(
                    text = "${uv.roundToInt()} · $uvLabel",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(10.dp))
                SpectrumSliderBar(
                    fraction = (uv / 12.0).toFloat().coerceIn(0f, 1f),
                    colors = listOf(
                        AtmosGoodEmerald,
                        AtmosSunAmber,
                        AtmosWarnOrange,
                        AtmosAlertCoral,
                        AtmosStormViolet
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${stringResource(R.string.detail_feels_like)}: ${
                        UnitFormatter.formatTemp(current.feelsLikeC, prefs.temperatureUnit)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }

        // Row 2: Humidity + Dew Point & Pressure Dial
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailInstrumentTile(
                title = stringResource(R.string.detail_humidity),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${current.humidityPercent}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${stringResource(R.string.detail_dew_point)}: ${
                                UnitFormatter.formatTemp(current.dewPointC, prefs.temperatureUnit)
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    CircularGaugeRing(
                        fraction = current.humidityPercent / 100f,
                        color = AtmosRainCyan
                    )
                }
            }

            DetailInstrumentTile(
                title = stringResource(R.string.detail_pressure),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${current.pressureHpa.roundToInt()} hPa",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = UnitFormatter.pressureTrendLabel(current.pressureTrend, context),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtmosSunAmber
                        )
                    }
                    PressureArcGauge(pressureHpa = current.pressureHpa)
                }
            }
        }

        // Row 3: Visibility & Cloud Cover
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailInstrumentTile(
                title = stringResource(R.string.detail_visibility),
                modifier = Modifier.weight(1f)
            ) {
                val vis = current.visibilityKm
                Text(
                    text = "${"%.1f".format(vis)} km",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                val desc = when {
                    vis >= 15.0 -> stringResource(R.string.visibility_excellent)
                    vis >= 8.0 -> stringResource(R.string.visibility_good)
                    vis >= 3.0 -> stringResource(R.string.visibility_moderate)
                    else -> stringResource(R.string.visibility_poor)
                }
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            DetailInstrumentTile(
                title = stringResource(R.string.detail_cloud_cover),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${current.cloudCoverPercent}%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = UnitFormatter.conditionLabel(current.condition, context),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                    CircularGaugeRing(
                        fraction = current.cloudCoverPercent / 100f,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInstrumentTile(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.22f))
            .border(0.8.dp, Color.White.copy(alpha = 0.16f), shape)
            .padding(14.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun WindCompassDial(directionDeg: Int) {
    Canvas(modifier = Modifier.size(54.dp)) {
        val r = size.minDimension / 2f
        val c = Offset(r, r)
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = r - 2f,
            center = c,
            style = Stroke(width = 2.5f)
        )
        // Cardinal ticks
        for (i in 0 until 4) {
            val a = i * PI / 2.0
            drawLine(
                color = if (i == 3) AtmosSunAmber else Color.White.copy(alpha = 0.4f),
                start = Offset(c.x + (cos(a) * (r - 8)).toFloat(), c.y + (sin(a) * (r - 8)).toFloat()),
                end = Offset(c.x + (cos(a) * (r - 2)).toFloat(), c.y + (sin(a) * (r - 2)).toFloat()),
                strokeWidth = 2f
            )
        }
        val rad = Math.toRadians(directionDeg - 90.0)
        val tip = Offset(c.x + (cos(rad) * (r - 8)).toFloat(), c.y + (sin(rad) * (r - 8)).toFloat())
        val tail = Offset(c.x - (cos(rad) * (r - 14)).toFloat(), c.y - (sin(rad) * (r - 14)).toFloat())
        drawLine(color = AtmosSkyBlue, start = tail, end = tip, strokeWidth = 4f, cap = StrokeCap.Round)
        drawCircle(color = Color.White, radius = 4f, center = tip)
    }
}

@Composable
private fun CircularGaugeRing(fraction: Float, color: Color) {
    val clamped = fraction.coerceIn(0f, 1f)
    Canvas(modifier = Modifier.size(46.dp)) {
        val stroke = 5.5.dp.toPx()
        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * clamped,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PressureArcGauge(pressureHpa: Double) {
    val norm = ((pressureHpa - 970.0) / 70.0).toFloat().coerceIn(0f, 1f)
    Canvas(modifier = Modifier.size(54.dp)) {
        val stroke = 5.dp.toPx()
        drawArc(
            color = Color.White.copy(alpha = 0.18f),
            startAngle = 140f,
            sweepAngle = 260f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.horizontalGradient(listOf(AtmosRainCyan, AtmosSunAmber)),
            startAngle = 140f,
            sweepAngle = 260f * norm,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun SpectrumSliderBar(fraction: Float, colors: List<Color>) {
    val f = fraction.coerceIn(0f, 1f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val barH = 5.dp.toPx()
        val top = (size.height - barH) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(colors),
            topLeft = Offset(0f, top),
            size = Size(size.width, barH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2f)
        )
        val dotX = (size.width * f).coerceIn(5.dp.toPx(), size.width - 5.dp.toPx())
        drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(dotX, size.height / 2f))
    }
}

@Composable
fun AirQualitySection(
    aqi: AirQualityData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cat = aqi.category
    val catColor = when (cat) {
        AqiCategory.GOOD -> AtmosGoodEmerald
        AqiCategory.MODERATE -> AtmosSunAmber
        AqiCategory.SENSITIVE -> AtmosWarnOrange
        AqiCategory.UNHEALTHY -> AtmosAlertCoral
        AqiCategory.VERY_UNHEALTHY -> AtmosStormViolet
        AqiCategory.HAZARDOUS -> Color(0xFFE11D48)
    }

    AtmosGlassSection(
        title = stringResource(R.string.section_aqi),
        modifier = modifier.testTag("air_quality_section"),
        trailing = {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.22f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${stringResource(R.string.aqi_us_label)} ${aqi.usAqi}",
                    style = MaterialTheme.typography.labelSmall,
                    color = catColor
                )
            }
        }
    ) {
        Text(
            text = "${aqi.usAqi} · ${UnitFormatter.aqiCategoryLabel(cat, context)}",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = UnitFormatter.aqiAdvice(cat, context),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(12.dp))
        SpectrumSliderBar(
            fraction = (aqi.usAqi / 300f).coerceIn(0f, 1f),
            colors = listOf(
                AtmosGoodEmerald,
                AtmosSunAmber,
                AtmosWarnOrange,
                AtmosAlertCoral,
                AtmosStormViolet
            )
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PollutantChip(
                label = stringResource(R.string.aqi_pm25),
                value = "${"%.1f".format(aqi.pm25)} µg/m³",
                modifier = Modifier.weight(1f)
            )
            PollutantChip(
                label = stringResource(R.string.aqi_pm10),
                value = "${"%.1f".format(aqi.pm10)} µg/m³",
                modifier = Modifier.weight(1f)
            )
            PollutantChip(
                label = stringResource(R.string.aqi_o3),
                value = "${"%.0f".format(aqi.ozone)} µg/m³",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PollutantChip(
                label = stringResource(R.string.aqi_no2),
                value = "${"%.1f".format(aqi.nitrogenDioxide)}",
                modifier = Modifier.weight(1f)
            )
            PollutantChip(
                label = stringResource(R.string.aqi_so2),
                value = "${"%.1f".format(aqi.sulphurDioxide)}",
                modifier = Modifier.weight(1f)
            )
            PollutantChip(
                label = stringResource(R.string.aqi_co),
                value = "${"%.0f".format(aqi.carbonMonoxide)}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PollutantChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.65f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@Composable
fun SunMoonSection(
    astronomy: AstronomyData,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBn = prefs.language == AppLanguage.BANGLA

    AtmosGlassSection(
        title = stringResource(R.string.section_sun_moon),
        modifier = modifier.testTag("sun_moon_section"),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WbTwilight,
                    contentDescription = null,
                    tint = AtmosSunAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${stringResource(R.string.daylight_duration)} ${
                        UnitFormatter.formatDaylightDuration(astronomy.daylightDurationSeconds, isBn)
                    }",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    ) {
        // Solar horizon arc
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            val w = size.width
            val h = size.height
            val horizonY = h * 0.78f

            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, horizonY),
                end = Offset(w, horizonY),
                strokeWidth = 2f
            )

            val arcPath = Path().apply {
                moveTo(w * 0.08f, horizonY)
                quadraticTo(w * 0.5f, -h * 0.30f, w * 0.92f, horizonY)
            }
            drawPath(
                path = arcPath,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFF59E0B), Color(0xFFFDE047), Color(0xFFFB7185))
                ),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            // Midday solar glow dot
            val sunPos = Offset(w * 0.50f, h * 0.24f)
            drawCircle(color = Color(0x44FDE047), radius = 14f, center = sunPos)
            drawCircle(color = Color(0xFFFDE047), radius = 7f, center = sunPos)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.sunrise),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Text(
                    text = UnitFormatter.formatTime12h(astronomy.sunriseIso),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.sunset),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.65f)
                )
                Text(
                    text = UnitFormatter.formatTime12h(astronomy.sunsetIso),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Lunar disc & phase row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoonPhaseDisc(
                phaseType = astronomy.moonPhaseType,
                illuminationPercent = astronomy.moonIlluminationPercent
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = UnitFormatter.moonPhaseLabel(astronomy.moonPhaseType, context),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.illumination_fmt, astronomy.moonIlluminationPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFDE68A)
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stringResource(R.string.moonrise)}: ${UnitFormatter.formatTime12h(astronomy.moonriseTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Text(
                        text = "${stringResource(R.string.moonset)}: ${UnitFormatter.formatTime12h(astronomy.moonsetTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoonPhaseDisc(phaseType: MoonPhaseType, illuminationPercent: Int) {
    Canvas(modifier = Modifier.size(46.dp)) {
        val r = size.minDimension / 2f - 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        // Dark lunar face
        drawCircle(color = Color(0xFF1E293B), radius = r, center = c)
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = r,
            center = c,
            style = Stroke(width = 1.5f)
        )
        // Illuminated portion approximated cleanly
        val isWaxing = phaseType in listOf(
            MoonPhaseType.WAXING_CRESCENT,
            MoonPhaseType.FIRST_QUARTER,
            MoonPhaseType.WAXING_GIBBOUS,
            MoonPhaseType.FULL_MOON
        )
        val frac = (illuminationPercent / 100f).coerceIn(0.02f, 1f)
        if (frac > 0.92f) {
            drawCircle(color = Color(0xFFFEF9C3), radius = r, center = c)
        } else if (frac > 0.05f) {
            drawArc(
                color = Color(0xFFFEF9C3),
                startAngle = if (isWaxing) -90f else 90f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2f, r * 2f)
            )
            if (frac > 0.5f) {
                val bulgeW = r * 2f * ((frac - 0.5f) * 2f)
                drawOval(
                    color = Color(0xFFFEF9C3),
                    topLeft = Offset(c.x - bulgeW / 2f, c.y - r),
                    size = Size(bulgeW, r * 2f)
                )
            } else {
                val maskW = r * 2f * ((0.5f - frac) * 2f)
                drawOval(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(c.x - maskW / 2f, c.y - r),
                    size = Size(maskW, r * 2f)
                )
            }
        }
    }
}
