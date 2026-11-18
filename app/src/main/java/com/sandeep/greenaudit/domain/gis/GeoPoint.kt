package com.sandeep.greenaudit.domain.gis

/**
 * A WGS84 coordinate. Plain data, no Android dependency, so the whole
 * `domain.gis` package stays usable from JVM unit tests.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)
