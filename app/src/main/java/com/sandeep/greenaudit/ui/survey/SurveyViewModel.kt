package com.sandeep.greenaudit.ui.survey

import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandeep.greenaudit.data.local.entity.Cleanliness
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.PhotoEntity
import com.sandeep.greenaudit.data.local.entity.SurveyEntity
import com.sandeep.greenaudit.data.location.LocationTracker
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.repository.ParkRepository
import com.sandeep.greenaudit.domain.repository.PhotoRepository
import com.sandeep.greenaudit.domain.repository.SurveyRepository
import com.sandeep.greenaudit.domain.survey.CheckInEligibility
import com.sandeep.greenaudit.domain.survey.CheckInEligibilityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SurveyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val parkRepository: ParkRepository,
    private val surveyRepository: SurveyRepository,
    private val photoRepository: PhotoRepository,
    private val locationTracker: LocationTracker,
) : ViewModel() {

    private val checkInEligibilityUseCase = CheckInEligibilityUseCase()

    val parkId: String = checkNotNull(savedStateHandle["parkId"])

    val park: StateFlow<ParkEntity?> = parkRepository.observePark(parkId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    private val _survey = MutableStateFlow<SurveyEntity?>(null)
    val survey: StateFlow<SurveyEntity?> = _survey.asStateFlow()

    var treesCounted by mutableStateOf(0)
        private set
    var lightsWorking by mutableStateOf(true)
        private set
    var cleanliness by mutableStateOf(Cleanliness.GOOD)
        private set
    var notes by mutableStateOf("")
        private set
    var isSubmitted by mutableStateOf(false)
        private set

    val photos: StateFlow<List<PhotoEntity>> = _survey
        .flatMapLatest { survey -> survey?.let { photoRepository.observeForSurvey(it.id) } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun eligibility(): CheckInEligibility? {
        val location = _userLocation.value ?: return null
        val boundary = park.value?.boundary ?: return null
        return checkInEligibilityUseCase(
            userLocation = GeoPoint(location.latitude, location.longitude),
            gpsAccuracyMeters = location.accuracy,
            parkBoundary = boundary,
        )
    }

    fun checkIn() {
        val location = _userLocation.value ?: return
        if (eligibility() !is CheckInEligibility.Eligible) return
        viewModelScope.launch {
            _survey.value = surveyRepository.checkIn(
                parkId = parkId,
                location = GeoPoint(location.latitude, location.longitude),
                accuracyMeters = location.accuracy,
                nowMillis = System.currentTimeMillis(),
            )
        }
    }

    fun onTreesCountedChange(value: Int) {
        treesCounted = value.coerceAtLeast(0)
    }

    fun onLightsWorkingChange(value: Boolean) {
        lightsWorking = value
    }

    fun onCleanlinessChange(value: Cleanliness) {
        cleanliness = value
    }

    fun onNotesChange(value: String) {
        notes = value
    }

    fun submit() {
        val currentSurvey = _survey.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            surveyRepository.submit(
                surveyId = currentSurvey.id,
                treesCounted = treesCounted,
                lightsWorking = lightsWorking,
                cleanliness = cleanliness,
                notes = notes,
                nowMillis = now,
            )
            parkRepository.markSurveyed(parkId, now)
            isSubmitted = true
        }
    }
}
