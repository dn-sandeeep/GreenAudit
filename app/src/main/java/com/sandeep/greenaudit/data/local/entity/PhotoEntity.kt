package com.sandeep.greenaudit.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A photo captured during a survey. [filePath] points at the on-device
 * JPEG, which already has the park/lat-lon/accuracy/date-time stamp
 * burned into its pixels (see PhotoStampUtil) - this row is just the
 * record of it for listing, sync, and cleanup on logout.
 */
@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = SurveyEntity::class,
            parentColumns = ["id"],
            childColumns = ["surveyId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("surveyId")],
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val surveyId: String,
    val filePath: String,
    val capturedAtMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val syncState: SyncState,
    val updatedAtMillis: Long,
)
