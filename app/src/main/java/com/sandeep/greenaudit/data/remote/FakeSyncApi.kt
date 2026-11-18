package com.sandeep.greenaudit.data.remote

import com.sandeep.greenaudit.BuildConfig
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.PhotoEntity
import com.sandeep.greenaudit.data.local.entity.SurveyEntity
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Stands in for a real backend behind [SyncApi]. [BuildConfig.BASE_URL] is
 * read from build config (per build type), never hardcoded, even though
 * this fake never actually opens a connection to it - a real client would
 * read the same field. Behaves like a flaky network: a short randomized
 * delay and an occasional induced failure, so "Sync now" and the
 * pending-sync badge have something real to demonstrate.
 */
class FakeSyncApi @Inject constructor() : SyncApi {

    private val baseUrl = BuildConfig.BASE_URL

    override suspend fun pushPark(park: ParkEntity): Boolean = simulateCall()

    override suspend fun pushSurvey(survey: SurveyEntity): Boolean = simulateCall()

    override suspend fun pushPhoto(photo: PhotoEntity): Boolean = simulateCall()

    private suspend fun simulateCall(): Boolean {
        delay(Random.nextLong(MIN_DELAY_MILLIS, MAX_DELAY_MILLIS))
        return Random.nextDouble() > FAILURE_RATE
    }

    private companion object {
        const val MIN_DELAY_MILLIS = 250L
        const val MAX_DELAY_MILLIS = 900L
        const val FAILURE_RATE = 0.1
    }
}
