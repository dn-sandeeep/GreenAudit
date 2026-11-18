package com.sandeep.greenaudit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A survey visit to a park. A row is created at check-in time (with the
 * form fields still empty/default) so the check-in itself is never lost
 * even if the surveyor backs out before submitting; [submittedAtMillis]
 * is null until "Submit survey" is tapped.
 */
@Entity(
    tableName = "surveys",
    foreignKeys = [
        ForeignKey(
            entity = ParkEntity::class,
            parentColumns = ["id"],
            childColumns = ["parkId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parkId")],
)
data class SurveyEntity(
    @PrimaryKey val id: String,
    val parkId: String,
    val checkInAtMillis: Long,
    val checkInLatitude: Double,
    val checkInLongitude: Double,
    val checkInAccuracyMeters: Float,
    val treesCounted: Int,
    val lightsWorking: Boolean,
    val cleanliness: Cleanliness,
    val notes: String,
    val submittedAtMillis: Long?,
    val syncState: SyncState,
    val updatedAtMillis: Long,
)
