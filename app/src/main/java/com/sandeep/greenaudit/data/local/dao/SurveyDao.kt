package com.sandeep.greenaudit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sandeep.greenaudit.data.local.entity.SurveyEntity
import com.sandeep.greenaudit.data.local.entity.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDao {

    @Query("SELECT * FROM surveys WHERE parkId = :parkId ORDER BY checkInAtMillis DESC")
    fun observeForPark(parkId: String): Flow<List<SurveyEntity>>

    @Query("SELECT * FROM surveys WHERE id = :id")
    fun observeById(id: String): Flow<SurveyEntity?>

    @Query("SELECT * FROM surveys WHERE id = :id")
    suspend fun getById(id: String): SurveyEntity?

    @Query(
        "SELECT * FROM surveys WHERE parkId = :parkId ORDER BY checkInAtMillis DESC LIMIT 1",
    )
    suspend fun getLatestForPark(parkId: String): SurveyEntity?

    @Query("SELECT * FROM surveys WHERE syncState != :synced")
    suspend fun getUnsynced(synced: SyncState = SyncState.SYNCED): List<SurveyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(survey: SurveyEntity)

    @Update
    suspend fun update(survey: SurveyEntity)

    @Query("DELETE FROM surveys")
    suspend fun clearAll()
}
