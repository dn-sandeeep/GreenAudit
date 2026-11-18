package com.sandeep.greenaudit.data.remote

import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.PhotoEntity
import com.sandeep.greenaudit.data.local.entity.SurveyEntity

/**
 * The backend boundary. There is no real server for this project (see
 * [FakeSyncApi]) - everything pushes through this interface so a real
 * implementation could be swapped in later without touching [SyncManager]
 * or anything above it.
 */
interface SyncApi {
    suspend fun pushPark(park: ParkEntity): Boolean
    suspend fun pushSurvey(survey: SurveyEntity): Boolean
    suspend fun pushPhoto(photo: PhotoEntity): Boolean
}
