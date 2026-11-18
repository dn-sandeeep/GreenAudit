package com.sandeep.greenaudit.domain.survey

/** Result of [CheckInEligibilityUseCase]. */
sealed interface CheckInEligibility {

    data object Eligible : CheckInEligibility

    data class Ineligible(val reason: Reason) : CheckInEligibility

    sealed interface Reason {
        /** User is outside the park boundary, [distanceMeters] away from it. */
        data class OutsideBoundary(val distanceMeters: Double) : Reason

        /** User is inside the park, but the GPS fix isn't accurate enough. */
        data class WeakGps(val accuracyMeters: Float) : Reason
    }
}
