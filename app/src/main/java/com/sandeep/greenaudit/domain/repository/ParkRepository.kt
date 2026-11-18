package com.sandeep.greenaudit.domain.repository

import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.ParkStatus
import com.sandeep.greenaudit.data.local.entity.ParkType
import com.sandeep.greenaudit.domain.gis.GeoPoint
import kotlinx.coroutines.flow.Flow

/**
 * Parks are local-first: every read comes from Room, every write is saved
 * locally first and flagged for [com.sandeep.greenaudit.data.sync.SyncManager]
 * to push later. There is no network round-trip on the write path.
 */
interface ParkRepository {

    fun observeParks(): Flow<List<ParkEntity>>

    fun observePark(id: String): Flow<ParkEntity?>

    suspend fun getPark(id: String): ParkEntity?

    /** Seeds demo parks on first run only; a no-op once the table has rows. */
    suspend fun seedIfNeeded()

    /** Saves a newly-drawn park; computes area/perimeter from [boundary] via GeoMath. */
    suspend fun saveNewPark(name: String, type: ParkType, boundary: List<GeoPoint>): ParkEntity

    suspend fun updateStatus(parkId: String, status: ParkStatus, nowMillis: Long)

    suspend fun markSurveyed(parkId: String, surveyedAtMillis: Long)
}
