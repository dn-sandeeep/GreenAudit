package com.sandeep.greenaudit.domain.repository

import com.sandeep.greenaudit.data.local.entity.PhotoEntity
import com.sandeep.greenaudit.domain.gis.GeoPoint
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {

    fun observeForSurvey(surveyId: String): Flow<List<PhotoEntity>>

    /** Records a photo already captured and stamped to [filePath] on disk. */
    suspend fun addPhoto(
        surveyId: String,
        filePath: String,
        location: GeoPoint,
        accuracyMeters: Float,
        nowMillis: Long,
    ): PhotoEntity
}
