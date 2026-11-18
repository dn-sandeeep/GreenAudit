package com.sandeep.greenaudit.domain.gis

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geodesic calculations (distance, bearing) on the earth's surface.
 * Polygon area/perimeter and point-in-polygon live in sibling files in
 * this package; everything here is pure Kotlin so it is unit-testable
 * without Robolectric or an emulator.
 */
object GeoMath {

    /** Authalic mean Earth radius, in meters - gives the best area accuracy. */
    const val EARTH_RADIUS_METERS = 6_371_008.8

    private val COMPASS_POINTS = arrayOf(
        "north", "north-east", "east", "south-east",
        "south", "south-west", "west", "north-west",
    )

    /** Great-circle distance between two points, via the haversine formula. */
    fun haversineDistanceMeters(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /** Initial bearing (forward azimuth) from `from` to `to`, in degrees [0, 360). */
    fun initialBearingDegrees(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val theta = atan2(y, x)
        return (Math.toDegrees(theta) + 360.0) % 360.0
    }

    /** Buckets a bearing into one of 8 compass directions, e.g. "north-east". */
    fun bearingToCompass(bearingDegrees: Double): String {
        val index = round(bearingDegrees / 45.0).toInt().mod(8)
        return COMPASS_POINTS[index]
    }

    internal fun degreesToRadians(degrees: Double): Double = degrees * PI / 180.0
}
