package ru.svetok.app.ui.map

import android.graphics.Color
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Polyline
import ru.svetok.app.data.geo.StreetFeature
import ru.svetok.app.data.outage.OutageMapStatus

private val SVETLOGRAD_CENTER = GeoPoint(45.3330, 42.8575)
private val SVETLOGRAD_BOUNDS = BoundingBox(45.38, 42.92, 45.28, 42.80)
private val OSM_STANDARD_TILE_SOURCE = XYTileSource(
    "OpenStreetMap",
    0,
    19,
    256,
    ".png",
    arrayOf("https://tile.openstreetmap.org/"),
    "© OpenStreetMap contributors",
)

@Composable
fun OsmMapView(
    streets: List<StreetFeature>,
    highlightByStreetNorm: Map<String, OutageMapStatus>,
    selectedStreetNorm: String?,
    onStreetTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            setTileSource(OSM_STANDARD_TILE_SOURCE)
            setMultiTouchControls(true)
            setUseDataConnection(true)
            setTilesScaledToDpi(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            controller.setZoom(14.0)
            controller.setCenter(SVETLOGRAD_CENTER)
            setScrollableAreaLimitDouble(SVETLOGRAD_BOUNDS)
            minZoomLevel = 13.0
            maxZoomLevel = 18.0
            overlays.add(CopyrightOverlay(context))
        }
    }

    LaunchedEffect(streets, highlightByStreetNorm, selectedStreetNorm) {
        mapView.overlays.removeAll { it is Polyline }

        val sortedStreets = streets.sortedBy { street ->
            when {
                street.streetNorm == selectedStreetNorm -> 3
                highlightByStreetNorm[street.streetNorm] == OutageMapStatus.ACTIVE -> 2
                highlightByStreetNorm[street.streetNorm] == OutageMapStatus.PLANNED -> 1
                else -> 0
            }
        }

        sortedStreets.forEach { street ->
            val status = highlightByStreetNorm[street.streetNorm]
            val visualStyle = visualStyleFor(
                status = status,
                isSelected = street.streetNorm == selectedStreetNorm,
            )

            street.segments.forEach { segment ->
                val polyline = Polyline(mapView).apply {
                    outlinePaint.color = visualStyle.color
                    outlinePaint.strokeWidth = visualStyle.strokeWidth
                    setPoints(segment.map { GeoPoint(it.lat, it.lon) })
                    title = street.displayName

                    if (status != null) {
                        setOnClickListener { _, _, _ ->
                            onStreetTap(street.streetNorm)
                            true
                        }
                    }
                }
                mapView.overlays.add(polyline)
            }
        }

        mapView.postInvalidate()
        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
    )
}

private fun visualStyleFor(
    status: OutageMapStatus?,
    isSelected: Boolean,
): StreetVisualStyle =
    when (status) {
        OutageMapStatus.PLANNED -> StreetVisualStyle(
            color = Color.parseColor(if (isSelected) "#F29B1D" else "#D47A00"),
            strokeWidth = if (isSelected) 12f else 10f,
        )

        OutageMapStatus.ACTIVE -> StreetVisualStyle(
            color = Color.parseColor(if (isSelected) "#D83B2E" else "#C53A2D"),
            strokeWidth = if (isSelected) 13f else 10f,
        )

        null -> StreetVisualStyle(
            color = Color.argb(120, 145, 117, 72),
            strokeWidth = 5f,
        )
    }

private data class StreetVisualStyle(
    val color: Int,
    val strokeWidth: Float,
)
