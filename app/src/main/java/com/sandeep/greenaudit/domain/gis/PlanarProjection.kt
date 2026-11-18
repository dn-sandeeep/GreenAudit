package com.sandeep.greenaudit.domain.gis

import kotlin.math.cos

/**
 * Projects [GeoPoint]s onto a local tangent plane (equirectangular
 * approximation) centered near a given origin. Accurate enough at
 * city-park scale (a few hundred meters across) and keeps point-in-polygon
 * / nearest-edge math simple flat-plane geometry instead of full geodesic
 * arc intersection, which would be real over-engineering for this scale.
 */
object PlanarProjection {

    data class PlanarPoint(val x: Double, val y: Double)

    /** Projects [point] to meters on the tangent plane centered at [origin]. */
    fun project(point: GeoPoint, origin: GeoPoint): PlanarPoint {
        val originLatRad = Math.toRadians(origin.latitude)
        val x = Math.toRadians(point.longitude - origin.longitude) *
            cos(originLatRad) * GeoMath.EARTH_RADIUS_METERS
        val y = Math.toRadians(point.latitude - origin.latitude) * GeoMath.EARTH_RADIUS_METERS
        return PlanarPoint(x, y)
    }
}
