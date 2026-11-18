package com.sandeep.greenaudit.domain.gis

import kotlinx.serialization.Serializable

/**
 * A WGS84 coordinate. Plain data, no Android dependency, so the whole
 * `domain.gis` package stays usable from JVM unit tests. @Serializable
 * (kotlinx.serialization is pure Kotlin too) lets Room's PolygonConverter
 * store a park boundary as a JSON column without a separate DTO.
 */
@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)
