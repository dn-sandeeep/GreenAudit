package com.sandeep.greenaudit.domain.gis

/**
 * Ray-casting (even-odd rule) point-in-polygon test, run on a local
 * tangent-plane projection of the polygon (see [PlanarProjection]) so it
 * works directly with [GeoPoint] coordinates.
 */
object PointInPolygon {

    fun contains(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        if (polygon.size < 3) return false

        val origin = polygon[0]
        val projectedPoint = PlanarProjection.project(point, origin)
        val projectedPolygon = polygon.map { PlanarProjection.project(it, origin) }

        return containsPlanar(projectedPoint, projectedPolygon)
    }

    private fun containsPlanar(
        point: PlanarProjection.PlanarPoint,
        polygon: List<PlanarProjection.PlanarPoint>,
    ): Boolean {
        var inside = false
        val n = polygon.size
        var j = n - 1
        for (i in 0 until n) {
            val vi = polygon[i]
            val vj = polygon[j]
            val edgeCrossesRay = (vi.y > point.y) != (vj.y > point.y) &&
                point.x < (vj.x - vi.x) * (point.y - vi.y) / (vj.y - vi.y) + vi.x
            if (edgeCrossesRay) inside = !inside
            j = i
        }
        return inside
    }
}
