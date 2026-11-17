package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.repository.BangladeshQuickCities
import com.example.domain.model.GridWeatherPoint
import com.example.domain.model.UserPreferences
import com.example.domain.model.WeatherMapLayer
import com.example.domain.util.UnitFormatter
import com.example.ui.theme.AtmosRainCyan
import com.example.ui.theme.AtmosSkyBlue
import com.example.ui.theme.AtmosSunAmber
import com.example.ui.viewmodel.WeatherMapState
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.tan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherMapScreen(
    mapState: WeatherMapState,
    prefs: UserPreferences,
    onBack: () -> Unit,
    onSelectLayer: (WeatherMapLayer) -> Unit,
    onViewportChanged: (Double, Double, Int) -> Unit,
    onSelectPoint: (GridWeatherPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lat by remember(mapState.centerLat) { mutableDoubleStateOf(mapState.centerLat) }
    var lon by remember(mapState.centerLon) { mutableDoubleStateOf(mapState.centerLon) }
    var zoom by remember(mapState.zoomLevel) { mutableIntStateOf(mapState.zoomLevel) }

    LaunchedEffect(Unit) {
        onViewportChanged(lat, lon, zoom)
    }

    val windLoop = rememberInfiniteTransition(label = "windFlow")
    val windPhase by windLoop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "windPhase"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("map_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (mapState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B1628),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0B1628))
        ) {
            // Layer Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeatherMapLayer.entries.forEach { layer ->
                    val (labelRes, icon) = when (layer) {
                        WeatherMapLayer.PRECIPITATION -> R.string.layer_precipitation to Icons.Rounded.WaterDrop
                        WeatherMapLayer.TEMPERATURE -> R.string.layer_temperature to Icons.Rounded.Thermostat
                        WeatherMapLayer.CLOUDS -> R.string.layer_clouds to Icons.Rounded.Cloud
                        WeatherMapLayer.WIND -> R.string.layer_wind to Icons.Rounded.Air
                    }
                    FilterChip(
                        selected = mapState.activeLayer == layer,
                        onClick = { onSelectLayer(layer) },
                        label = { Text(stringResource(labelRes)) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AtmosSkyBlue,
                            selectedLabelColor = Color(0xFF0B1628),
                            selectedLeadingIconColor = Color(0xFF0B1628),
                            containerColor = Color(0xFF15253E),
                            labelColor = Color.White,
                            iconColor = Color.White.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.testTag("map_layer_${layer.name.lowercase()}")
                    )
                }
            }

            // Quick jump Bangladesh cities bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BangladeshQuickCities.all.take(6).forEach { city ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                lat = city.latitude
                                lon = city.longitude
                                zoom = 7
                                onViewportChanged(lat, lon, zoom)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = city.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Interactive Slippy Map Canvas
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .background(Color(0xFF0E1B2E))
                    .pointerInput(zoom) {
                        detectDragGestures(
                            onDragEnd = {
                                onViewportChanged(lat, lon, zoom)
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            val worldPx = 256.0 * 2.0.pow(zoom)
                            val cx = lonToWorldX(lon, worldPx) - dragAmount.x
                            val cy = latToWorldY(lat, worldPx) - dragAmount.y
                            lon = worldXToLon(cx, worldPx)
                            lat = worldYToLat(cy, worldPx).coerceIn(-80.0, 80.0)
                        }
                    }
            ) {
                val density = LocalDensity.current
                val viewW = with(density) { maxWidth.toPx() }
                val viewH = with(density) { maxHeight.toPx() }
                val tileSizePx = 256
                val worldPx = tileSizePx * 2.0.pow(zoom)
                val centerWorldX = lonToWorldX(lon, worldPx)
                val centerWorldY = latToWorldY(lat, worldPx)
                val topLeftWorldX = centerWorldX - viewW / 2.0
                val topLeftWorldY = centerWorldY - viewH / 2.0

                val startTileX = floor(topLeftWorldX / tileSizePx).toInt()
                val endTileX = floor((topLeftWorldX + viewW) / tileSizePx).toInt()
                val startTileY = floor(topLeftWorldY / tileSizePx).toInt()
                val endTileY = floor((topLeftWorldY + viewH) / tileSizePx).toInt()
                val maxTileIdx = (1 shl zoom)

                // 1. Base Map Tiles (CartoDB Dark Matter) + RainViewer Live Radar Tiles
                for (ty in startTileY..endTileY) {
                    if (ty < 0 || ty >= maxTileIdx) continue
                    for (tx in startTileX..endTileX) {
                        val wrappedTx = ((tx % maxTileIdx) + maxTileIdx) % maxTileIdx
                        val screenX = (tx * tileSizePx - topLeftWorldX).roundToInt()
                        val screenY = (ty * tileSizePx - topLeftWorldY).roundToInt()
                        val tileDp = with(density) { tileSizePx.toDp() }

                        val baseUrl = "https://a.basemaps.cartocdn.com/dark_all/$zoom/$wrappedTx/$ty.png"
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(baseUrl)
                                .addHeader("User-Agent", "AtmosWeatherAndroid/1.0")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .offset { IntOffset(screenX, screenY) }
                                .size(tileDp)
                        )

                        if (mapState.activeLayer == WeatherMapLayer.PRECIPITATION && mapState.radarPath != null) {
                            val radarUrl = "${mapState.radarPath}/256/$zoom/$wrappedTx/$ty/2/1_1.png"
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(radarUrl)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                alpha = 0.72f,
                                modifier = Modifier
                                    .offset { IntOffset(screenX, screenY) }
                                    .size(tileDp)
                            )
                        }
                    }
                }

                // 2. Gridded Weather Overlays (Temperature, Clouds, Wind)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    mapState.gridPoints.forEach { pt ->
                        val px = (lonToWorldX(pt.longitude, worldPx) - topLeftWorldX).toFloat()
                        val py = (latToWorldY(pt.latitude, worldPx) - topLeftWorldY).toFloat()
                        if (px < -120f || px > size.width + 120f || py < -120f || py > size.height + 120f) return@forEach

                        when (mapState.activeLayer) {
                            WeatherMapLayer.TEMPERATURE -> {
                                val heatColor = tempToColor(pt.tempC)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(heatColor.copy(alpha = 0.42f), Color.Transparent),
                                        center = Offset(px, py),
                                        radius = 120f
                                    ),
                                    radius = 120f,
                                    center = Offset(px, py)
                                )
                            }
                            WeatherMapLayer.CLOUDS -> {
                                val cloudAlpha = (pt.cloudCoverPercent / 100f * 0.55f).coerceIn(0.05f, 0.55f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.White.copy(alpha = cloudAlpha), Color.Transparent),
                                        center = Offset(px, py),
                                        radius = 115f
                                    ),
                                    radius = 115f,
                                    center = Offset(px, py)
                                )
                            }
                            WeatherMapLayer.WIND -> {
                                val rad = Math.toRadians((pt.windDirectionDeg + 180.0))
                                val speedFactor = (pt.windSpeedKmh / 25.0).coerceIn(0.4, 2.2).toFloat()
                                val len = 34f * speedFactor
                                val offsetShift = (windPhase * len)
                                val dx = cos(rad).toFloat()
                                val dy = sin(rad).toFloat()
                                val start = Offset(px - dx * (len * 0.5f) + dx * offsetShift * 0.4f, py - dy * (len * 0.5f) + dy * offsetShift * 0.4f)
                                val end = Offset(start.x + dx * len, start.y + dy * len)
                                drawLine(
                                    color = AtmosSkyBlue.copy(alpha = 0.85f),
                                    start = start,
                                    end = end,
                                    strokeWidth = 3.2f,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(color = Color.White, radius = 3.5f, center = end)
                            }
                            WeatherMapLayer.PRECIPITATION -> {
                                if (pt.precipMm > 0.05) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(AtmosRainCyan.copy(alpha = 0.35f), Color.Transparent),
                                            center = Offset(px, py),
                                            radius = 95f
                                        ),
                                        radius = 95f,
                                        center = Offset(px, py)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Clickable Named City / Station Markers
                mapState.gridPoints.filter { !it.label.isNullOrBlank() }.forEach { pt ->
                    val px = (lonToWorldX(pt.longitude, worldPx) - topLeftWorldX).roundToInt()
                    val py = (latToWorldY(pt.latitude, worldPx) - topLeftWorldY).roundToInt()
                    val valueText = when (mapState.activeLayer) {
                        WeatherMapLayer.TEMPERATURE -> UnitFormatter.formatTemp(pt.tempC, prefs.temperatureUnit)
                        WeatherMapLayer.CLOUDS -> "${pt.cloudCoverPercent}%"
                        WeatherMapLayer.WIND -> UnitFormatter.formatWind(pt.windSpeedKmh, prefs.windSpeedUnit)
                        WeatherMapLayer.PRECIPITATION -> UnitFormatter.formatPrecip(pt.precipMm, prefs.precipitationUnit)
                    }

                    Column(
                        modifier = Modifier
                            .offset { IntOffset(px - 42, py - 24) }
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC0F1E34))
                            .border(1.dp, AtmosSkyBlue.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .clickable { onSelectPoint(pt) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = pt.label.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = valueText,
                            style = MaterialTheme.typography.labelLarge,
                            color = AtmosSunAmber
                        )
                    }
                }

                // Zoom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xCC0F1E34))
                        .border(0.8.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    IconButton(onClick = {
                        if (zoom < 10) {
                            zoom += 1
                            onViewportChanged(lat, lon, zoom)
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Zoom in", tint = Color.White)
                    }
                    IconButton(onClick = {
                        if (zoom > 3) {
                            zoom -= 1
                            onViewportChanged(lat, lon, zoom)
                        }
                    }) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Zoom out", tint = Color.White)
                    }
                }

                // Bottom Overlay Info / Selected Station Card
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp)
                ) {
                    mapState.selectedPoint?.let { sp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xE60F1E34))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                                .clickable { onSelectPoint(null) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = sp.label ?: "${"%.2f".format(sp.latitude)}, ${"%.2f".format(sp.longitude)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "${stringResource(R.string.layer_clouds)}: ${sp.cloudCoverPercent}% · ${
                                        stringResource(R.string.layer_wind)
                                    }: ${UnitFormatter.formatWind(sp.windSpeedKmh, prefs.windSpeedUnit)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                            Text(
                                text = UnitFormatter.formatTemp(sp.tempC, prefs.temperatureUnit, true),
                                style = MaterialTheme.typography.headlineSmall,
                                color = AtmosSunAmber
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xB30F1E34))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        val layerSource = if (mapState.activeLayer == WeatherMapLayer.PRECIPITATION) {
                            "${stringResource(R.string.map_radar_live)} (RainViewer) · ${stringResource(R.string.map_tap_hint)}"
                        } else {
                            "${stringResource(R.string.map_grid_live)} (Open-Meteo) · ${stringResource(R.string.map_tap_hint)}"
                        }
                        Text(
                            text = layerSource,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

private fun lonToWorldX(lon: Double, worldPx: Double): Double =
    ((lon + 180.0) / 360.0) * worldPx

private fun latToWorldY(lat: Double, worldPx: Double): Double {
    val rad = Math.toRadians(lat.coerceIn(-85.05, 85.05))
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * worldPx
}

private fun worldXToLon(x: Double, worldPx: Double): Double =
    ((x / worldPx) * 360.0 - 180.0).let { ((it + 180.0) % 360.0 + 360.0) % 360.0 - 180.0 }

private fun worldYToLat(y: Double, worldPx: Double): Double {
    val n = PI - 2.0 * PI * (y / worldPx)
    return Math.toDegrees(atan(sinh(n)))
}

private fun tempToColor(tempC: Double): Color = when {
    tempC >= 36.0 -> Color(0xFFEF4444)
    tempC >= 30.0 -> Color(0xFFF97316)
    tempC >= 24.0 -> Color(0xFFFBBF24)
    tempC >= 16.0 -> Color(0xFF34D399)
    tempC >= 8.0 -> Color(0xFF38BDF8)
    else -> Color(0xFF6366F1)
}
