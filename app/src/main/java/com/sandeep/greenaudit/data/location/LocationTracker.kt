package com.sandeep.greenaudit.data.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Wraps the Fused Location Provider as a cold [Flow]. Callers are
 * responsible for only collecting this while the relevant screen is
 * visible and the location permission is granted - the Flow itself has no
 * opinion about either, it just starts updates on collection and removes
 * them via [awaitClose] when the collector is cancelled, which is what
 * gives us "foreground only, stopped when the screen is not visible".
 */
class LocationTracker @Inject constructor(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
) {
    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMillis: Long = 3_000L): Flow<Location> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { fusedLocationProviderClient.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    suspend fun lastKnownLocation(): Location? = fusedLocationProviderClient.lastLocation.await()
}
