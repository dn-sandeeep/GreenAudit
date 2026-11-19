package com.sandeep.greenaudit.ui.parks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.domain.gis.AreaUnits
import com.sandeep.greenaudit.domain.gis.GeoMath
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.survey.CheckInEligibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkDetailBottomSheet(
    park: ParkEntity,
    userLocation: GeoPoint?,
    eligibility: CheckInEligibility?,
    hasLocationPermission: Boolean,
    onDismiss: () -> Unit,
    onStartSurvey: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(text = park.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = "%,.0f m² (%.2f acres)".format(park.areaSqMeters, AreaUnits.squareMetersToAcres(park.areaSqMeters)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (userLocation != null) {
                val centroid = park.boundary.centroid()
                val distance = GeoMath.haversineDistanceMeters(userLocation, centroid)
                val bearing = GeoMath.initialBearingDegrees(userLocation, centroid)
                Text(
                    text = "%,.0f m, %s".format(distance, GeoMath.bearingToCompass(bearing)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            val reasonText = when {
                !hasLocationPermission -> "Allow location to see distance and check in."
                eligibility == null -> "Waiting for your location…"
                eligibility is CheckInEligibility.Eligible -> null
                eligibility is CheckInEligibility.Ineligible -> when (val reason = eligibility.reason) {
                    is CheckInEligibility.Reason.OutsideBoundary ->
                        "Go inside to start survey — %,.0f m away".format(reason.distanceMeters)
                    is CheckInEligibility.Reason.WeakGps ->
                        "GPS signal too weak (±%.0f m) — move to open sky".format(reason.accuracyMeters)
                }
                else -> null
            }
            if (reasonText != null) {
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Button(
                onClick = onStartSurvey,
                enabled = eligibility is CheckInEligibility.Eligible,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 24.dp),
            ) {
                Text("Go inside to start survey")
            }
        }
    }
}

private fun List<GeoPoint>.centroid(): GeoPoint {
    val lat = sumOf { it.latitude } / size
    val lon = sumOf { it.longitude } / size
    return GeoPoint(lat, lon)
}
