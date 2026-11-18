package com.sandeep.greenaudit.domain.gis

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathDistanceBearingTest {

    private val equator0 = GeoPoint(0.0, 0.0)

    @Test
    fun `one degree of longitude at the equator is about 111 km`() {
        val oneDegreeEast = GeoPoint(0.0, 1.0)

        val distance = GeoMath.haversineDistanceMeters(equator0, equator0.let { oneDegreeEast })

        assertEquals(111_195.0, distance, 100.0)
    }

    @Test
    fun `one degree of latitude is about 111 km`() {
        val oneDegreeNorth = GeoPoint(1.0, 0.0)

        val distance = GeoMath.haversineDistanceMeters(equator0, oneDegreeNorth)

        assertEquals(111_195.0, distance, 100.0)
    }

    @Test
    fun `distance between identical points is zero`() {
        val distance = GeoMath.haversineDistanceMeters(equator0, equator0)

        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `bearing due north is zero degrees`() {
        val dueNorth = GeoPoint(1.0, 0.0)

        val bearing = GeoMath.initialBearingDegrees(equator0, dueNorth)

        assertEquals(0.0, bearing, 0.5)
    }

    @Test
    fun `bearing due east is ninety degrees`() {
        val dueEast = GeoPoint(0.0, 1.0)

        val bearing = GeoMath.initialBearingDegrees(equator0, dueEast)

        assertEquals(90.0, bearing, 0.5)
    }

    @Test
    fun `bearing due south is 180 degrees`() {
        val dueSouth = GeoPoint(-1.0, 0.0)

        val bearing = GeoMath.initialBearingDegrees(equator0, dueSouth)

        assertEquals(180.0, bearing, 0.5)
    }

    @Test
    fun `bearing due west is 270 degrees`() {
        val dueWest = GeoPoint(0.0, -1.0)

        val bearing = GeoMath.initialBearingDegrees(equator0, dueWest)

        assertEquals(270.0, bearing, 0.5)
    }

    @Test
    fun `compass bucketing matches cardinal and intercardinal directions`() {
        assertEquals("north", GeoMath.bearingToCompass(0.0))
        assertEquals("north-east", GeoMath.bearingToCompass(45.0))
        assertEquals("east", GeoMath.bearingToCompass(90.0))
        assertEquals("south-east", GeoMath.bearingToCompass(135.0))
        assertEquals("south", GeoMath.bearingToCompass(180.0))
        assertEquals("south-west", GeoMath.bearingToCompass(225.0))
        assertEquals("west", GeoMath.bearingToCompass(270.0))
        assertEquals("north-west", GeoMath.bearingToCompass(315.0))
    }

    @Test
    fun `compass bucketing wraps around 360 degrees`() {
        assertEquals("north", GeoMath.bearingToCompass(359.0))
        assertEquals("north", GeoMath.bearingToCompass(1.0))
    }
}
