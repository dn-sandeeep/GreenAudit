package com.sandeep.greenaudit.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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

private const val KEY_LOCATION_REQUEST_COUNT = "location_request_count"

/** True if either fine or coarse location is currently granted. */
fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

fun markLocationPermissionRequested(context: Context) =
    markPermissionRequested(context, KEY_LOCATION_REQUEST_COUNT)

fun isLocationPermissionPermanentlyDenied(context: Context): Boolean =
    isPermissionPermanentlyDenied(context, KEY_LOCATION_REQUEST_COUNT, hasLocationPermission(context))

fun resetLocationPermissionRequestCountIfGranted(context: Context) =
    resetPermissionRequestCountIfGranted(context, KEY_LOCATION_REQUEST_COUNT, hasLocationPermission(context))

/**
 * The explanation shown once, right before the system permission dialog -
 * satisfies "explain why" without a persistent on-screen card. "Allow"
 * should trigger the real `permissionLauncher.launch(...)`; "Not now"
 * just dismisses, leaving the caller's retry path (a [PermissionPlaceholder]
 * button) as the way back in.
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    message: String,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onAllow) { Text("Allow") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

/**
 * The one case that needs a fallback: the user picked "Don't ask again",
 * so re-launching the system dialog silently does nothing.
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
 * Shown instead of the feature's content while permission is missing but
 * not yet permanently denied - including right after the system dialog
 * was cancelled with the back button (Android reports that the same as
 * tapping "Deny"). Gives a way back in without leaving the screen.
 */
@Composable
fun PermissionPlaceholder(
    message: String,
    buttonLabel: String,
    onAllowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Text(buttonLabel)
            }
        }
    }
}
