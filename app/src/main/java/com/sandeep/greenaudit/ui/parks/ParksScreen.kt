package com.sandeep.greenaudit.ui.parks

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sandeep.greenaudit.domain.gis.GeoPoint
import androidx.compose.runtime.LaunchedEffect
import com.sandeep.greenaudit.ui.common.PermissionPlaceholder
import com.sandeep.greenaudit.ui.common.PermissionDeniedMessage
import com.sandeep.greenaudit.ui.common.hasLocationPermission
import com.sandeep.greenaudit.ui.common.isLocationPermissionPermanentlyDenied
import com.sandeep.greenaudit.ui.common.markLocationPermissionRequested
import com.sandeep.greenaudit.ui.common.resetLocationPermissionRequestCountIfGranted

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
    val selectedPark = viewModel.selectedParkId?.let { id -> parks.find { it.id == id } }

    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var permissionPermanentlyDenied by remember { mutableStateOf(isLocationPermissionPermanentlyDenied(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        hasPermission = results.values.any { it }
        if (!hasPermission) permissionPermanentlyDenied = isLocationPermissionPermanentlyDenied(context)
    }

    // A granted permission clears any prior denial streak, so a later Settings
    // revoke doesn't get stuck looking "permanently denied" from old history.
    LaunchedEffect(hasPermission) { resetLocationPermissionRequestCountIfGranted(context) }

    fun requestLocationPermission() {
        markLocationPermissionRequested(context)
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    // Location is needed on the Map tab (live position, my-location button) AND
    // whenever a park's bottom sheet is open (List or Map - it shows distance/
    // bearing and the check-in eligibility reason), but not on a bare List tab.
    val onMapTab = viewModel.selectedTab == ParksTab.MAP
    val needsLocation = onMapTab || selectedPark != null

    // Tapping a park row/polygon is the first use of the "see distance and
    // check in" feature if the Map tab was never opened - ask directly here too.
    LaunchedEffect(selectedPark, hasPermission, permissionPermanentlyDenied) {
        if (selectedPark != null && !hasPermission && !permissionPermanentlyDenied) requestLocationPermission()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasPermission, needsLocation) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (hasPermission && needsLocation) viewModel.startLocationUpdates()
                Lifecycle.Event.ON_STOP -> viewModel.stopLocationUpdates()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (hasPermission && needsLocation) viewModel.startLocationUpdates() else viewModel.stopLocationUpdates()
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
                    onClick = {
                        viewModel.onTabSelected(ParksTab.MAP)
                        // First use of the map feature - ask directly, no rationale UI first.
                        if (!hasPermission && !permissionPermanentlyDenied) requestLocationPermission()
                    },
                    text = { Text("Map") },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (viewModel.selectedTab) {
                ParksTab.LIST -> ParkListContent(
                    parks = parks,
                    searchQuery = viewModel.currentSearchQuery,
                    statusFilter = viewModel.currentStatusFilter,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onStatusFilterChange = viewModel::onStatusFilterChange,
                    onParkClick = { viewModel.onParkSelected(it.id) },
                )
                ParksTab.MAP -> if (hasPermission || permissionPermanentlyDenied) {
                    // Permanently denied still shows the map (with the small
                    // fallback message below) - a retry button there couldn't
                    // do anything a Settings link doesn't already cover.
                    ParksMapContent(
                        parks = parks,
                        userLocation = userLocation,
                        selectedParkId = viewModel.selectedParkId,
                        onParkTapped = { viewModel.onParkSelected(it) },
                    )
                } else {
                    // Not granted yet - including right after the dialog was
                    // cancelled with the back button, which Android reports as
                    // a denial just like tapping "Deny". Don't render the map
                    // until permission is actually resolved.
                    PermissionPlaceholder(
                        message = "Allow location to see the map and your position in it.",
                        buttonLabel = "Allow location",
                        onAllowClick = ::requestLocationPermission,
                    )
                }
            }

            // Declared after the map/list content so it draws above it -
            // AndroidView-hosted content (the osmdroid map) can otherwise
            // paint over a sibling placed before it in a plain Column.
            if (onMapTab && permissionPermanentlyDenied) {
                PermissionDeniedMessage(
                    message = "Location is off.",
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }

    if (selectedPark != null) {
        val eligibility = viewModel.checkInEligibility(selectedPark)
        val userGeoPoint = userLocation?.let { GeoPoint(it.latitude, it.longitude) }
        ParkDetailBottomSheet(
            park = selectedPark,
            userLocation = userGeoPoint,
            eligibility = eligibility,
            hasLocationPermission = hasPermission,
            onDismiss = { viewModel.onParkSelected(null) },
            onStartSurvey = { onStartSurvey(selectedPark.id) },
        )
    }
}
