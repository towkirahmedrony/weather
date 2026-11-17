package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.MoonPhaseType
import com.example.ui.theme.AtmosColors
import com.example.ui.theme.AtmosType
import java.util.Locale

/**
 * Shared building blocks for the Today / Air / Details redesign.
 * All spacing, radii and colours mirror the three reference exports.
 */

/** The three top-level destinations of the bottom navigation bar. */
enum class AtmosTab { TODAY, AIR, DETAILS }

/** Flat surface used for every card on the redesigned screens. */
@Composable
fun AtmosCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(cornerRadius))
        .background(AtmosColors.Card)
    Column(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        content = {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    )
}

/** 11sp uppercase, weight 500, 0.08em tracking, muted. */
@Composable
fun AtmosSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AtmosColors.TextMuted
) {
    Text(
        text = text.uppercase(Locale.ROOT),
        style = AtmosType.SectionLabel,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Label on the left, optional trailing content (chevron, status word) on the right. */
@Composable
fun AtmosCardHeader(
    label: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AtmosSectionLabel(text = label, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun AtmosDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AtmosColors.Divider)
    )
}

/**
 * Six-segment AQI scale with a white marker.
 *
 * Segment weights are 50/50/50/50/100/50 (sum 350) and the marker is positioned at
 * clamp(aqi / 350, 0, 1) of the full bar width, exactly as specified by the design.
 */
@Composable
fun AqiScaleBar(
    usAqi: Int,
    modifier: Modifier = Modifier,
    barHeight: Dp = 8.dp
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val barWidth = maxWidth
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AtmosColors.AqiScaleWeights.forEachIndexed { index, weight ->
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(barHeight / 2))
                        .background(AtmosColors.AqiScaleColors[index])
                )
            }
        }

        val fraction = (usAqi / AtmosColors.AQI_SCALE_MAX).coerceIn(0f, 1f)
        val dot = 12.dp
        val x = (barWidth * fraction - dot / 2).coerceIn(0.dp, (barWidth - dot).coerceAtLeast(0.dp))
        Box(
            modifier = Modifier
                .offset(x = x, y = (barHeight - dot) / 2)
                .size(dot)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** Sticky bottom navigation: icon + 11sp label, 1dp top border. */
@Composable
fun AtmosBottomNav(
    selected: AtmosTab,
    onSelect: (AtmosTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AtmosColors.Background)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AtmosColors.NavBorder)
        )
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)) {
            NavItem(
                label = stringResource(R.string.nav_today),
                selected = selected == AtmosTab.TODAY,
                onClick = { onSelect(AtmosTab.TODAY) }
            ) { tint -> CloudGlyph(tint = tint) }

            NavItem(
                label = stringResource(R.string.nav_air),
                selected = selected == AtmosTab.AIR,
                onClick = { onSelect(AtmosTab.AIR) }
            ) { tint -> WindGlyph(tint = tint) }

            NavItem(
                label = stringResource(R.string.nav_details),
                selected = selected == AtmosTab.DETAILS,
                onClick = { onSelect(AtmosTab.DETAILS) }
            ) { tint -> GridGlyph(tint = tint) }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit
) {
    val tint = if (selected) AtmosColors.Accent else AtmosColors.TextMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(tint)
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = AtmosType.NavLabel,
            color = tint,
            maxLines = 1
        )
    }
}

/** Header shared by the Air and Details screens: back button, title, city · time. */
@Composable
fun AtmosScreenHeader(
    title: String,
    locationName: String,
    timeText: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AtmosColors.Card)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            ChevronGlyph(size = 18.dp, tint = AtmosColors.TextPrimary, pointingLeft = true)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = title,
            style = AtmosType.CategoryLarge,
            color = AtmosColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (timeText.isBlank()) locationName else "$locationName · $timeText",
            style = AtmosType.Meta,
            color = AtmosColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Small square tile used in the four-up row on Today. */
@Composable
fun AtmosSmallTile(
    glyph: AtmosGlyph,
    value: String,
    unit: String?,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AtmosColors.Card)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        AtmosGlyphIcon(glyph = glyph, size = 20.dp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = AtmosType.Value,
                color = AtmosColors.TextPrimary,
                maxLines = 1
            )
            if (!unit.isNullOrBlank()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = AtmosType.Caption,
                    color = AtmosColors.TextMuted,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            style = AtmosType.Caption,
            color = AtmosColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Square tile used in the 3 x 2 grids on Air and Details. */
@Composable
fun AtmosDetailTile(
    label: String,
    value: String,
    unit: String?,
    caption: String,
    modifier: Modifier = Modifier,
    captionColor: Color = AtmosColors.TextMuted
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AtmosColors.Card)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = AtmosType.Meta,
            color = AtmosColors.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = AtmosType.NumberLarge,
                color = AtmosColors.TextPrimary,
                maxLines = 1
            )
            if (!unit.isNullOrBlank()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = AtmosType.Caption,
                    color = AtmosColors.TextMuted,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            style = AtmosType.Caption,
            color = captionColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Pill used for the AQI health-advice shortcuts on the Air screen. */
@Composable
fun AtmosChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AtmosColors.Card)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            style = AtmosType.Meta,
            color = AtmosColors.TextPrimary,
            maxLines = 2
        )
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Daylight arc.
 *
 * [progress] is the sun's position between sunrise (0) and sunset (1). It is null while
 * the sun is below the horizon; the hollow marker is then parked at [nightDotAtStart]
 * (start = before sunrise, end = after sunset) instead of sitting mid-arc.
 */
@Composable
fun SunArc(
    progress: Float?,
    nightDotAtStart: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 76.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val horizonY = h - 2.dp.toPx()
        val x0 = 2.dp.toPx()
        val x1 = w - 2.dp.toPx()
        val arcHeight = h * 0.64f

        fun pointAt(t: Float): Offset = Offset(
            x = x0 + (x1 - x0) * t,
            y = horizonY - 2f * (1f - t) * t * arcHeight
        )

        // Horizon
        drawLine(
            color = AtmosColors.Divider,
            start = Offset(0f, horizonY),
            end = Offset(w, horizonY),
            strokeWidth = 1.dp.toPx()
        )

        // Dashed sun path
        val arcPath = Path().apply {
            moveTo(pointAt(0f).x, pointAt(0f).y)
            quadraticTo((x0 + x1) / 2f, horizonY - 2f * arcHeight, pointAt(1f).x, pointAt(1f).y)
        }
        drawPath(
            path = arcPath,
            color = AtmosColors.TextMuted.copy(alpha = 0.75f),
            style = Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(5.dp.toPx(), 5.dp.toPx())
                )
            )
        )

        if (progress != null) {
            val sun = pointAt(progress.coerceIn(0f, 1f))
            drawCircle(color = AtmosColors.SunDot, radius = 5.dp.toPx(), center = sun)
        } else {
            val parked = pointAt(if (nightDotAtStart) 0f else 1f)
            drawCircle(
                color = AtmosColors.TextMuted,
                radius = 5.dp.toPx(),
                center = parked,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

/** Compact moon disc: outline plus the illuminated portion for the current phase. */
@Composable
fun MoonPhaseGlyph(
    phase: MoonPhaseType,
    illuminationPercent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f - 1.5.dp.toPx()
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val lit = Color(0xFFEDE3B8)

        val isWaxing = phase == MoonPhaseType.WAXING_CRESCENT ||
            phase == MoonPhaseType.FIRST_QUARTER ||
            phase == MoonPhaseType.WAXING_GIBBOUS ||
            phase == MoonPhaseType.FULL_MOON

        val fraction = (illuminationPercent / 100f).coerceIn(0f, 1f)

        if (fraction > 0.02f) {
            if (fraction > 0.98f) {
                drawCircle(color = lit, radius = radius, center = center)
            } else {
                drawArc(
                    color = lit,
                    startAngle = if (isWaxing) -90f else 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f)
                )
                if (fraction > 0.5f) {
                    val bulge = radius * 2f * ((fraction - 0.5f) * 2f)
                    drawOval(
                        color = lit,
                        topLeft = Offset(center.x - bulge / 2f, center.y - radius),
                        size = Size(bulge, radius * 2f)
                    )
                } else {
                    val mask = radius * 2f * ((0.5f - fraction) * 2f)
                    drawOval(
                        color = AtmosColors.Card,
                        topLeft = Offset(center.x - mask / 2f, center.y - radius),
                        size = Size(mask, radius * 2f)
                    )
                }
            }
        }

        drawCircle(
            color = AtmosColors.TextPrimary.copy(alpha = 0.55f),
            radius = radius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

/** Full-bleed empty state shared by all three tabs. */
@Composable
fun AtmosEmptyState(
    title: String,
    message: String,
    onAction: () -> Unit,
    actionLabel: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = AtmosType.Condition,
                color = AtmosColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = AtmosType.Meta,
                color = AtmosColors.TextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(AtmosColors.Card)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = AtmosType.Value.copy(fontSize = 15.sp),
                    color = AtmosColors.Accent
                )
            }
        }
    }
}
