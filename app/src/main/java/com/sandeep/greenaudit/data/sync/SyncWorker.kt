package com.sandeep.greenaudit.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs [SyncManager.syncNow] in the background, scheduled by
 * [BackgroundSyncScheduler] whenever an offline write leaves something
 * pending. A failed per-row push just leaves that row PENDING_* for the
 * next attempt (see [SyncManager]), so this always reports success rather
 * than triggering WorkManager's retry/backoff machinery.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        syncManager.syncNow()
        return Result.success()
    }
}
