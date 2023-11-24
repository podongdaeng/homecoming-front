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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var locationManager: LocationManager

    private val LOCATION_PERMISSION_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/openlayers.html")

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Setup the settings button
        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        requestLocationPermissionOrObtaionLocationIfGranted()

        val locationButton: ImageButton = findViewById(R.id.imageButton_location)
        locationButton.setOnClickListener {
//            println(webView.zoomIn()) // TODO: zoomIn() = false. 줌 변경이 여기선 안되나?
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    fetchAndDisplayLocationData(lastKnownLocation.latitude, lastKnownLocation.longitude)
                    locationManager.removeUpdates(locationListener) // Stop updates after sending the location
                } else {
                    Toast.makeText(this, "No location detected", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestLocationPermissionOrObtaionLocationIfGranted()
                obtainSpecificGpsAndLoadInWebView()
            }
        }
    }

    // Request location permission
    private fun requestLocationPermissionOrObtaionLocationIfGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            obtainLocationAndLoadInWebView()
        }
    }

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

    private fun obtainLocationAndLoadInWebView() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtainSpecificGpsAndLoadInWebView() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, testLocationListener)
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    // Define a location listener
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val lat = location.latitude
            val lng = location.longitude

            // Update the WebView with the current location icon.
            webView.loadUrl("javascript:addLocationIcon($lat, $lng)")
            locationManager.removeUpdates(this) // If you only need one update - TODO: 얘는 본인위치니까 계속 업데이트해야해서 필요가 없을 수 있어요
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private val testLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val lat = 37.5609
            val lng = 126.9460
            webView.loadUrl("javascript:addLocationIcon($lat, $lng)")
            locationManager.removeUpdates(this) // If you only need one update
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
        locationManager.removeUpdates(testLocationListener)
    }

    // Retrofit API interface
    interface ApiService {
        @GET("/front-test/2") // Assuming you have the correct endpoint here
        fun getLocationData(
            @Query("gps_lati_lower_left") latiLowerLeft: Double,
            @Query("gps_lati_upper_right") latiUpperRight: Double,
            @Query("gps_long_lower_left") longLowerLeft: Double,
            @Query("gps_long_upper_right") longUpperRight: Double
        ): Call<List<LocationData>>
    }

    // Retrofit Client Object
    object RetrofitClient {
        private const val BASE_URL = "http://10.0.2.2:8080/"

        val instance: ApiService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(ApiService::class.java)
        }
    }

    // Fetch and display location data
    private fun fetchAndDisplayLocationData(lat: Double, lng: Double) {
        val latiUpperRight = lat + 0.01 // Adjust the logic as needed
        val longUpperRight = lng + 0.01 // Adjust the logic as needed

        RetrofitClient.instance.getLocationData(lat, latiUpperRight, lng, longUpperRight)
            .enqueue(object : Callback<List<LocationData>> {
                override fun onResponse(call: Call<List<LocationData>>, response: Response<List<LocationData>>) {
                    if (response.isSuccessful) {
                        response.body()?.forEach { locationData ->
                            addLocationToMap(locationData.latitude, locationData.longitude)
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Error fetching data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<LocationData>>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Add location to map
    private fun addLocationToMap(lat: Double, lng: Double) {
        webView.loadUrl("javascript:addLocationIcon($lat, $lng)")
    }

    // Data model for location
    data class LocationData(
        val latitude: Double,
        val longitude: Double
    )
}
