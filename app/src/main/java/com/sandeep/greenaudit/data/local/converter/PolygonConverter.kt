package com.sandeep.greenaudit.data.local.converter

import androidx.room.TypeConverter
import com.sandeep.greenaudit.domain.gis.GeoPoint
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Stores a park boundary (ordered list of corners) as a JSON text column. */
class PolygonConverter {

    @TypeConverter
    fun fromPolygon(polygon: List<GeoPoint>): String = Json.encodeToString(polygon)

    @TypeConverter
    fun toPolygon(json: String): List<GeoPoint> = Json.decodeFromString(json)
}
