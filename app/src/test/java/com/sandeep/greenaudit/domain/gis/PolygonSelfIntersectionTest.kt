package com.sandeep.greenaudit.domain.gis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolygonSelfIntersectionTest {

    @Test
    fun `convex quad does not self-intersect`() {
        val square = listOf(
            GeoPoint(0.0, 0.0),
            GeoPoint(0.0, 0.002),
            GeoPoint(0.002, 0.002),
            GeoPoint(0.002, 0.0),
        )
        assertFalse(PolygonSelfIntersection.isSelfIntersecting(square))
    }

    @Test
    fun `bowtie quad self-intersects`() {
        // Corners added out of winding order, so edge 0-1 crosses edge 2-3.
        val bowtie = listOf(
            GeoPoint(0.0, 0.0),
            GeoPoint(0.002, 0.002),
            GeoPoint(0.0, 0.002),
            GeoPoint(0.002, 0.0),
        )
        assertTrue(PolygonSelfIntersection.isSelfIntersecting(bowtie))
    }

    @Test
    fun `triangle can never self-intersect`() {
        val triangle = listOf(
            GeoPoint(0.0, 0.0),
            GeoPoint(0.0, 0.002),
            GeoPoint(0.002, 0.001),
        )
        assertFalse(PolygonSelfIntersection.isSelfIntersecting(triangle))
    }

    @Test
    fun `fewer than 4 points is never self-intersecting`() {
        assertFalse(PolygonSelfIntersection.isSelfIntersecting(emptyList()))
        assertFalse(
            PolygonSelfIntersection.isSelfIntersecting(
                listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 0.002), GeoPoint(0.002, 0.0)),
            ),
        )
    }

    @Test
    fun `L-shape with more than 4 points does not self-intersect`() {
        val lShape = listOf(
            GeoPoint(0.0, 0.0),
            GeoPoint(0.0, 0.004),
            GeoPoint(0.002, 0.004),
            GeoPoint(0.002, 0.002),
            GeoPoint(0.004, 0.002),
            GeoPoint(0.004, 0.0),
        )
        assertFalse(PolygonSelfIntersection.isSelfIntersecting(lShape))
    }
}
