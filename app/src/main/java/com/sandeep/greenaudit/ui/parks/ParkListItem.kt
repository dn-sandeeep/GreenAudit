package com.sandeep.greenaudit.ui.parks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.SyncState
import com.sandeep.greenaudit.domain.gis.AreaUnits
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParkListItem(park: ParkEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = park.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (park.syncState != SyncState.SYNCED) {
                        PendingSyncBadge()
                    }
                    Surface(
                        shape = CircleShape,
                        color = park.status.toColor(),
                        modifier = Modifier.size(10.dp),
                    ) {}
                }
            }
            Text(
                text = "${park.type.label()} · ${formatArea(park.areaSqMeters)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = surveyLine(park),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PendingSyncBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = "Pending sync",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

private fun formatArea(areaSqMeters: Double): String {
    val acres = AreaUnits.squareMetersToAcres(areaSqMeters)
    return "%,.0f m² (%.2f acres)".format(areaSqMeters, acres)
}

private fun surveyLine(park: ParkEntity): String {
    val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    return when {
        park.status == com.sandeep.greenaudit.data.local.entity.ParkStatus.CLOSED && park.lastSurveyDateMillis != null ->
            "Closed since ${dateFormat.format(Date(park.lastSurveyDateMillis))}"
        park.lastSurveyDateMillis != null -> "Surveyed ${dateFormat.format(Date(park.lastSurveyDateMillis))}"
        else -> "Not yet surveyed"
    }
}
