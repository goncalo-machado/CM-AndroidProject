package com.example.projectcm.ui.mainapp.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.projectcm.ui.mainapp.problem_page.TrashProblemViewModel
import java.io.File


//@Composable
//fun CameraScreen(navController: NavController) {










//



//









//}
//
//@Composable
//fun CameraContent(navController: NavController) {








//






//




//







//










//































//}

@Composable
fun CameraScreen(trashProblemViewModel: TrashProblemViewModel, navController: NavController) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        CameraContent(trashProblemViewModel, navController)
    } else {
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
        ) {
            Text(text = "Camera permission is required to use this feature.")
        }
    }
}

@Composable
fun CameraContent(trashProblemViewModel: TrashProblemViewModel, navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    
    DisposableEffect(Unit) {
        val listener = {
            cameraProvider = cameraProviderFuture.get()
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    if (imageUri != null) {
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                imageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f) 
                            .padding(bottom = 16.dp)
                    )
                }

                Text(text = "Photo Captured!", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    imageUri?.let {
                        val generatedName = "photo_${System.currentTimeMillis()}" 
                        saveImageToGallery(it, generatedName, context)
                        trashProblemViewModel.setPhotoUri(imageUri!!)
                        trashProblemViewModel.saveTrashProblem()
                    }
                    imageUri = null 
                    navController.popBackStack()
                }) {
                    Text("Save Photo")
                }
            }
        }
    } else {
        CameraPreview(
            cameraProvider = cameraProvider,
            previewView = previewView,
            lifecycleOwner = lifecycleOwner,
            onImageCaptured = { uri -> imageUri = uri }
        )
    }
}

@Composable
fun CameraPreview(
    cameraProvider: ProcessCameraProvider?,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    onImageCaptured: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(cameraProvider) {
        cameraProvider?.let { provider ->
            val preview = androidx.camera.core.Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Button(
            onClick = {
                captureImage(imageCapture, context, onImageCaptured)
            }, modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Capture Photo")
        }
    }
}

fun captureImage(
    imageCapture: ImageCapture, context: Context, onImageCaptured: (Uri) -> Unit
) {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
        File(context.externalCacheDir, "photo_${System.currentTimeMillis()}.jpg")
    ).build()

    imageCapture.takePicture(outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri
                onImageCaptured(savedUri ?: Uri.EMPTY)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraPreview", "Error capturing image: ${exception.message}")
            }
        })
}

fun saveImageToGallery(imageUri: Uri, imageName: String, context: Context) {
    try {
        val contentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$imageName.jpg") 
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/CMProject"
            )
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
        }

        val imageUriForGallery =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        imageUriForGallery?.let { uri ->
            contentResolver.openOutputStream(uri).use { outputStream ->
                contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    inputStream.copyTo(outputStream!!)
                }
            }
            Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Log.e("CameraContent", "Error saving image to gallery: ${e.message}")
        Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
    }
}