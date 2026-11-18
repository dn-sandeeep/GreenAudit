package com.sandeep.greenaudit.data.repository

import com.sandeep.greenaudit.data.local.dao.ParkDao
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.ParkStatus
import com.sandeep.greenaudit.data.local.entity.ParkType
import com.sandeep.greenaudit.data.local.entity.SyncState
import com.sandeep.greenaudit.data.local.seed.SeedDataProvider
import com.sandeep.greenaudit.domain.gis.GeoMath
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.repository.ParkRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultParkRepository @Inject constructor(
    private val parkDao: ParkDao,
) : ParkRepository {

    override fun observeParks(): Flow<List<ParkEntity>> = parkDao.observeAll()

    override fun observePark(id: String): Flow<ParkEntity?> = parkDao.observeById(id)

    override suspend fun getPark(id: String): ParkEntity? = parkDao.getById(id)

    override suspend fun seedIfNeeded() {
        SeedDataProvider.seedIfEmpty(parkDao)
    }

    override suspend fun saveNewPark(name: String, type: ParkType, boundary: List<GeoPoint>): ParkEntity {
        val now = System.currentTimeMillis()
        val park = ParkEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            type = type,
            boundary = boundary,
            areaSqMeters = GeoMath.computeAreaSqMeters(boundary),
            perimeterMeters = GeoMath.computePerimeterMeters(boundary),
            status = ParkStatus.OPEN,
            lastSurveyDateMillis = null,
            syncState = SyncState.PENDING_CREATE,
            updatedAtMillis = now,
        )
        parkDao.upsert(park)
        return park
    }

    override suspend fun updateStatus(parkId: String, status: ParkStatus, nowMillis: Long) {
        val park = parkDao.getById(parkId) ?: return
        parkDao.update(
            park.copy(
                status = status,
                syncState = nextSyncState(park.syncState),
                updatedAtMillis = nowMillis,
            ),
        )
    }

    override suspend fun markSurveyed(parkId: String, surveyedAtMillis: Long) {
        val park = parkDao.getById(parkId) ?: return
        parkDao.update(
            park.copy(
                lastSurveyDateMillis = surveyedAtMillis,
                syncState = nextSyncState(park.syncState),
                updatedAtMillis = surveyedAtMillis,
            ),
        )
    }

    /** A row already pending a create keeps pending-create; anything synced moves to pending-update. */
    private fun nextSyncState(current: SyncState): SyncState =
        if (current == SyncState.PENDING_CREATE) SyncState.PENDING_CREATE else SyncState.PENDING_UPDATE
}
