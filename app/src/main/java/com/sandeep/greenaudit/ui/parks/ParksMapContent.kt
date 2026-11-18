package com.sandeep.greenaudit.ui.parks

import android.content.Context
import android.graphics.Color as AndroidColor
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import com.sandeep.greenaudit.data.local.entity.ParkEntity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private val JAIPUR_CENTER = OsmGeoPoint(26.9124, 75.7873)

@Composable
fun ParksMapContent(
    parks: List<ParkEntity>,
    userLocation: Location?,
    selectedParkId: String?,
    onParkTapped: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE),
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(JAIPUR_CENTER)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { view ->
                view.overlays.clear()

                parks.forEach { park ->
                    if (park.boundary.size < 3) return@forEach
                    val isSelected = park.id == selectedParkId
                    val polygon = Polygon(view).apply {
                        points = park.boundary.map { OsmGeoPoint(it.latitude, it.longitude) }
                        fillColor = ColorUtils.setAlphaComponent(
                            park.status.toColor().toArgb(),
                            if (isSelected) 120 else 70,
                        )
                        strokeColor = if (isSelected) AndroidColor.BLACK else park.status.toColor().toArgb()
                        strokeWidth = if (isSelected) 7f else 3f
                        setOnClickListener { _, _, _ -> onParkTapped(park.id); true }
                    }
                    view.overlays.add(polygon)
                }

                userLocation?.let { location ->
                    val marker = Marker(view).apply {
                        position = OsmGeoPoint(location.latitude, location.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "You"
                    }
                    view.overlays.add(marker)
                }

                view.invalidate()
            },
        )

        FloatingActionButton(
            onClick = {
                userLocation?.let { location ->
                    mapView.controller.animateTo(OsmGeoPoint(location.latitude, location.longitude))
                    mapView.controller.setZoom(17.0)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "My location")
        }
    }
}
