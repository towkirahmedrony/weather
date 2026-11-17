package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DailyForecast
import com.example.domain.model.HourlyForecast
import com.example.domain.model.UserPreferences
import com.example.domain.util.UnitFormatter
import com.example.ui.theme.AtmosRainCyan
import com.example.ui.theme.AtmosSunAmber
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun AtmosGlassSection(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.22f))
            .border(0.8.dp, Color.White.copy(alpha = 0.16f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.semantics { heading() }
            )
            trailing?.invoke()
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun HourlyForecastSection(
    hourly: List<HourlyForecast>,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    if (hourly.isEmpty()) return
    val context = LocalContext.current
    var selectedIdx by remember(hourly) { mutableIntStateOf(0) }
    val selectedHour = hourly.getOrNull(selectedIdx) ?: hourly.first()

    AtmosGlassSection(
        title = stringResource(R.string.section_hourly),
        modifier = modifier.testTag("hourly_forecast_section"),
        trailing = {
            Text(
                text = "${UnitFormatter.formatTemp(selectedHour.feelsLikeC, prefs.temperatureUnit)} · ${
                    UnitFormatter.formatWind(selectedHour.windSpeedKmh, prefs.windSpeedUnit)
                }",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    ) {
        val scrollState = rememberScrollState()
        val itemWidth = 64.dp
        val totalChartWidth = itemWidth * hourly.size

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            // Timeline items
            Row(modifier = Modifier.width(totalChartWidth)) {
                hourly.forEachIndexed { idx, h ->
                    val isSelected = idx == selectedIdx
                    val timeLabel = if (idx == 0) {
                        stringResource(R.string.hourly_now)
                    } else {
                        UnitFormatter.formatHourLabel(h.isoTime, prefs.language)
                    }
                    Column(
                        modifier = Modifier
                            .width(itemWidth)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.16f) else Color.Transparent
                            )
                            .clickable { selectedIdx = idx }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = if (isSelected) 1f else 0.78f),
                            maxLines = 1
                        )
                        Spacer(Modifier.height(6.dp))
                        WeatherConditionBadgeIcon(
                            condition = h.condition,
                            isDay = h.isDay,
                            contentDescription = UnitFormatter.conditionLabel(h.condition, context),
                            size = 22.dp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (h.precipProbPercent >= 15) "${h.precipProbPercent}%" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = AtmosRainCyan,
                            modifier = Modifier.height(15.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = UnitFormatter.formatTemp(h.tempC, prefs.temperatureUnit),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Smooth Bezier temperature curve aligned with hourly columns
            val temps = hourly.map { UnitFormatter.convertTempValue(it.tempC, prefs.temperatureUnit) }
            val minT = temps.minOrNull() ?: 0.0
            val maxT = temps.maxOrNull() ?: 1.0
            val spanT = (maxT - minT).coerceAtLeast(2.0)

            Canvas(
                modifier = Modifier
                    .width(totalChartWidth)
                    .height(54.dp)
                    .padding(vertical = 6.dp)
            ) {
                val colW = size.width / hourly.size
                val points = temps.mapIndexed { idx, t ->
                    val x = colW * idx + colW / 2f
                    val norm = ((t - minT) / spanT).toFloat()
                    val y = size.height * (0.85f - norm * 0.70f)
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, size.height)
                        lineTo(points.first().x, size.height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AtmosSunAmber.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        )
                    )
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(AtmosRainCyan, AtmosSunAmber, AtmosSunAmber)
                        ),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }

                points.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedIdx
                    drawCircle(
                        color = if (isSelected) Color.White else AtmosSunAmber,
                        radius = if (isSelected) 6f else 3f,
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
fun SevenDayForecastSection(
    daily: List<DailyForecast>,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    if (daily.isEmpty()) return
    val context = LocalContext.current
    var expandedIdx by remember { mutableIntStateOf(-1) }

    val weekMin = daily.minOf { it.minTempC }
    val weekMax = daily.maxOf { it.maxTempC }
    val weekSpan = (weekMax - weekMin).coerceAtLeast(1.0)

    AtmosGlassSection(
        title = stringResource(R.string.section_daily),
        modifier = modifier.testTag("seven_day_forecast_section")
    ) {
        daily.forEachIndexed { idx, day ->
            val isExpanded = expandedIdx == idx
            val dayName = UnitFormatter.formatDayName(day.isoDate, idx, context, prefs.language)
            val lowStr = UnitFormatter.formatTemp(day.minTempC, prefs.temperatureUnit)
            val highStr = UnitFormatter.formatTemp(day.maxTempC, prefs.temperatureUnit)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { expandedIdx = if (isExpanded) -1 else idx }
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.width(60.dp)
                    )

                    Row(
                        modifier = Modifier.width(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WeatherConditionBadgeIcon(
                            condition = day.condition,
                            isDay = true,
                            contentDescription = UnitFormatter.conditionLabel(day.condition, context),
                            size = 20.dp
                        )
                        if (day.precipProbMaxPercent >= 20) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${day.precipProbMaxPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AtmosRainCyan
                            )
                        }
                    }

                    Text(
                        text = lowStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.width(36.dp)
                    )

                    // Temperature range bar
                    val startFrac = ((day.minTempC - weekMin) / weekSpan).toFloat().coerceIn(0f, 1f)
                    val endFrac = ((day.maxTempC - weekMin) / weekSpan).toFloat().coerceIn(startFrac, 1f)
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .padding(horizontal = 6.dp)
                    ) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.16f),
                            size = size,
                            cornerRadius = CornerRadius(size.height / 2f)
                        )
                        val left = size.width * startFrac
                        val barW = max(size.height, size.width * (endFrac - startFrac))
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(AtmosRainCyan, AtmosSunAmber, Color(0xFFF97316))
                            ),
                            topLeft = Offset(left, 0f),
                            size = Size(barW.coerceAtMost(size.width - left), size.height),
                            cornerRadius = CornerRadius(size.height / 2f)
                        )
                    }

                    Text(
                        text = highStr,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.width(36.dp)
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = UnitFormatter.conditionLabel(day.condition, context),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DailyMetricPill(
                                label = stringResource(R.string.daily_precip_total),
                                value = UnitFormatter.formatPrecip(day.precipSumMm, prefs.precipitationUnit)
                            )
                            DailyMetricPill(
                                label = stringResource(R.string.daily_uv_max),
                                value = day.uvIndexMax.roundToInt().toString()
                            )
                            DailyMetricPill(
                                label = stringResource(R.string.daily_wind_gust),
                                value = UnitFormatter.formatWind(day.windGustsMaxKmh, prefs.windSpeedUnit)
                            )
                            DailyMetricPill(
                                label = stringResource(R.string.daily_humidity_mean),
                                value = "${day.humidityMeanPercent}%"
                            )
                        }
                    }
                }
            }

            if (idx < daily.lastIndex) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 0.6.dp)
            }
        }
    }
}

@Composable
private fun DailyMetricPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.62f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@Composable
fun RainForecastSection(
    hourly: List<HourlyForecast>,
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    if (hourly.isEmpty()) return
    val next24 = hourly.take(24)
    val totalRainMm = next24.sumOf { it.precipMm }
    val peakProb = next24.maxOfOrNull { it.precipProbPercent } ?: 0
    val peakHourlyMm = next24.maxOfOrNull { it.precipMm } ?: 0.0

    val intensityLabel = when {
        peakHourlyMm <= 0.1 && peakProb < 20 -> stringResource(R.string.rain_intensity_none)
        peakHourlyMm < 2.5 -> stringResource(R.string.rain_intensity_light)
        peakHourlyMm < 7.5 -> stringResource(R.string.rain_intensity_moderate)
        peakHourlyMm < 15.0 -> stringResource(R.string.rain_intensity_heavy)
        else -> stringResource(R.string.rain_intensity_violent)
    }

    AtmosGlassSection(
        title = stringResource(R.string.section_rain),
        modifier = modifier.testTag("rain_forecast_section"),
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = AtmosRainCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = intensityLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = AtmosRainCyan
                )
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.rain_expected_24h),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = UnitFormatter.formatPrecip(totalRainMm, prefs.precipitationUnit),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.rain_peak_prob),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "$peakProb%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AtmosRainCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        val maxBarMm = max(peakHourlyMm, 4.0)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
        ) {
            val count = next24.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.54f).coerceAtLeast(4f)

            // Baseline
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.5f
            )

            next24.forEachIndexed { idx, h ->
                val x = idx * slotW + (slotW - barW) / 2f
                // Probability background ghost bar
                val probH = size.height * (h.precipProbPercent / 100f)
                if (probH > 2f) {
                    drawRoundRect(
                        color = AtmosRainCyan.copy(alpha = 0.22f),
                        topLeft = Offset(x, size.height - probH),
                        size = Size(barW, probH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
                // Actual rainfall mm foreground bar
                val mmH = (size.height * (h.precipMm / maxBarMm).toFloat()).coerceIn(0f, size.height)
                if (mmH > 1.5f) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF67E8F9), Color(0xFF0284C7))
                        ),
                        topLeft = Offset(x, size.height - mmH),
                        size = Size(barW, mmH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, 6, 12, 18, 23).forEach { idx ->
                val h = next24.getOrNull(idx)
                if (h != null) {
                    Text(
                        text = if (idx == 0) stringResource(R.string.hourly_now)
                        else UnitFormatter.formatHourLabel(h.isoTime, prefs.language),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}
