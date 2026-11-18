package com.sandeep.greenaudit.domain.repository

import com.sandeep.greenaudit.data.local.entity.Cleanliness
import com.sandeep.greenaudit.data.local.entity.SurveyEntity
import com.sandeep.greenaudit.domain.gis.GeoPoint
import kotlinx.coroutines.flow.Flow

interface SurveyRepository {

    fun observeForPark(parkId: String): Flow<List<SurveyEntity>>

    fun observeById(surveyId: String): Flow<SurveyEntity?>

    suspend fun getById(surveyId: String): SurveyEntity?

    /**
     * Creates the survey row at check-in time, before any form field is
     * filled in - see [SurveyEntity]'s own doc comment for why.
     */
    suspend fun checkIn(
        parkId: String,
        location: GeoPoint,
        accuracyMeters: Float,
        nowMillis: Long,
    ): SurveyEntity

    suspend fun submit(
        surveyId: String,
        treesCounted: Int,
        lightsWorking: Boolean,
        cleanliness: Cleanliness,
        notes: String,
        nowMillis: Long,
    )
}
