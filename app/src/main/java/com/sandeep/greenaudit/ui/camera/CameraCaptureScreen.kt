package com.sandeep.greenaudit.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private fun hasCameraPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
fun CameraCaptureScreen(
    onBack: () -> Unit,
    onPhotoSaved: () -> Unit,
    viewModel: CameraCaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (!granted) {
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity?.let {
                androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            permanentlyDenied = !shouldShowRationale
        }
    }

    if (!hasPermission) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Card {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "GreenAudit needs the camera to take a stamped photo for this survey.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = {
                            if (permanentlyDenied) {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    ),
                                )
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        Text(if (permanentlyDenied) "Open settings" else "Allow camera")
                    }
                }
            }
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    androidx.compose.runtime.LaunchedEffect(previewView) {
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
