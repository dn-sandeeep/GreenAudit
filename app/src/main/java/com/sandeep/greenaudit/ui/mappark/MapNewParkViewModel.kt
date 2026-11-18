package com.sandeep.greenaudit.ui.mappark

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandeep.greenaudit.data.local.entity.ParkType
import com.sandeep.greenaudit.domain.gis.GeoMath
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.repository.ParkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MapNewParkViewModel @Inject constructor(
    private val parkRepository: ParkRepository,
) : ViewModel() {

    val corners = mutableStateListOf<GeoPoint>()

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
