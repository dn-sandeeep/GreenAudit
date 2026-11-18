package com.sandeep.greenaudit.data.sync

import com.sandeep.greenaudit.data.local.dao.ParkDao
import com.sandeep.greenaudit.data.local.dao.PhotoDao
import com.sandeep.greenaudit.data.local.dao.SurveyDao
import com.sandeep.greenaudit.data.local.entity.SyncState
import com.sandeep.greenaudit.data.remote.SyncApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pushes every row with a [SyncState] other than SYNCED through [SyncApi],
 * flipping it to SYNCED on success. Parks are pushed before surveys and
 * surveys before photos so a backend validating foreign keys would see
 * parents before children; a failed push just leaves that row's state
 * alone; so it's retried on request the next time "Sync now" runs.
 *
 * Conflict policy (two phones editing the same park offline): last write
 * wins by [com.sandeep.greenaudit.data.local.entity.ParkEntity.updatedAtMillis] -
 * whichever device syncs last overwrites the server copy. This is called
 * out in the README as a deliberate, documented tradeoff rather than true
 * merge resolution, which is out of scope for this task.
 */
@Singleton
class SyncManager @Inject constructor(
    private val parkDao: ParkDao,
    private val surveyDao: SurveyDao,
    private val photoDao: PhotoDao,
    private val syncApi: SyncApi,
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun syncNow() {
        _isSyncing.value = true
        try {
            parkDao.getUnsynced().forEach { park ->
                if (syncApi.pushPark(park)) {
                    parkDao.update(park.copy(syncState = SyncState.SYNCED))
                }
            }
            surveyDao.getUnsynced().forEach { survey ->
                if (syncApi.pushSurvey(survey)) {
                    surveyDao.update(survey.copy(syncState = SyncState.SYNCED))
                }
            }
            photoDao.getUnsynced().forEach { photo ->
                if (syncApi.pushPhoto(photo)) {
                    photoDao.upsert(photo.copy(syncState = SyncState.SYNCED))
                }
            }
        } finally {
            _isSyncing.value = false
        }
    }
}
