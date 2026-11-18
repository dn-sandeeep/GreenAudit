package com.sandeep.greenaudit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sandeep.greenaudit.data.local.entity.PhotoEntity
import com.sandeep.greenaudit.data.local.entity.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Query("SELECT * FROM photos WHERE surveyId = :surveyId ORDER BY capturedAtMillis ASC")
    fun observeForSurvey(surveyId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE syncState != :synced")
    suspend fun getUnsynced(synced: SyncState = SyncState.SYNCED): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: PhotoEntity)

    @Query("DELETE FROM photos")
    suspend fun clearAll()
}
