package com.sandeep.greenaudit.domain.gis

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathAreaPerimeterTest {

    /**
     * A tiny square at the equator, where a degree of longitude and a degree
     * of latitude are (locally) the same length, so the flat-plane estimate
     * below is a valid independent check on the spherical-excess formula.
     */
    private val equatorSquare = listOf(
        GeoPoint(0.0, 0.0),
        GeoPoint(0.0, 0.001),
        GeoPoint(0.001, 0.001),
        GeoPoint(0.001, 0.0),
    )

    @Test
    fun `area of a small equatorial square matches the flat-plane estimate`() {
        val metersPerDegree = GeoMath.EARTH_RADIUS_METERS * Math.PI / 180.0
        val sideMeters = 0.001 * metersPerDegree
        val expectedAreaSqMeters = sideMeters * sideMeters

        val area = GeoMath.computeAreaSqMeters(equatorSquare)

        assertEquals(expectedAreaSqMeters, area, expectedAreaSqMeters * 0.01)
    }

    @Test
    fun `perimeter of a small equatorial square matches the flat-plane estimate`() {
        val metersPerDegree = GeoMath.EARTH_RADIUS_METERS * Math.PI / 180.0
        val sideMeters = 0.001 * metersPerDegree
        val expectedPerimeterMeters = sideMeters * 4

        val perimeter = GeoMath.computePerimeterMeters(equatorSquare)

        assertEquals(expectedPerimeterMeters, perimeter, 1.0)
    }

    @Test
    fun `area is zero for fewer than 3 points`() {
        assertEquals(0.0, GeoMath.computeAreaSqMeters(listOf(GeoPoint(0.0, 0.0))), 0.0)
        assertEquals(
            0.0,
            GeoMath.computeAreaSqMeters(listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 1.0))),
            0.0,
        )
    }

    @Test
    fun `area ignores winding direction`() {
        val clockwise = equatorSquare
        val counterClockwise = equatorSquare.reversed()

        assertEquals(
            GeoMath.computeAreaSqMeters(clockwise),
            GeoMath.computeAreaSqMeters(counterClockwise),
            0.0001,
        )
    }

    @Test
    fun `one acre is about 4047 square meters`() {
        assertEquals(1.0, AreaUnits.squareMetersToAcres(4046.8564224), 0.0001)
    }
}
