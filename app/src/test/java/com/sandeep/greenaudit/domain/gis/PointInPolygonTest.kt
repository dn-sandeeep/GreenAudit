package com.sandeep.greenaudit.domain.gis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointInPolygonTest {

    /** ~222m x 222m square near the equator. */
    private val square = listOf(
        GeoPoint(0.0, 0.0),
        GeoPoint(0.0, 0.002),
        GeoPoint(0.002, 0.002),
        GeoPoint(0.002, 0.0),
    )

    /**
     * An "L" shape (a square with its top-right quadrant removed), traced
     * clockwise, at a realistic park scale (edges ~0.002-0.004 degrees,
     * i.e. a couple hundred meters).
     */
    private val lShape = listOf(
        GeoPoint(0.0, 0.0),
        GeoPoint(0.0, 0.004),
        GeoPoint(0.002, 0.004),
        GeoPoint(0.002, 0.002),
        GeoPoint(0.004, 0.002),
        GeoPoint(0.004, 0.0),
    )

    @Test
    fun `point well inside the square is inside`() {
        assertTrue(PointInPolygon.contains(GeoPoint(0.001, 0.001), square))
    }

    @Test
    fun `point well outside the square is outside`() {
        assertFalse(PointInPolygon.contains(GeoPoint(0.005, 0.005), square))
        assertFalse(PointInPolygon.contains(GeoPoint(-0.001, -0.001), square))
    }

    @Test
    fun `fewer than 3 points is never inside`() {
        assertFalse(PointInPolygon.contains(GeoPoint(0.0, 0.0), emptyList()))
        assertFalse(
            PointInPolygon.contains(
                GeoPoint(0.0, 0.0),
                listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.002)),
            ),
        )
    }

    @Test
    fun `point in the notch of an L-shape is outside`() {
        assertFalse(PointInPolygon.contains(GeoPoint(0.003, 0.003), lShape))
    }

    @Test
    fun `point in the base of an L-shape is inside`() {
        assertTrue(PointInPolygon.contains(GeoPoint(0.001, 0.001), lShape))
    }

    @Test
    fun `point in the bottom leg of an L-shape is inside`() {
        assertTrue(PointInPolygon.contains(GeoPoint(0.001, 0.003), lShape))
    }

    @Test
    fun `point in the left leg of an L-shape is inside`() {
        assertTrue(PointInPolygon.contains(GeoPoint(0.003, 0.001), lShape))
    }

    @Test
    fun `point far outside an L-shape is outside`() {
        assertFalse(PointInPolygon.contains(GeoPoint(0.01, 0.01), lShape))
    }

    @Test
    fun `distance to boundary for a point just above the top edge`() {
        // ~111m above the top edge, horizontally within the square's span.
        val pointAboveTopEdge = GeoPoint(0.003, 0.001)

        val distance = GeoMath.distanceToPolygonBoundaryMeters(pointAboveTopEdge, square)

        assertEquals(111.19, distance, 1.0)
    }

    @Test
    fun `distance to boundary is zero for a point on an edge`() {
        val pointOnEdge = GeoPoint(0.0, 0.001)

        val distance = GeoMath.distanceToPolygonBoundaryMeters(pointOnEdge, square)

        assertEquals(0.0, distance, 0.01)
    }
}
