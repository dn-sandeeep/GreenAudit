package com.sandeep.greenaudit.ui.parks

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sandeep.greenaudit.domain.gis.GeoPoint

private fun hasLocationPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParksScreen(
    onAddPark: () -> Unit,
    onStartSurvey: (parkId: String) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ParksViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val parks by viewModel.filteredParks.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var permissionPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasPermission = results.values.any { it }
        if (!hasPermission) {
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            } ?: false
            permissionPermanentlyDenied = !shouldShowRationale
        }
    }

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
        if (hasPermission) viewModel.startLocationUpdates()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopLocationUpdates()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parks") },
                actions = {
                    IconButton(onClick = { viewModel.syncNow() }, enabled = !isSyncing) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync now")
                        }
                    }
                    IconButton(onClick = { viewModel.logout(onLoggedOut) }) {
                        Icon(Icons.Default.Logout, contentDescription = "Log out")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPark) {
                Icon(Icons.Default.Add, contentDescription = "Map a new park")
            }
        },
        bottomBar = {
            TabRow(
                selectedTabIndex = viewModel.selectedTab.ordinal,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Tab(
                    selected = viewModel.selectedTab == ParksTab.LIST,
                    onClick = { viewModel.onTabSelected(ParksTab.LIST) },
                    text = { Text("List") },
                )
                Tab(
                    selected = viewModel.selectedTab == ParksTab.MAP,
                    onClick = { viewModel.onTabSelected(ParksTab.MAP) },
                    text = { Text("Map") },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (!hasPermission) {
                LocationPermissionBanner(
                    permanentlyDenied = permissionPermanentlyDenied,
                    onRequest = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (viewModel.selectedTab) {
                    ParksTab.LIST -> ParkListContent(
                        parks = parks,
                        searchQuery = viewModel.currentSearchQuery,
                        statusFilter = viewModel.currentStatusFilter,
                        onSearchQueryChange = viewModel::onSearchQueryChange,
                        onStatusFilterChange = viewModel::onStatusFilterChange,
                        onParkClick = { viewModel.onParkSelected(it.id) },
                    )
                    ParksTab.MAP -> ParksMapContent(
                        parks = parks,
                        userLocation = userLocation,
                        selectedParkId = viewModel.selectedParkId,
                        onParkTapped = { viewModel.onParkSelected(it) },
                    )
                }
            }
        }
    }

    val selectedPark = viewModel.selectedParkId?.let { id -> parks.find { it.id == id } }
    if (selectedPark != null) {
        val eligibility = viewModel.checkInEligibility(selectedPark)
        val userGeoPoint = userLocation?.let { GeoPoint(it.latitude, it.longitude) }
        ParkDetailBottomSheet(
            park = selectedPark,
            userLocation = userGeoPoint,
            eligibility = eligibility,
            onDismiss = { viewModel.onParkSelected(null) },
            onStartSurvey = { onStartSurvey(selectedPark.id) },
        )
    }
}

@Composable
private fun LocationPermissionBanner(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "GreenAudit needs your location to show it on the map and to check you're inside a park before a survey.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = if (permanentlyDenied) onOpenSettings else onRequest,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(if (permanentlyDenied) "Open settings" else "Allow location")
            }
        }
    }
}
