package com.example.helloworld

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class EventMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_map)

        // Request location permissions if not granted
        requestLocationPermissions()

        // Initialize osmdroid configuration
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))

        // Initialize the MapView
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Set up the location overlay
        locationOverlay = MyLocationNewOverlay(mapView)
        locationOverlay.enableMyLocation()
        mapView.overlays.add(locationOverlay)

        // Get event details from intent
        val eventName = intent.getStringExtra("eventName") ?: "Event"
        val eventVenue = intent.getStringExtra("eventVenue") ?: "Venue"
        val eventLatitude = intent.getDoubleExtra("eventLatitude", 48.8583)
        val eventLongitude = intent.getDoubleExtra("eventLongitude", 2.2944)

        // Add event marker
        addEventMarker(eventLatitude, eventLongitude, "$eventName at $eventVenue")

        // Set the map center and zoom level
        val eventPoint = GeoPoint(eventLatitude, eventLongitude)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(eventPoint)

        // Set up the show location button
        findViewById<Button>(R.id.showLocationButton).setOnClickListener {
            // Center on current location if available
            locationOverlay.myLocation?.let { location ->
                mapView.controller.animateTo(location)
                mapView.controller.setZoom(15.0)
            }
        }
    }

    private fun addEventMarker(latitude: Double, longitude: Double, title: String) {
        val eventPoint = GeoPoint(latitude, longitude)
        val marker = Marker(mapView)
        marker.position = eventPoint
        marker.title = title
        marker.snippet = "Click to view details"
        mapView.overlays.add(marker)
    }

    private fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}