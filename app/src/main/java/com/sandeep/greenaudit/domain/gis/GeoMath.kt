package com.sandeep.greenaudit.domain.gis

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
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

    /**
     * Area of a closed polygon on the earth's surface, in square meters.
     *
     * Uses the spherical-excess formula (a line integral over the sphere via
     * Green's theorem - the same family as Google Maps Utils'
     * SphericalUtil.computeArea), NOT flat shoelace on raw lat/lon degrees,
     * which would be wrong off the equator since a degree of longitude
     * shrinks by cos(latitude) while a degree of latitude does not.
     *
     * `polygon` need not repeat its first point as its last - the ring is
     * closed automatically.
     */
    fun computeAreaSqMeters(polygon: List<GeoPoint>): Double {
        if (polygon.size < 3) return 0.0

        var total = 0.0
        val n = polygon.size
        for (i in 0 until n) {
            val current = polygon[i]
            val next = polygon[(i + 1) % n]
            val lat1 = Math.toRadians(current.latitude)
            val lat2 = Math.toRadians(next.latitude)
            val lon1 = Math.toRadians(current.longitude)
            val lon2 = Math.toRadians(next.longitude)
            total += (lon2 - lon1) * (2 + sin(lat1) + sin(lat2))
        }
        return abs(total) * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS / 2.0
    }

    /** Perimeter of a closed polygon, summing haversine distance edge by edge. */
    fun computePerimeterMeters(polygon: List<GeoPoint>): Double {
        if (polygon.size < 2) return 0.0

        var total = 0.0
        val n = polygon.size
        for (i in 0 until n) {
            total += haversineDistanceMeters(polygon[i], polygon[(i + 1) % n])
        }
        return total
    }

    /**
     * Shortest distance from [point] to the polygon's boundary (nearest
     * edge), in meters. Used to tell a surveyor who is outside a park how
     * far away they still are. Computed on the same tangent-plane
     * projection as [PointInPolygon], so it stays consistent with the
     * inside/outside check.
     */
    fun distanceToPolygonBoundaryMeters(point: GeoPoint, polygon: List<GeoPoint>): Double {
        if (polygon.isEmpty()) return Double.NaN
        if (polygon.size == 1) return haversineDistanceMeters(point, polygon[0])

        val origin = polygon[0]
        val projectedPoint = PlanarProjection.project(point, origin)
        val projectedPolygon = polygon.map { PlanarProjection.project(it, origin) }

        var minDistance = Double.MAX_VALUE
        val n = projectedPolygon.size
        for (i in 0 until n) {
            val a = projectedPolygon[i]
            val b = projectedPolygon[(i + 1) % n]
            val distance = distancePointToSegment(projectedPoint, a, b)
            if (distance < minDistance) minDistance = distance
        }
        return minDistance
    }

    private fun distancePointToSegment(
        p: PlanarProjection.PlanarPoint,
        a: PlanarProjection.PlanarPoint,
        b: PlanarProjection.PlanarPoint,
    ): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        if (dx == 0.0 && dy == 0.0) {
            return hypot(p.x - a.x, p.y - a.y)
        }
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
        val closestX = a.x + t * dx
        val closestY = a.y + t * dy
        return hypot(p.x - closestX, p.y - closestY)
    }

    internal fun degreesToRadians(degrees: Double): Double = degrees * PI / 180.0
}
