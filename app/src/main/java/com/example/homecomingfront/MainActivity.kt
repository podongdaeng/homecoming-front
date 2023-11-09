package com.example.homecomingfront

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var locationManager: LocationManager

    private val LOCATION_PERMISSION_REQUEST_CODE = 1234 // Location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        // Enable JavaScript for the WebView
        webView.settings.javaScriptEnabled = true

        // Load the local HTML file into the WebView
        webView.loadUrl("file:///android_asset/openlayers.html")

        // Setup the settings button
        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Setup the location button
        val locationButton: ImageButton = findViewById(R.id.imageButton_location)
        locationButton.setOnClickListener {
            requestLocationPermission()
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    // Request location permission
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            obtainLocationAndLoadInWebView()
        }
    }

    // Handle permission request response
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                obtainLocationAndLoadInWebView()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Obtain location and load into WebView
    private fun obtainLocationAndLoadInWebView() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    // Define a location listener
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val lat = location.latitude
            val lng = location.longitude
            webView.loadUrl("javascript:addLocationIcon($lat, $lng)")
            locationManager.removeUpdates(this) // If you only need one update
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    // Make sure to remove location updates when the activity is paused or stopped
    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
    }
}
