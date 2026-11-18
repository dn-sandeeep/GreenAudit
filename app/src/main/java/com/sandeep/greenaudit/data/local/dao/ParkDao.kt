package com.sandeep.greenaudit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import com.sandeep.greenaudit.data.local.entity.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkDao {

    @Query("SELECT * FROM parks ORDER BY name ASC")
    fun observeAll(): Flow<List<ParkEntity>>

    @Query("SELECT * FROM parks WHERE id = :id")
    fun observeById(id: String): Flow<ParkEntity?>

    @Query("SELECT * FROM parks WHERE id = :id")
    suspend fun getById(id: String): ParkEntity?

    @Query("SELECT * FROM parks WHERE syncState != :synced")
    suspend fun getUnsynced(synced: SyncState = SyncState.SYNCED): List<ParkEntity>

    @Query("SELECT COUNT(*) FROM parks")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(park: ParkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(parks: List<ParkEntity>)

    @Update
    suspend fun update(park: ParkEntity)

    @Query("DELETE FROM parks")
    suspend fun clearAll()
}
