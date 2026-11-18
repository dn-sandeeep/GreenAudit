package com.sandeep.greenaudit.domain.survey

import com.sandeep.greenaudit.domain.gis.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInEligibilityUseCaseTest {

    private val useCase = CheckInEligibilityUseCase()

    /** ~222m x 222m square near the equator. */
    private val parkBoundary = listOf(
        GeoPoint(0.0, 0.0),
        GeoPoint(0.0, 0.002),
        GeoPoint(0.002, 0.002),
        GeoPoint(0.002, 0.0),
    )

    private val insidePoint = GeoPoint(0.001, 0.001)
    private val outsidePoint = GeoPoint(0.01, 0.01)

    @Test
    fun `inside the park with a good GPS fix is eligible`() {
        val result = useCase(insidePoint, gpsAccuracyMeters = 10f, parkBoundary)

        assertEquals(CheckInEligibility.Eligible, result)
    }

    @Test
    fun `inside the park but exactly at the accuracy threshold is eligible`() {
        val result = useCase(insidePoint, gpsAccuracyMeters = 25f, parkBoundary)

        assertEquals(CheckInEligibility.Eligible, result)
    }

    @Test
    fun `inside the park with a weak GPS fix is ineligible with the GPS reason`() {
        val result = useCase(insidePoint, gpsAccuracyMeters = 30f, parkBoundary)

        assertTrue(result is CheckInEligibility.Ineligible)
        val reason = (result as CheckInEligibility.Ineligible).reason
        assertTrue(reason is CheckInEligibility.Reason.WeakGps)
        assertEquals(30f, (reason as CheckInEligibility.Reason.WeakGps).accuracyMeters)
    }

    @Test
    fun `outside the park with a good GPS fix is ineligible with the boundary reason`() {
        val result = useCase(outsidePoint, gpsAccuracyMeters = 5f, parkBoundary)

        assertTrue(result is CheckInEligibility.Ineligible)
        val reason = (result as CheckInEligibility.Ineligible).reason
        assertTrue(reason is CheckInEligibility.Reason.OutsideBoundary)
    }

    @Test
    fun `outside the park with a weak GPS fix reports the boundary reason, not GPS`() {
        val result = useCase(outsidePoint, gpsAccuracyMeters = 50f, parkBoundary)

        assertTrue(result is CheckInEligibility.Ineligible)
        val reason = (result as CheckInEligibility.Ineligible).reason
        assertTrue(
            "outside-boundary should take priority over weak-GPS",
            reason is CheckInEligibility.Reason.OutsideBoundary,
        )
    }
}
