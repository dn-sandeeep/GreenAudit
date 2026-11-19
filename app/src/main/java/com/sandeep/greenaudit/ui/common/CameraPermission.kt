package com.sandeep.greenaudit.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

private const val KEY_CAMERA_REQUEST_COUNT = "camera_request_count"

/** True if the CAMERA permission is currently granted. */
fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun markCameraPermissionRequested(context: Context) =
    markPermissionRequested(context, KEY_CAMERA_REQUEST_COUNT)

fun isCameraPermissionPermanentlyDenied(context: Context): Boolean =
    isPermissionPermanentlyDenied(context, KEY_CAMERA_REQUEST_COUNT, hasCameraPermission(context))

fun resetCameraPermissionRequestCountIfGranted(context: Context) =
    resetPermissionRequestCountIfGranted(context, KEY_CAMERA_REQUEST_COUNT, hasCameraPermission(context))
