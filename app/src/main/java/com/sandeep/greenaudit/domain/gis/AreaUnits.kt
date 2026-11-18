package com.sandeep.greenaudit.domain.gis

/** Unit conversions for areas computed by [GeoMath]. */
object AreaUnits {

    private const val SQ_METERS_PER_ACRE = 4046.8564224

    fun squareMetersToAcres(squareMeters: Double): Double = squareMeters / SQ_METERS_PER_ACRE
}
