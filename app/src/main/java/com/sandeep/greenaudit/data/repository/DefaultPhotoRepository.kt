package com.sandeep.greenaudit.data.repository

import com.sandeep.greenaudit.data.local.dao.PhotoDao
import com.sandeep.greenaudit.data.local.entity.PhotoEntity
import com.sandeep.greenaudit.data.local.entity.SyncState
import com.sandeep.greenaudit.data.sync.BackgroundSyncScheduler
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.repository.PhotoRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultPhotoRepository @Inject constructor(
    private val photoDao: PhotoDao,
    private val backgroundSyncScheduler: BackgroundSyncScheduler,
) : PhotoRepository {

    override fun observeForSurvey(surveyId: String): Flow<List<PhotoEntity>> =
        photoDao.observeForSurvey(surveyId)

    override suspend fun addPhoto(
        surveyId: String,
        filePath: String,
        location: GeoPoint,
        accuracyMeters: Float,
        nowMillis: Long,
    ): PhotoEntity {
        val photo = PhotoEntity(
            id = UUID.randomUUID().toString(),
            surveyId = surveyId,
            filePath = filePath,
            capturedAtMillis = nowMillis,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = accuracyMeters,
            syncState = SyncState.PENDING_CREATE,
            updatedAtMillis = nowMillis,
        )
        photoDao.upsert(photo)
        backgroundSyncScheduler.scheduleSync()
        return photo
    }
}
