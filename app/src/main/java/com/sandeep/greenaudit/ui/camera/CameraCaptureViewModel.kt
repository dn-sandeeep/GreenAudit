package com.sandeep.greenaudit.ui.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandeep.greenaudit.data.location.LocationTracker
import com.sandeep.greenaudit.data.photo.PhotoStampUtil
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.repository.ParkRepository
import com.sandeep.greenaudit.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CameraCaptureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val parkRepository: ParkRepository,
    private val photoRepository: PhotoRepository,
    private val locationTracker: LocationTracker,
) : ViewModel() {

    val parkId: String = checkNotNull(savedStateHandle["parkId"])
    val surveyId: String = checkNotNull(savedStateHandle["surveyId"])

    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onPhotoCaptured(file: File, onSaved: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            errorMessage = null
            val location = locationTracker.lastKnownLocation()
            if (location == null) {
                errorMessage = "Couldn't get a location fix - try again in open sky"
                file.delete()
                isSaving = false
                return@launch
            }
            val parkName = parkRepository.getPark(parkId)?.name ?: "Unknown park"
            val now = System.currentTimeMillis()
            PhotoStampUtil.stamp(
                file = file,
                parkName = parkName,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                timestampMillis = now,
            )
            photoRepository.addPhoto(
                surveyId = surveyId,
                filePath = file.absolutePath,
                location = GeoPoint(location.latitude, location.longitude),
                accuracyMeters = location.accuracy,
                nowMillis = now,
            )
            isSaving = false
            onSaved()
        }
    }
}
