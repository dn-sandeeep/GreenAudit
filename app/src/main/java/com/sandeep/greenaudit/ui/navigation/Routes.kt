package com.sandeep.greenaudit.ui.navigation

/** Route string constants for the single-activity Nav host. */
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val PARKS = "parks"
    const val MAP_NEW_PARK = "mapNewPark"
    const val SURVEY = "survey/{parkId}"
    const val CAMERA = "camera/{parkId}/{surveyId}"

    fun survey(parkId: String) = "survey/$parkId"
    fun camera(parkId: String, surveyId: String) = "camera/$parkId/$surveyId"
}
