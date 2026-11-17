package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.WeatherCondition
import com.example.ui.theme.AtmosColors

/**
 * Hand-drawn line-art weather glyphs for the redesign.
 *
 * Everything is stroked (no fills, no emoji) with rounded caps/joins so the icons stay
 * crisp at 22dp in the weekly columns and at 76dp in the hero.
 * Geometry is authored on a 24 x 24 grid and scaled to the requested size.
 */
@Composable
fun WeatherLineIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    tint: Color = AtmosColors.CloudStroke
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = (this.size.minDimension * 0.062f).coerceAtLeast(1.45.dp.toPx())

        when (condition) {
            WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR -> {
                if (isDay) drawLineSun(Offset(12f * s, 12f * s), 4.6f * s, stroke, tint)
                else drawCrescent(Offset(12f * s, 12f * s), 6.4f * s, stroke, tint)
            }

            WeatherCondition.PARTLY_CLOUDY -> {
                if (isDay) {
                    drawLineSun(Offset(17.2f * s, 6.6f * s), 3.1f * s, stroke, tint)
                } else {
                    drawCrescent(Offset(17.2f * s, 6.6f * s), 4.2f * s, stroke, tint)
                }
                drawCloudOutline(cloudPath(0.86f * s, -0.4f * s, 3.1f * s), stroke, tint)
            }

            WeatherCondition.OVERCAST -> {
                drawCloudOutline(cloudPath(0.92f * s, 0f, 1.4f * s), stroke, tint)
            }

            WeatherCondition.FOG -> {
                drawCloudOutline(cloudPath(0.92f * s, 0f, 0f), stroke, tint)
                for (i in 0..2) {
                    val y = (18.2f + i * 2.2f) * s
                    drawLine(
                        color = tint,
                        start = Offset((5f + i) * s, y),
                        end = Offset((19f - i) * s, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> {
                drawCloudOutline(cloudPath(0.94f * s, 0f, -0.4f * s), stroke, tint)
                val drops = when (condition) {
                    WeatherCondition.DRIZZLE -> 2
                    WeatherCondition.HEAVY_RAIN -> 4
                    else -> 3
                }
                for (i in 0 until drops) {
                    val x = (8f + i * (8f / (drops - 1).coerceAtLeast(1))) * s
                    drawLine(
                        color = tint,
                        start = Offset(x, 17.6f * s),
                        end = Offset(x - 1.6f * s, 20.4f * s),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherCondition.THUNDERSTORM -> {
                drawCloudOutline(cloudPath(0.94f * s, 0f, -1.4f * s), stroke, tint)
                val bolt = Path().apply {
                    moveTo(12.6f * s, 15.2f * s)
                    lineTo(9.4f * s, 19.2f * s)
                    lineTo(12.1f * s, 19.2f * s)
                    lineTo(10.6f * s, 22.4f * s)
                    lineTo(15.4f * s, 18.2f * s)
                    lineTo(12.6f * s, 18.2f * s)
                }
                drawPath(
                    path = bolt,
                    color = tint,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            WeatherCondition.SNOW -> {
                drawCloudOutline(cloudPath(0.92f * s, 0f, -0.6f * s), stroke, tint)
                for (i in 0..2) {
                    drawCircle(
                        color = tint,
                        radius = stroke * 0.9f,
                        center = Offset((8.5f + i * 3.6f) * s, 19.4f * s)
                    )
                }
            }

            WeatherCondition.SQUALL -> {
                drawCloudOutline(cloudPath(0.9f * s, 0f, -0.6f * s), stroke, tint)
                for (i in 0..1) {
                    val y = (18.2f + i * 2.6f) * s
                    drawLine(
                        color = tint,
                        start = Offset(5f * s, y),
                        end = Offset(19f * s, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/** Sun as a circle plus eight short rays, all stroked. */
private fun DrawScope.drawLineSun(center: Offset, radius: Float, stroke: Float, tint: Color) {
    drawCircle(color = tint, radius = radius, center = center, style = Stroke(width = stroke))
    for (i in 0 until 8) {
        val angle = Math.toRadians(i * 45.0)
        val cos = kotlin.math.cos(angle).toFloat()
        val sin = kotlin.math.sin(angle).toFloat()
        drawLine(
            color = tint,
            start = Offset(center.x + cos * radius * 1.35f, center.y + sin * radius * 1.35f),
            end = Offset(center.x + cos * radius * 1.85f, center.y + sin * radius * 1.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/** Crescent moon: outer right-hand arc closed by an inner curve. */
private fun DrawScope.drawCrescent(center: Offset, radius: Float, stroke: Float, tint: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = center.x - radius,
                top = center.y - radius,
                right = center.x + radius,
                bottom = center.y + radius
            ),
            startAngleDegrees = -90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        quadraticTo(center.x + radius * 0.42f, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(
        path = path,
        color = tint,
        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Cloud silhouette authored inside the 24 x 24 grid, then scaled by [s] and shifted.
 * [dx]/[dy] are expressed in grid units so callers can nudge the cloud up to make
 * room for rain, snow or a lightning bolt.
 */
private fun cloudPath(s: Float, dx: Float, dy: Float): Path {
    fun x(v: Float) = (v + dx) * s
    fun y(v: Float) = (v + dy) * s
    return Path().apply {
        moveTo(x(6.8f), y(16.6f))
        cubicTo(x(4.32f), y(16.6f), x(2.3f), y(14.58f), x(2.3f), y(12.1f))
        cubicTo(x(2.3f), y(9.9f), x(3.86f), y(8.05f), x(6.0f), y(7.68f))
        cubicTo(x(6.5f), y(5.1f), x(8.77f), y(3.2f), x(11.45f), y(3.2f))
        cubicTo(x(14.5f), y(3.2f), x(17.07f), y(5.5f), x(17.5f), y(8.45f))
        cubicTo(x(19.3f), y(8.95f), x(20.6f), y(10.6f), x(20.6f), y(12.55f))
        cubicTo(x(20.6f), y(14.8f), x(18.78f), y(16.6f), x(16.55f), y(16.6f))
        close()
    }
}

private fun DrawScope.drawCloudOutline(path: Path, stroke: Float, tint: Color) {
    drawPath(
        path = path,
        color = tint,
        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Standalone line icon used for the small metric tiles (wind, humidity, UV, rain).
 * Not condition-driven, so it stays out of [WeatherLineIcon].
 */
enum class AtmosGlyph { WIND, HUMIDITY, UV, RAIN, PRESSURE, EYE, CLOUD, DROPLET, GUST }

@Composable
fun AtmosGlyphIcon(
    glyph: AtmosGlyph,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = AtmosColors.TextMuted
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = (this.size.minDimension * 0.085f).coerceAtLeast(1.4.dp.toPx())
        val line = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (glyph) {
            AtmosGlyph.WIND, AtmosGlyph.GUST -> {
                drawPath(
                    path = Path().apply {
                        moveTo(3f * s, 8.5f * s)
                        lineTo(14f * s, 8.5f * s)
                        quadraticTo(18.2f * s, 8.5f * s, 18f * s, 5.6f * s)
                        moveTo(3f * s, 13f * s)
                        lineTo(19.5f * s, 13f * s)
                        moveTo(3f * s, 17.5f * s)
                        lineTo(15f * s, 17.5f * s)
                        quadraticTo(19f * s, 17.5f * s, 18.8f * s, 20.2f * s)
                    },
                    color = tint,
                    style = line
                )
            }

            AtmosGlyph.HUMIDITY, AtmosGlyph.DROPLET -> {
                drawPath(
                    path = Path().apply {
                        moveTo(12f * s, 3.4f * s)
                        cubicTo(12f * s, 3.4f * s, 5.6f * s, 10.6f * s, 5.6f * s, 14.6f * s)
                        cubicTo(5.6f * s, 18.1f * s, 8.5f * s, 21f * s, 12f * s, 21f * s)
                        cubicTo(15.5f * s, 21f * s, 18.4f * s, 18.1f * s, 18.4f * s, 14.6f * s)
                        cubicTo(18.4f * s, 10.6f * s, 12f * s, 3.4f * s, 12f * s, 3.4f * s)
                        close()
                    },
                    color = tint,
                    style = line
                )
            }

            AtmosGlyph.UV -> drawLineSun(Offset(12f * s, 12f * s), 4.2f * s, stroke, tint)

            AtmosGlyph.RAIN -> {
                drawPath(
                    path = cloudPath(0.86f * s, 0.4f * s, -0.8f * s),
                    color = tint,
                    style = line
                )
                for (i in 0..2) {
                    val x = (8.5f + i * 3.4f) * s
                    drawLine(
                        color = tint,
                        start = Offset(x, 18.4f * s),
                        end = Offset(x - 1.4f * s, 21f * s),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }

            AtmosGlyph.CLOUD -> {
                drawPath(path = cloudPath(0.94f * s, 0f, 1.2f * s), color = tint, style = line)
            }

            AtmosGlyph.PRESSURE -> {
                drawCircle(
                    color = tint,
                    radius = 7.4f * s,
                    center = Offset(12f * s, 12f * s),
                    style = Stroke(width = stroke)
                )
                drawPath(
                    path = Path().apply {
                        moveTo(12f * s, 8.2f * s)
                        lineTo(14.6f * s, 14.4f * s)
                    },
                    color = tint,
                    style = line
                )
            }

            AtmosGlyph.EYE -> {
                drawPath(
                    path = Path().apply {
                        moveTo(2.8f * s, 12f * s)
                        quadraticTo(12f * s, 3.6f * s, 21.2f * s, 12f * s)
                        quadraticTo(12f * s, 20.4f * s, 2.8f * s, 12f * s)
                        close()
                    },
                    color = tint,
                    style = line
                )
                drawCircle(
                    color = tint,
                    radius = 2.6f * s,
                    center = Offset(12f * s, 12f * s),
                    style = Stroke(width = stroke)
                )
            }
        }
    }
}

/** 2 x 2 rounded squares, used by the "Details" tab. */
@Composable
fun GridGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = AtmosColors.TextMuted
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = (this.size.minDimension * 0.075f).coerceAtLeast(1.4.dp.toPx())
        val cell = 7.4f * s
        val radius = 2.2f * s
        listOf(
            Offset(3.6f * s, 3.6f * s),
            Offset(13f * s, 3.6f * s),
            Offset(3.6f * s, 13f * s),
            Offset(13f * s, 13f * s)
        ).forEach { topLeft ->
            drawRoundRect(
                color = tint,
                topLeft = topLeft,
                size = Size(cell, cell),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                style = Stroke(width = stroke)
            )
        }
    }
}

/** Small cloud outline for the "Today" tab. */
@Composable
fun CloudGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = AtmosColors.TextMuted
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = (this.size.minDimension * 0.075f).coerceAtLeast(1.4.dp.toPx())
        drawPath(
            path = cloudPath(0.94f * s, 0f, 1.4f * s),
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Wind-lines glyph for the "Air" tab. */
@Composable
fun WindGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = AtmosColors.TextMuted
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = (this.size.minDimension * 0.075f).coerceAtLeast(1.4.dp.toPx())
        drawPath(
            path = Path().apply {
                moveTo(3f * s, 7.6f * s)
                lineTo(14f * s, 7.6f * s)
                quadraticTo(18.4f * s, 7.6f * s, 18.1f * s, 4.6f * s)
                moveTo(3f * s, 12f * s)
                lineTo(19.6f * s, 12f * s)
                moveTo(3f * s, 16.4f * s)
                lineTo(15f * s, 16.4f * s)
                quadraticTo(19.2f * s, 16.4f * s, 18.9f * s, 19.4f * s)
            },
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Thin chevron used on the air quality card and screen headers. */
@Composable
fun ChevronGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    tint: Color = AtmosColors.TextMuted,
    pointingLeft: Boolean = false
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = (this.size.minDimension * 0.11f).coerceAtLeast(1.5.dp.toPx())
        val sign = if (pointingLeft) -1f else 1f
        val cx = 12f * s
        drawPath(
            path = Path().apply {
                moveTo(cx - 4.2f * s * sign, 5.4f * s)
                lineTo(cx + 3.6f * s * sign, 12f * s)
                lineTo(cx - 4.2f * s * sign, 18.6f * s)
            },
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/** Location pin outline. */
@Composable
fun PinGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    tint: Color = AtmosColors.Accent
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = (this.size.minDimension * 0.09f).coerceAtLeast(1.4.dp.toPx())
        drawPath(
            path = Path().apply {
                moveTo(12f * s, 21.4f * s)
                cubicTo(12f * s, 21.4f * s, 4.8f * s, 14.6f * s, 4.8f * s, 9.8f * s)
                cubicTo(4.8f * s, 5.8f * s, 8f * s, 2.6f * s, 12f * s, 2.6f * s)
                cubicTo(16f * s, 2.6f * s, 19.2f * s, 5.8f * s, 19.2f * s, 9.8f * s)
                cubicTo(19.2f * s, 14.6f * s, 12f * s, 21.4f * s, 12f * s, 21.4f * s)
                close()
            },
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(
            color = tint,
            radius = 2.4f * s,
            center = Offset(12f * s, 9.8f * s),
            style = Stroke(width = stroke)
        )
    }
}

/** Thin gear outline for the settings round button. */
@Composable
fun GearGlyph(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = AtmosColors.TextMuted
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = (this.size.minDimension * 0.085f).coerceAtLeast(1.4.dp.toPx())
        drawCircle(
            color = tint,
            radius = 3.1f * s,
            center = Offset(12f * s, 12f * s),
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = tint,
            radius = 8.1f * s,
            center = Offset(12f * s, 12f * s),
            style = Stroke(width = stroke)
        )
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            drawLine(
                color = tint,
                start = Offset(12f * s + cos * 7.4f * s, 12f * s + sin * 7.4f * s),
                end = Offset(12f * s + cos * 9.6f * s, 12f * s + sin * 9.6f * s),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
