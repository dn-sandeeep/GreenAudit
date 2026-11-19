package com.sandeep.greenaudit.domain.gis

/**
 * Detects a self-intersecting (e.g. bowtie/figure-8) closed boundary, which
 * would make [GeoMath.computeAreaSqMeters] and [PointInPolygon] give
 * misleading results. Projected onto the same local tangent plane as
 * [PointInPolygon] (see [PlanarProjection]) so a straight-line "do these two
 * edges cross" test is enough - no need for geodesic arc intersection at
 * city-park scale.
 */
object PolygonSelfIntersection {

    fun isSelfIntersecting(polygon: List<GeoPoint>): Boolean {
        if (polygon.size < 4) return false

        val origin = polygon[0]
        val points = polygon.map { PlanarProjection.project(it, origin) }
        val n = points.size

        for (i in 0 until n) {
            val a1 = points[i]
            val a2 = points[(i + 1) % n]
            // Start the inner loop past the edge adjacent to i (shares a
            // vertex, not a crossing) and stop before the wrap-around pair
            // (edge n-1 / edge 0, which shares the closing vertex).
            for (j in i + 2 until n) {
                if (i == 0 && j == n - 1) continue
                val b1 = points[j]
                val b2 = points[(j + 1) % n]
                if (segmentsIntersect(a1, a2, b1, b2)) return true
            }
        }
        return false
    }

    private fun segmentsIntersect(
        p1: PlanarProjection.PlanarPoint,
        p2: PlanarProjection.PlanarPoint,
        p3: PlanarProjection.PlanarPoint,
        p4: PlanarProjection.PlanarPoint,
    ): Boolean {
        val d1 = direction(p3, p4, p1)
        val d2 = direction(p3, p4, p2)
        val d3 = direction(p1, p2, p3)
        val d4 = direction(p1, p2, p4)

        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
    }

    /** Cross product of (b - a) and (c - a): sign gives c's turn direction relative to a->b. */
    private fun direction(
        a: PlanarProjection.PlanarPoint,
        b: PlanarProjection.PlanarPoint,
        c: PlanarProjection.PlanarPoint,
    ): Double = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
}
