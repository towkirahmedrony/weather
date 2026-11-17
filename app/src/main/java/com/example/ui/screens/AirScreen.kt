package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AirQualityData
import com.example.domain.model.AqiCategory
import com.example.domain.util.UnitFormatter
import com.example.ui.components.AqiScaleBar
import com.example.ui.components.AtmosChip
import com.example.ui.components.AtmosScreenHeader
import com.example.ui.components.AtmosSectionLabel
import com.example.ui.theme.AtmosColors
import com.example.ui.theme.AtmosType
import java.util.Locale

/**
 * Screen 2 - "Air". Oversized category-coloured US AQI number, the shared scale bar
 * and a 3 x 2 grid of pollutant tiles.
 */
@Composable
fun AirContent(
    airQuality: AirQualityData,
    locationName: String,
    timeText: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val category = airQuality.category
    val categoryColor = AtmosColors.aqiColor(category)

    Column(modifier = modifier.fillMaxWidth()) {
        AtmosScreenHeader(
            title = stringResource(R.string.title_air_quality),
            locationName = locationName,
            timeText = timeText,
            onBack = onBack
        )

        Spacer(Modifier.height(20.dp))

        AtmosSectionLabel(
            text = stringResource(R.string.aqi_us_label),
            color = AtmosColors.TextMuted
        )

        Text(
            text = airQuality.usAqi.toString(),
            style = AtmosType.AqiNumber,
            color = categoryColor,
            maxLines = 1
        )

        Text(
            text = UnitFormatter.aqiCategoryLabel(category, context),
            style = AtmosType.CategoryLarge,
            color = AtmosColors.TextPrimary,
            maxLines = 2
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = UnitFormatter.aqiAdvice(category, context),
            style = AtmosType.Body,
            color = AtmosColors.TextMuted
        )

        Spacer(Modifier.height(18.dp))

        AqiScaleBar(usAqi = airQuality.usAqi)

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.aqi_good),
                style = AtmosType.Meta,
                color = AtmosColors.TextMuted
            )
            Text(
                text = stringResource(R.string.aqi_unhealthy),
                style = AtmosType.Meta,
                color = AtmosColors.TextMuted
            )
            Text(
                text = stringResource(R.string.aqi_hazardous),
                style = AtmosType.Meta,
                color = AtmosColors.TextMuted
            )
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PollutantTile(
                label = stringResource(R.string.aqi_pm25),
                value = "%.1f".format(Locale.US, airQuality.pm25),
                modifier = Modifier.weight(1f)
            )
            PollutantTile(
                label = stringResource(R.string.aqi_pm10),
                value = "%.1f".format(Locale.US, airQuality.pm10),
                modifier = Modifier.weight(1f)
            )
            PollutantTile(
                label = stringResource(R.string.aqi_o3),
                value = "%.0f".format(Locale.US, airQuality.ozone),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PollutantTile(
                label = stringResource(R.string.aqi_no2),
                value = "%.1f".format(Locale.US, airQuality.nitrogenDioxide),
                modifier = Modifier.weight(1f)
            )
            PollutantTile(
                label = stringResource(R.string.aqi_so2),
                value = "%.1f".format(Locale.US, airQuality.sulphurDioxide),
                modifier = Modifier.weight(1f)
            )
            PollutantTile(
                label = stringResource(R.string.aqi_co),
                value = "%.0f".format(Locale.US, airQuality.carbonMonoxide),
                modifier = Modifier.weight(1f)
            )
        }

        if (category.ordinal >= AqiCategory.UNHEALTHY.ordinal) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AtmosChip(
                    text = stringResource(R.string.aqi_chip_windows),
                    modifier = Modifier.weight(1f)
                )
                AtmosChip(
                    text = stringResource(R.string.aqi_chip_outdoor),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PollutantTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
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
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = AtmosType.NumberLarge,
            color = AtmosColors.TextPrimary,
            maxLines = 1
        )
        Text(
            text = stringResource(R.string.unit_ugm3),
            style = AtmosType.Caption,
            color = AtmosColors.TextMuted,
            maxLines = 1
        )
    }
}
