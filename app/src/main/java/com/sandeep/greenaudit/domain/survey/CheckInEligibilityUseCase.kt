package com.sandeep.greenaudit.domain.survey

import com.sandeep.greenaudit.domain.gis.GeoMath
import com.sandeep.greenaudit.domain.gis.GeoPoint
import com.sandeep.greenaudit.domain.gis.PointInPolygon

/**
 * Decides whether a surveyor can start a survey: they must be physically
 * inside the park boundary AND the current GPS fix must be accurate to
 * 25m or better.
 *
 * When both checks fail, the outside-boundary reason takes priority over
 * the weak-GPS reason - "walk closer" is more actionable than "wait for a
 * better fix" when the user doesn't yet know which direction to walk.
 */
class CheckInEligibilityUseCase {

    operator fun invoke(
        userLocation: GeoPoint,
        gpsAccuracyMeters: Float,
        parkBoundary: List<GeoPoint>,
    ): CheckInEligibility {
        val isInside = PointInPolygon.contains(userLocation, parkBoundary)
        if (!isInside) {
            val distance = GeoMath.distanceToPolygonBoundaryMeters(userLocation, parkBoundary)
            return CheckInEligibility.Ineligible(
                CheckInEligibility.Reason.OutsideBoundary(distance),
            )
        }
        if (gpsAccuracyMeters > MAX_GPS_ACCURACY_METERS) {
            return CheckInEligibility.Ineligible(
                CheckInEligibility.Reason.WeakGps(gpsAccuracyMeters),
            )
        }
        return CheckInEligibility.Eligible
    }

    companion object {
        const val MAX_GPS_ACCURACY_METERS = 25f
    }
}
