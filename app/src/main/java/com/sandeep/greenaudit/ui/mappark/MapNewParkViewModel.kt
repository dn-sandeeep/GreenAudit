package com.sandeep.greenaudit.ui.mappark

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandeep.greenaudit.data.local.entity.ParkType
import com.sandeep.greenaudit.data.location.LocationTracker
import com.sandeep.greenaudit.domain.gis.GeoMath
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.repository.ParkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MapNewParkViewModel @Inject constructor(
    private val parkRepository: ParkRepository,
    private val locationTracker: LocationTracker,
) : ViewModel() {

    val corners = mutableStateListOf<GeoPoint>()

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private var locationJob: Job? = null

    fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            locationTracker.locationUpdates().collect { location -> _userLocation.value = location }
        }
    }

    fun stopLocationUpdates() {
        locationJob?.cancel()
        locationJob = null
    }

    var parkName by mutableStateOf("")
        private set
    var parkType by mutableStateOf(ParkType.GARDEN)
        private set
    var isSaving by mutableStateOf(false)
        private set

    val areaSqMeters: Double get() = if (corners.size >= 3) GeoMath.computeAreaSqMeters(corners) else 0.0
    val perimeterMeters: Double get() = if (corners.size >= 2) GeoMath.computePerimeterMeters(corners) else 0.0

    val canSave: Boolean get() = corners.size >= 3 && parkName.isNotBlank() && !isSaving

    fun onMapTapped(point: GeoPoint) {
        corners.add(point)
    }

    fun onCornerDragged(index: Int, newPosition: GeoPoint) {
        if (index in corners.indices) corners[index] = newPosition
    }

    fun undoLastCorner() {
        if (corners.isNotEmpty()) corners.removeAt(corners.lastIndex)
    }

    fun onNameChange(value: String) {
        parkName = value
    }

    fun onTypeChange(value: ParkType) {
        parkType = value
    }

    fun save(onSaved: () -> Unit) {
        if (!canSave) return
        viewModelScope.launch {
            isSaving = true
            parkRepository.saveNewPark(parkName.trim(), parkType, corners.toList())
            isSaving = false
            onSaved()
        }
    }
}
