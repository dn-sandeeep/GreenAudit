package com.sandeep.greenaudit.ui.camera

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sandeep.greenaudit.ui.common.PermissionDeniedMessage
import com.sandeep.greenaudit.ui.common.PermissionPlaceholder
import com.sandeep.greenaudit.ui.common.hasCameraPermission
import com.sandeep.greenaudit.ui.common.isCameraPermissionPermanentlyDenied
import com.sandeep.greenaudit.ui.common.markCameraPermissionRequested
import com.sandeep.greenaudit.ui.common.resetCameraPermissionRequestCountIfGranted
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun CameraCaptureScreen(
    onBack: () -> Unit,
    onPhotoSaved: () -> Unit,
    viewModel: CameraCaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    var permanentlyDenied by remember { mutableStateOf(isCameraPermissionPermanentlyDenied(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) permanentlyDenied = isCameraPermissionPermanentlyDenied(context)
    }

    // A granted permission clears any prior denial streak, so a later Settings
    // revoke doesn't get stuck looking "permanently denied" from old history.
    LaunchedEffect(hasPermission) { resetCameraPermissionRequestCountIfGranted(context) }

    // Tapping the "+" photo tile is the first use of the camera feature -
    // ask directly on screen entry, no rationale UI first.
    LaunchedEffect(Unit) {
        if (!hasPermission && !permanentlyDenied) {
            markCameraPermissionRequested(context)
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        if (permanentlyDenied) {
            PermissionDeniedMessage(
                message = "Camera access is off - allow it in Settings to take a stamped photo.",
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                },
                modifier = Modifier.fillMaxSize().padding(24.dp),
            )
        } else {
            // Not granted yet - including right after the dialog was
            // cancelled with the back button, which Android reports as a
            // denial just like tapping "Deny". Give a way to retry without
            // leaving the screen.
            PermissionPlaceholder(
                message = "Allow the camera to take a stamped photo for this survey.",
                buttonLabel = "Allow camera",
                onAllowClick = {
                    markCameraPermissionRequested(context)
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
            )
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(previewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = suspendCancellableCoroutine { continuation ->
            cameraProviderFuture.addListener(
                { continuation.resume(cameraProviderFuture.get()) },
                ContextCompat.getMainExecutor(context),
            )
        }
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        if (viewModel.isSaving) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        viewModel.errorMessage?.let { message ->
            Card(modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth()) {
                Text(text = message, modifier = Modifier.padding(12.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                val outputDir = File(context.filesDir, "survey_photos").apply { mkdirs() }
                val photoFile = File(outputDir, "${UUID.randomUUID()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            viewModel.onPhotoCaptured(photoFile, onPhotoSaved)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            photoFile.delete()
                        }
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .size(72.dp),
        ) {
            Icon(Icons.Default.Camera, contentDescription = "Capture photo")
        }
    }
}
