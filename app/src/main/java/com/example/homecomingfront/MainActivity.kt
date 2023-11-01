package com.example.homecomingfront

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.ViewOverlay
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import org.osmdroid.views.overlay.GroundOverlay
import org.osmdroid.views.overlay.Overlay
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.util.Base64

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    @RequiresApi(Build.VERSION_CODES.O) // TODO: API 버전 자체를 올려야할수도
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("www.safemap.go.kr/openApiService/wms/getLayerData.do?apikey=0B3O17CH-0B3O-0B3O-0B3O-0B3O17CHIK") // Replace with your WMS server URL
            .build()
        val responseBody = client.newCall(request).execute().body().toString()
        println(responseBody)
        val wmsImage = parseWMSResponse(responseBody)

        // Send a request to the WMS server to fetch an image
        val overlay = GroundOverlay()
        overlay.image = wmsImage // wmsImage is the bitmap obtained from the WMS response
        mapView.overlays.add(overlay)
        mapView = MapView(this)
        setContentView(mapView)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun parseWMSResponse(response: String?): Bitmap? {
    if (response == null) {
        return null
    }

    var image: Bitmap? = null

    try {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(response.reader())

        var eventType = parser.eventType
        var base64Data: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "Image") {
                        base64Data = parser.nextText()
                        break // Exit the loop after finding the image data
                    }
                }
            }
            eventType = parser.next()
        }

        if (base64Data != null) {
            val decoder = Base64.getDecoder()
            val imageData = decoder.decode(base64Data)
            image = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return image
}
