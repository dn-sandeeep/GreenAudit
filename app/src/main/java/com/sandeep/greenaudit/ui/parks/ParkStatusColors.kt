package com.sandeep.greenaudit.ui.parks

import androidx.compose.ui.graphics.Color
import com.sandeep.greenaudit.data.local.entity.ParkStatus

/** Status colours per the spec: green = Open, amber = Needs repair, red = Closed. */
fun ParkStatus.toColor(): Color = when (this) {
    ParkStatus.OPEN -> Color(0xFF2E7D32)
    ParkStatus.NEEDS_REPAIR -> Color(0xFFF9A825)
    ParkStatus.CLOSED -> Color(0xFFC62828)
}

fun ParkStatus.label(): String = when (this) {
    ParkStatus.OPEN -> "Open"
    ParkStatus.NEEDS_REPAIR -> "Needs repair"
    ParkStatus.CLOSED -> "Closed"
}

fun com.sandeep.greenaudit.data.local.entity.ParkType.label(): String = when (this) {
    com.sandeep.greenaudit.data.local.entity.ParkType.GARDEN -> "Garden"
    com.sandeep.greenaudit.data.local.entity.ParkType.PLAYGROUND -> "Playground"
    com.sandeep.greenaudit.data.local.entity.ParkType.FOREST_PARK -> "Forest park"
    com.sandeep.greenaudit.data.local.entity.ParkType.LAKE_FRONT -> "Lake front"
}
