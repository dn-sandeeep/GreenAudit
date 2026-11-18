package com.sandeep.greenaudit.ui.mappark

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.sandeep.greenaudit.domain.gis.GeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

private val JAIPUR_CENTER = OsmGeoPoint(26.9124, 75.7873)

/**
 * A map the surveyor taps to drop boundary corners, with each corner shown
 * as a draggable marker so it can be adjusted afterwards. A single
 * [MapEventsOverlay] is kept at overlay index 0 for the lifetime of the
 * view so new taps keep registering; markers/shape overlays above it are
 * rebuilt on every [corners] change.
 */
@Composable
fun MapDrawingView(
    corners: List<GeoPoint>,
    onMapTapped: (GeoPoint) -> Unit,
    onCornerDragged: (Int, GeoPoint) -> Unit,
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
            controller.setZoom(17.0)
            controller.setCenter(JAIPUR_CENTER)
            overlays.add(
                MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: OsmGeoPoint): Boolean {
                            onMapTapped(GeoPoint(p.latitude, p.longitude))
                            return true
                        }

                        override fun longPressHelper(p: OsmGeoPoint): Boolean = false
                    },
                ),
            )
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { view ->
            while (view.overlays.size > 1) view.overlays.removeAt(view.overlays.size - 1)

            if (corners.size == 2) {
                view.overlays.add(
                    Polyline(view).apply {
                        setPoints(corners.map { OsmGeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = AndroidColor.parseColor("#1565C0")
                        outlinePaint.strokeWidth = 5f
                    },
                )
            } else if (corners.size >= 3) {
                view.overlays.add(
                    Polygon(view).apply {
                        points = (corners + corners.first()).map { OsmGeoPoint(it.latitude, it.longitude) }
                        fillColor = AndroidColor.argb(70, 21, 101, 192)
                        strokeColor = AndroidColor.parseColor("#1565C0")
                        strokeWidth = 5f
                    },
                )
            }

            corners.forEachIndexed { index, point ->
                view.overlays.add(
                    Marker(view).apply {
                        position = OsmGeoPoint(point.latitude, point.longitude)
                        isDraggable = true
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerDragListener(
                            object : Marker.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: Marker) = Unit
                                override fun onMarkerDragEnd(marker: Marker) {
                                    onCornerDragged(index, GeoPoint(marker.position.latitude, marker.position.longitude))
                                }
                                override fun onMarkerDragStart(marker: Marker) = Unit
                            },
                        )
                    },
                )
            }

            view.invalidate()
        },
    )
}
