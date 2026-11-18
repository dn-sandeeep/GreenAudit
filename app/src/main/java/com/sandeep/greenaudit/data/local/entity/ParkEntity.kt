package com.sandeep.greenaudit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.sandeep.greenaudit.data.local.converter.PolygonConverter
import com.sandeep.greenaudit.domain.gis.GeoPoint

/**
 * A surveyed city park. [boundary] is the ordered list of corners that
 * define its polygon; [areaSqMeters]/[perimeterMeters] are computed once
 * at save time from that boundary (see GeoMath) rather than recomputed on
 * every read.
 */
@Entity(tableName = "parks")
@TypeConverters(PolygonConverter::class)
data class ParkEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: ParkType,
    val boundary: List<GeoPoint>,
    val areaSqMeters: Double,
    val perimeterMeters: Double,
    val status: ParkStatus,
    val lastSurveyDateMillis: Long?,
    val syncState: SyncState,
    val updatedAtMillis: Long,
)
