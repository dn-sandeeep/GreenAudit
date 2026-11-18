package com.sandeep.greenaudit.data.repository

import com.sandeep.greenaudit.data.local.dao.SurveyDao
import com.sandeep.greenaudit.data.local.entity.Cleanliness
import com.sandeep.greenaudit.data.local.entity.SurveyEntity
import com.sandeep.greenaudit.data.local.entity.SyncState
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.repository.SurveyRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class DefaultSurveyRepository @Inject constructor(
    private val surveyDao: SurveyDao,
) : SurveyRepository {

    override fun observeForPark(parkId: String): Flow<List<SurveyEntity>> = surveyDao.observeForPark(parkId)

    override fun observeById(surveyId: String): Flow<SurveyEntity?> = surveyDao.observeById(surveyId)

    override suspend fun getById(surveyId: String): SurveyEntity? = surveyDao.getById(surveyId)

    override suspend fun checkIn(
        parkId: String,
        location: GeoPoint,
        accuracyMeters: Float,
        nowMillis: Long,
    ): SurveyEntity {
        val survey = SurveyEntity(
            id = UUID.randomUUID().toString(),
            parkId = parkId,
            checkInAtMillis = nowMillis,
            checkInLatitude = location.latitude,
            checkInLongitude = location.longitude,
            checkInAccuracyMeters = accuracyMeters,
            treesCounted = 0,
            lightsWorking = true,
            cleanliness = Cleanliness.GOOD,
            notes = "",
            submittedAtMillis = null,
            syncState = SyncState.PENDING_CREATE,
            updatedAtMillis = nowMillis,
        )
        surveyDao.upsert(survey)
        return survey
    }

    override suspend fun submit(
        surveyId: String,
        treesCounted: Int,
        lightsWorking: Boolean,
        cleanliness: Cleanliness,
        notes: String,
        nowMillis: Long,
    ) {
        val survey = surveyDao.getById(surveyId) ?: return
        surveyDao.update(
            survey.copy(
                treesCounted = treesCounted,
                lightsWorking = lightsWorking,
                cleanliness = cleanliness,
                notes = notes,
                submittedAtMillis = nowMillis,
                syncState = if (survey.syncState == SyncState.PENDING_CREATE) {
                    SyncState.PENDING_CREATE
                } else {
                    SyncState.PENDING_UPDATE
                },
                updatedAtMillis = nowMillis,
            ),
        )
    }
}
