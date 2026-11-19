package com.sandeep.greenaudit.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private const val PREFS_NAME = "permission_prefs"
private const val KEY_LOCATION_REQUEST_COUNT = "location_request_count"

/** True if either fine or coarse location is currently granted. */
fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Call right before every `permissionLauncher.launch(...)` for location.
 * Tracked ourselves (rather than relying on
 * `shouldShowRequestPermissionRationale`, which proved unreliable for
 * detecting "Don't ask again" on-device) so we know, deterministically,
 * when re-launching the system dialog would silently no-op.
 */
fun markLocationPermissionRequested(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putInt(KEY_LOCATION_REQUEST_COUNT, prefs.getInt(KEY_LOCATION_REQUEST_COUNT, 0) + 1).apply()
}

/**
 * True once we've asked twice and still don't have it - Android stops
 * showing the dialog after a second denial ("Don't ask again" / the user
 * picks Deny twice), so a third `launch()` call would otherwise silently
 * do nothing, leaving the screen stuck with no explanation.
 */
fun isLocationPermissionPermanentlyDenied(context: Context): Boolean {
    if (hasLocationPermission(context)) return false
    val requestCount = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_LOCATION_REQUEST_COUNT, 0)
    return requestCount >= 2
}

/**
 * Call whenever a screen observes the permission is currently granted.
 * Granting it again (via the dialog or via Settings) clears any prior
 * denial streak on Android's side too, so if it's revoked again later
 * (e.g. manually in Settings, not via "Don't ask again"), the dialog
 * should be askable fresh - without this, [isLocationPermissionPermanentlyDenied]
 * would stay stuck reporting "permanently denied" from an old streak
 * forever, even though nothing about the current state warrants that.
 */
fun resetLocationPermissionRequestCountIfGranted(context: Context) {
    if (hasLocationPermission(context)) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LOCATION_REQUEST_COUNT, 0)
            .apply()
    }
}

/**
 * The one case that needs a fallback: the user picked "Don't ask again",
 * so re-launching the system dialog silently does nothing. Everywhere
 * else, the system dialog is fired directly with no rationale UI first.
 */
@Composable
fun PermissionDeniedMessage(
    message: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = message)
        TextButton(onClick = onOpenSettings) {
            Text("Open settings")
        }
    }
}

/**
 * Shown instead of a map while permission is missing but not yet
 * permanently denied - including right after the system dialog was
 * cancelled with the back button (Android reports that the same as
 * tapping "Deny"). Gives a way back in without leaving the screen.
 */
@Composable
fun LocationPermissionPlaceholder(message: String, onAllowClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAllowClick, modifier = Modifier.padding(top = 16.dp)) {
                Text("Allow location")
            }
        }
    }
}
