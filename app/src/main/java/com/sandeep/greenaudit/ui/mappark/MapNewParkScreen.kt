package com.sandeep.greenaudit.ui.mappark

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sandeep.greenaudit.data.local.entity.ParkType
import com.sandeep.greenaudit.domain.gis.AreaUnits
import com.sandeep.greenaudit.ui.common.PermissionPlaceholder
import com.sandeep.greenaudit.ui.common.PermissionDeniedMessage
import com.sandeep.greenaudit.ui.common.PermissionRationaleDialog
import com.sandeep.greenaudit.ui.common.hasLocationPermission
import com.sandeep.greenaudit.ui.common.isLocationPermissionPermanentlyDenied
import com.sandeep.greenaudit.ui.common.markLocationPermissionRequested
import com.sandeep.greenaudit.ui.common.resetLocationPermissionRequestCountIfGranted
import com.sandeep.greenaudit.ui.parks.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapNewParkScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MapNewParkViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val userLocation by viewModel.userLocation.collectAsState()
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var permissionPermanentlyDenied by remember { mutableStateOf(isLocationPermissionPermanentlyDenied(context)) }
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasPermission = results.values.any { it }
        if (!hasPermission) permissionPermanentlyDenied = isLocationPermissionPermanentlyDenied(context)
    }

    // A granted permission clears any prior denial streak, so a later Settings
    // revoke doesn't get stuck looking "permanently denied" from old history.
    LaunchedEffect(hasPermission) { resetLocationPermissionRequestCountIfGranted(context) }

    // Entering this screen (from the "+" FAB) is the first use of the mapping
    // feature - explain why, then ask, matching the Parks screen's Map tab.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasPermission) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (hasPermission) viewModel.startLocationUpdates()
                Lifecycle.Event.ON_STOP -> viewModel.stopLocationUpdates()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (hasPermission) {
            viewModel.startLocationUpdates()
        } else if (!permissionPermanentlyDenied) {
            showRationale = true
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopLocationUpdates()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Map a new park") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        // Permanently denied still shows the drawing UI (with the small
        // fallback message below) - a retry button there couldn't do
        // anything a Settings link doesn't already cover, same as the Map tab.
        if (hasPermission || permissionPermanentlyDenied) {
            Column(modifier = Modifier.padding(innerPadding)) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    MapDrawingView(
                        corners = viewModel.corners,
                        userLocation = userLocation,
                        onMapTapped = viewModel::onMapTapped,
                        onCornerDragged = viewModel::onCornerDragged,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Declared after MapDrawingView so it draws above the
                    // AndroidView-hosted osmdroid map - a sibling placed before
                    // an AndroidView in a plain Column can get painted over.
                    if (permissionPermanentlyDenied) {
                        PermissionDeniedMessage(
                            message = "Location is off - the map won't center on you.",
                            onOpenSettings = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (viewModel.corners.isEmpty()) "Tap to add corners" else "${viewModel.corners.size} corners",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Area: %,.0f m² (%.2f acres)".format(
                            viewModel.areaSqMeters,
                            AreaUnits.squareMetersToAcres(viewModel.areaSqMeters),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Perimeter: %,.0f m".format(viewModel.perimeterMeters),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Drag a corner to adjust",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    OutlinedTextField(
                        value = viewModel.parkName,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Park name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )

                    ParkTypeDropdown(
                        selected = viewModel.parkType,
                        onSelected = viewModel::onTypeChange,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = viewModel::undoLastCorner,
                            enabled = viewModel.corners.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Undo")
                        }
                        Button(
                            onClick = { viewModel.save(onSaved) },
                            enabled = viewModel.canSave,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Save park")
                        }
                    }
                }
            }
        } else {
            // Not granted yet - including right after the dialog was
            // cancelled with the back button, which Android reports as a
            // denial just like tapping "Deny". Don't render the map/form
            // until permission is actually resolved.
            PermissionPlaceholder(
                message = "Allow location to draw a park boundary centered on where you're standing.",
                buttonLabel = "Allow location",
                onAllowClick = { showRationale = true },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            title = "Location access",
            message = "GreenAudit uses your location to center the map on where you're standing, so you can draw a park boundary around yourself.",
            onAllow = {
                showRationale = false
                markLocationPermissionRequested(context)
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
            onDismiss = { showRationale = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParkTypeDropdown(selected: ParkType, onSelected: (ParkType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ParkType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.label()) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}
