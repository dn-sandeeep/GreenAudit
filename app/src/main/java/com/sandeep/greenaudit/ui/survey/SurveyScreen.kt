package com.sandeep.greenaudit.ui.survey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil3.compose.AsyncImage
import com.sandeep.greenaudit.data.local.entity.Cleanliness
import com.sandeep.greenaudit.domain.survey.CheckInEligibility
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyScreen(
    onBack: () -> Unit,
    onAddPhoto: (surveyId: String) -> Unit,
    onSubmitted: () -> Unit,
    viewModel: SurveyViewModel = hiltViewModel(),
) {
    val park by viewModel.park.collectAsState()
    val survey by viewModel.survey.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startLocationUpdates()
                Lifecycle.Event.ON_STOP -> viewModel.stopLocationUpdates()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.startLocationUpdates()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopLocationUpdates()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(park?.name ?: "Survey") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val currentSurvey = survey
        if (currentSurvey == null) {
            CheckInGate(
                eligibility = viewModel.eligibility(),
                userLocationKnown = userLocation != null,
                onCheckIn = viewModel::checkIn,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                CheckedInBadge(checkInAtMillis = currentSurvey.checkInAtMillis, accuracyMeters = currentSurvey.checkInAccuracyMeters)

                Text(
                    text = "Cleanliness",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cleanliness.entries.forEach { option ->
                        FilterChip(
                            selected = viewModel.cleanliness == option,
                            onClick = { viewModel.onCleanlinessChange(option) },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }

                Text(
                    text = "Photos (${photos.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos, key = { it.id }) { photo ->
                        AsyncImage(
                            model = File(photo.filePath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                    item {
                        Surface(
                            onClick = { onAddPhoto(currentSurvey.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add photo")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Trees counted", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { viewModel.onTreesCountedChange(viewModel.treesCounted - 1) }) {
                            Text("−")
                        }
                        Text(
                            text = "${viewModel.treesCounted}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        OutlinedButton(onClick = { viewModel.onTreesCountedChange(viewModel.treesCounted + 1) }) {
                            Text("+")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Lights working", style = MaterialTheme.typography.titleSmall)
                    Switch(checked = viewModel.lightsWorking, onCheckedChange = viewModel::onLightsWorkingChange)
                }

                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text("Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )

                Button(
                    onClick = { viewModel.submit() },
                    enabled = !viewModel.isSubmitted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text(if (viewModel.isSubmitted) "Survey submitted" else "Submit survey")
                }

                if (viewModel.isSubmitted) {
                    DisposableEffect(Unit) {
                        onSubmitted()
                        onDispose {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckedInBadge(checkInAtMillis: Long, accuracyMeters: Float) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(checkInAtMillis))
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = "Checked in at $time · ±${accuracyMeters.toInt()} m",
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun CheckInGate(
    eligibility: CheckInEligibility?,
    userLocationKnown: Boolean,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Card {
            Column(modifier = Modifier.padding(20.dp)) {
                val message = when {
                    !userLocationKnown -> "Getting your location…"
                    eligibility is CheckInEligibility.Eligible -> "You're inside the park. Ready to check in."
                    eligibility is CheckInEligibility.Ineligible -> when (val reason = eligibility.reason) {
                        is CheckInEligibility.Reason.OutsideBoundary ->
                            "You're %,.0f m outside the park boundary — walk closer to start a survey.".format(reason.distanceMeters)
                        is CheckInEligibility.Reason.WeakGps ->
                            "GPS signal is too weak (±%.0f m, need ±25 m or better).".format(reason.accuracyMeters)
                    }
                    else -> "Getting your location…"
                }
                Text(text = message, style = MaterialTheme.typography.bodyLarge)

                if (!userLocationKnown) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
                } else {
                    Button(
                        onClick = onCheckIn,
                        enabled = eligibility is CheckInEligibility.Eligible,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text("Check in")
                    }
                }
            }
        }
    }
}
