package com.sandeep.greenaudit.ui.parks

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.ParkStatus
import com.sandeep.greenaudit.data.location.LocationTracker
import com.sandeep.greenaudit.data.sync.SyncManager
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.repository.AuthRepository
import com.sandeep.greenaudit.domain.repository.ParkRepository
import com.sandeep.greenaudit.domain.survey.CheckInEligibility
import com.sandeep.greenaudit.domain.survey.CheckInEligibilityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ParksTab { LIST, MAP }

@HiltViewModel
class ParksViewModel @Inject constructor(
    private val parkRepository: ParkRepository,
    private val authRepository: AuthRepository,
    private val locationTracker: LocationTracker,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val checkInEligibilityUseCase = CheckInEligibilityUseCase()

    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<ParkStatus?>(null)

    val filteredParks: StateFlow<List<ParkEntity>> = combine(
        parkRepository.observeParks(),
        searchQuery,
        statusFilter,
    ) { parks, query, status ->
        parks
            .filter { status == null || it.status == status }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing

    var selectedTab by mutableStateOf(ParksTab.LIST)
        private set
    var selectedParkId by mutableStateOf<String?>(null)
        private set
    var currentSearchQuery by mutableStateOf("")
        private set
    var currentStatusFilter by mutableStateOf<ParkStatus?>(null)
        private set

    private var locationJob: Job? = null

    fun onTabSelected(tab: ParksTab) {
        selectedTab = tab
    }

    fun onSearchQueryChange(query: String) {
        currentSearchQuery = query
        searchQuery.value = query
    }

    fun onStatusFilterChange(status: ParkStatus?) {
        currentStatusFilter = status
        statusFilter.value = status
    }

    fun onParkSelected(parkId: String?) {
        selectedParkId = parkId
    }

    /** Call when the screen becomes visible and the location permission is granted. */
    fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            locationTracker.locationUpdates().collect { location -> _userLocation.value = location }
        }
    }

    /** Call when the screen is no longer visible - satisfies "foreground only" location use. */
    fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
    }

    fun checkInEligibility(park: ParkEntity): CheckInEligibility? {
        val location = _userLocation.value ?: return null
        return checkInEligibilityUseCase(
            userLocation = GeoPoint(location.latitude, location.longitude),
            gpsAccuracyMeters = location.accuracy,
            parkBoundary = park.boundary,
        )
    }

    fun syncNow() {
        viewModelScope.launch { syncManager.syncNow() }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
