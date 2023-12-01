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
import org.json.JSONObject
import android.util.Log

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

        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        val locationButton: ImageButton = findViewById(R.id.imageButton_location)
        locationButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    fetchAndDisplayLocationData(lastKnownLocation.latitude, lastKnownLocation.longitude) // 좌표 보내주고, 받아오고
                    addLocationToMap(lastKnownLocation.latitude, lastKnownLocation.longitude) //
                    locationManager.removeUpdates(locationListener) // Stop updates after sending the location
                } else {
                    Toast.makeText(this, "No location detected", Toast.LENGTH_SHORT).show()
                }
            } else {
                requestLocationPermission()
            }
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun requestLocationPermission() {
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

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val lat = location.latitude
            val lng = location.longitude

            // Update the WebView with the current location icon.
            webView.loadUrl("javascript:addLocationIcon($lat, $lng)")
            // Do not remove updates here
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onPause() {
        super.onPause()
        locationManager.removeUpdates(locationListener)
    }

    // Retrofit API interface
    interface ApiService {
        @GET("/near-station") // Assuming you have the correct endpoint here
        fun getLocationData(
            @Query("gps_lati") gps_Lati: Double,
            @Query("gps_long") gps_Long: Double
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

        RetrofitClient.instance.getLocationData(lat, lng)
            .enqueue(object : Callback<List<LocationData>> {
                override fun onResponse(call: Call<List<LocationData>>, response: Response<List<LocationData>>) {
                    if (response.isSuccessful) {
                        response.body()?.forEach { locationData ->
                            addLocationToMap(locationData.latitude, locationData.longitude)
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Error fetching data", Toast.LENGTH_SHORT).show()
                        //Log.e("API Error", "Response Code: " + response.code() + " - " + response.errorBody()?.string())
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
        val name: String,
        val latitude: Double,
        val longitude: Double
    )
}