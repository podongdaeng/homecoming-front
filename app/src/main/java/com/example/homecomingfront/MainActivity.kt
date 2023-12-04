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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.telephony.SmsManager
import android.util.Log
import org.json.JSONObject
import android.view.View

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var webView: WebView
    private lateinit var locationManager: LocationManager

    private val LOCATION_PERMISSION_REQUEST_CODE = 1234


    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastUpdate: Long = 0
    private var last_x: Float = 0.0f
    private var last_y: Float = 0.0f
    private var last_z: Float = 0.0f
    private val SHAKE_THRESHOLD = 800 // 흔들림 감지 임계값

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
                    fetchAndDisplayLocationData_terror(lastKnownLocation.latitude, lastKnownLocation.longitude)
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

        // 가속도 센서 리스너 설정
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }


    override fun onSensorChanged(event: SensorEvent) {
        val sensor = event.sensor
        if (sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > 100) {
                val diffTime = currentTime - lastUpdate
                lastUpdate = currentTime

                val speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000

                if (speed > SHAKE_THRESHOLD) {
                    // 흔들림 감지됨
                    if (isThreatAlertEnabled()) {
                        sendSmsMessage("피보호자 위협 알림!")
                    }
                }

                last_x = x
                last_y = y
                last_z = z
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // 센서 정확도 변경 시 처리
    }

    private fun isThreatAlertEnabled(): Boolean {
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("ThreatAlarmEnabled", false)
        // SharedPreferences 또는 다른 방법으로 설정 상태 확인
        return true // 여기서는 예시로 항상 true를 반환
    }

    private fun sendSmsMessage(message: String) {
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val phoneNumber = sharedPref.getString("GuardianPhoneNumber", null)
        phoneNumber?.let {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
        }
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


    // Retrofit API interface
    interface ApiService {
        @GET("/near-station") // Assuming you have the correct endpoint here
        fun getLocationData(
            @Query("gps_lati") gps_Lati: Double,
            @Query("gps_long") gps_Long: Double
        ): Call<List<LocationData>>
    }

    interface ApiService_terror {
        @GET("/near-threat") // Assuming you have the correct endpoint here
        fun getLocationData_terror(
            @Query("gps_lati") gps_Lati: Double,
            @Query("gps_long") gps_Long: Double
        ): Call<List<LocationData>>
    }

    // Retrofit Client Object
    object RetrofitClient {
        private const val BASE_URL = "http://10.0.2.2:8080/"
        private const val BASE_URL_terror = "http://10.0.2.2:8080/"

        val instance: ApiService by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(ApiService::class.java)
        }

        val instance_terror: ApiService_terror by lazy {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL_terror)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            retrofit.create(ApiService_terror::class.java)
        }
    }

    // Fetch and display location data
    private fun fetchAndDisplayLocationData(lat: Double, lng: Double) {

        RetrofitClient.instance.getLocationData(lat, lng)
            .enqueue(object : Callback<List<LocationData>> {
                override fun onResponse(call: Call<List<LocationData>>, response: Response<List<LocationData>>) {
                    if (response.isSuccessful) {
                        response.body()?.forEach { locationData ->
                            addBusToMap(locationData.latitude, locationData.longitude)
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

    private fun fetchAndDisplayLocationData_terror(lat: Double, lng: Double) {

        RetrofitClient.instance_terror.getLocationData_terror(lat, lng)
            .enqueue(object : Callback<List<LocationData>> {
                override fun onResponse(call: Call<List<LocationData>>, response: Response<List<LocationData>>) {
                    if (response.isSuccessful) {
                        response.body()?.forEach { locationData ->
                            addTerrorToMap(locationData.latitude, locationData.longitude)
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

    private fun addBusToMap(lat: Double, lng: Double) {
        webView.loadUrl("javascript:addIcon_bus($lat, $lng)")
    }
    private fun addTerrorToMap(lat: Double, lng: Double) {
        webView.loadUrl("javascript:addIcon_terror($lat, $lng)")
    }


    // Data model for location
    data class LocationData(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )

    override fun onResume() {
        super.onResume()
        if (isThreatAlertEnabled()) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        if (isThreatAlertEnabled()) {
            sensorManager.unregisterListener(this)
        }
        locationManager.removeUpdates(locationListener)
    }

}