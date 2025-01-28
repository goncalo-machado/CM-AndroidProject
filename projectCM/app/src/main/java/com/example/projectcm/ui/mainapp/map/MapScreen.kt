package com.example.projectcm.ui.mainapp.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.projectcm.R
import com.example.projectcm.SharedViewModel
import com.example.projectcm.database.entities.TrashProblem
import com.example.projectcm.ui.mainapp.problem_page.TrashProblemViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay


@Composable
fun MapScreen(
    sharedViewModel: SharedViewModel,
    trashProblemViewModel: TrashProblemViewModel,
    navController: NavController,
    fusedLocationClient: FusedLocationProviderClient
) {
    val context = LocalContext.current
    val currentUser by sharedViewModel.currentUser.collectAsState()
    val trashProblems by trashProblemViewModel.trashProblems.collectAsState()
    val defaultLocation = GeoPoint(37.7749, -122.4194)
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var longPressMarker by remember { mutableStateOf<Marker?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            getCurrentLocation(context, fusedLocationClient) { location ->
                location?.let {
                    userLocation = GeoPoint(it.latitude, it.longitude)
                    Log.d("MapScreen", "Fused location: $userLocation")
                } ?: run {
                    Log.d("MapScreen", "No location available from FusedLocationProviderClient")
                }
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { context ->
            MapView(context).apply {
                setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                minZoomLevel = 5.0
                maxZoomLevel = 20.0
                controller.setZoom(15.0)
                controller.setCenter(userLocation ?: defaultLocation)
                mapViewRef.value = this


                if (currentUser?.role == "User") {
                    
                    overlays.add(object : Overlay() {
                        override fun onLongPress(e: MotionEvent, mapView: MapView?): Boolean {
                            val geoPoint = mapView?.projection?.fromPixels(e.x.toInt(), e.y.toInt())
                            geoPoint?.let {

                                longPressMarker?.let { marker ->
                                    mapView.overlays?.remove(marker)
                                }

                                longPressMarker = Marker(mapView).apply {
                                    position = geoPoint as GeoPoint?
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = "New Marker"
                                    snippet = "Tap to take a photo and save"
                                    setOnMarkerClickListener { _, _ ->
                                        val trashProblem = TrashProblem(
                                            latitude = position.latitude,
                                            longitude = position.longitude,
                                            status = "Reported",
                                            userId = sharedViewModel.currentUser.value?.id ?: 0,
                                            imagePath = ""
                                        )

                                        trashProblemViewModel.setTrashProblem(trashProblem)

                                        navController.navigate("Camera")
                                        true
                                    }
                                }
                                mapView.overlays?.add(longPressMarker)
                                mapView.invalidate()
                            }
                            return true
                        }
                    })
                }
            }
        }, modifier = Modifier.fillMaxSize(), update = { mapView ->
            mapView.controller.setCenter(userLocation ?: defaultLocation)

            
            userLocation?.let { location ->
                val userMarker = Marker(mapView).apply {
                    position = location
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "You are here"
                    icon = ContextCompat.getDrawable(mapView.context, R.drawable.my_location)
                }
                mapView.overlays.removeIf { it is Marker && it.title == "You are here" }
                mapView.overlays.add(userMarker)
                mapView.invalidate()
            }

            mapView.overlays.removeIf { it is Marker && it.title?.startsWith("Trash Problem") == true }
            trashProblems.forEach { problem ->
                val marker = Marker(mapView).apply {
                    position = GeoPoint(problem.latitude, problem.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Trash Problem #${problem.id}"
                    snippet =
                        if (problem.status == "Resolved") "Resolved Problem" else "Reported Problem"
                    icon = if (problem.status == "Resolved") ContextCompat.getDrawable(
                        mapView.context,
                        R.drawable.resolved_marker
                    )
                    else ContextCompat.getDrawable(mapView.context, R.drawable.reported_marker)

                    
                    setOnMarkerClickListener { _, _ ->
                        
                        val problemId = problem.id.toString()
                        navController.navigate("Problem_Details/$problemId")
                        true 
                    }
                }

                mapView.overlays.add(marker)
            }
        })

        
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .background(Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(text = userLocation?.let {
                "Latitude: %.6f\nLongitude: %.6f".format(it.latitude, it.longitude)
            } ?: "Latitude: %.6f\nLongitude: %.6f".format(
                defaultLocation.latitude,
                defaultLocation.longitude
            ), style = MaterialTheme.typography.bodySmall.copy(color = Color.Black))
        }

        
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Button(onClick = {
                userLocation?.let { location ->
                    mapViewRef.value?.controller?.animateTo(location)
                } ?: Toast.makeText(context, "Location not available", Toast.LENGTH_SHORT).show()
            }) {
                Text(text = "Center on Me")
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            
            Box(
                modifier = Modifier
                    .background(Color.White, shape = CircleShape)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = { mapViewRef.value?.controller?.zoomIn() },
                    modifier = Modifier.size(35.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ZoomIn,
                        contentDescription = "Zoom In",
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            Spacer(Modifier.size(8.dp))

            
            Box(
                modifier = Modifier
                    .background(Color.White, shape = CircleShape)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = { mapViewRef.value?.controller?.zoomOut() },
                    modifier = Modifier.size(35.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ZoomOut,
                        contentDescription = "Zoom Out",
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}


private fun getCurrentLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location?) -> Unit
) {
    if (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            onLocationReceived(location)
        }.addOnFailureListener {
            Log.e("MapScreen", "Error fetching location: ${it.message}")
            onLocationReceived(null)
        }
    } else {
        Log.e("MapScreen", "Permission not granted for location")
        onLocationReceived(null)
    }
}

