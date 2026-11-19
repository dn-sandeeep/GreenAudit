package com.sandeep.greenaudit.ui.common

import android.content.Context

private const val PREFS_NAME = "permission_prefs"

/**
 * Shared two-strikes tracking behind [hasLocationPermission]'s and
 * [hasCameraPermission]'s "permanently denied" detection - both
 * permissions have identical Android-API characteristics here
 * (`shouldShowRequestPermissionRationale` proved unreliable on-device for
 * telling "never asked" apart from "Don't ask again"), so the logic is
 * written once and keyed per-permission in [prefsKey] rather than
 * duplicated for each one.
 */

/** Call right before every `permissionLauncher.launch(...)` for this permission. */
fun markPermissionRequested(context: Context, prefsKey: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(prefsKey, prefs.getInt(prefsKey, 0) + 1).apply()
}

/**
 * True once we've asked twice and still don't have it - Android stops
 * showing the dialog after a second denial, so a third `launch()` call
 * would otherwise silently do nothing, leaving the screen stuck with no
 * explanation.
 */
fun isPermissionPermanentlyDenied(context: Context, prefsKey: String, hasPermission: Boolean): Boolean {
    if (hasPermission) return false
    val requestCount = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(prefsKey, 0)
    return requestCount >= 2
}

/**
 * Call whenever a screen observes the permission is currently granted.
 * Granting it again (via the dialog or via Settings) clears any prior
 * denial streak on Android's side too, so if it's revoked again later
 * (e.g. manually in Settings, not via "Don't ask again"), the dialog
 * should be askable fresh - without this, [isPermissionPermanentlyDenied]
 * would stay stuck reporting "permanently denied" from an old streak
 * forever, even though nothing about the current state warrants that.
 */
fun resetPermissionRequestCountIfGranted(context: Context, prefsKey: String, hasPermission: Boolean) {
    if (hasPermission) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(prefsKey, 0).apply()
    }
}
