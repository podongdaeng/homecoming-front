package com.example.homecomingfront

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebSettings
import android.widget.ImageButton
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        // Enable JavaScript
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
//        webSettings.allowFileAccess = true

        // Load the OpenLayers map in the WebView
        webView.loadUrl("file:///android_asset/openlayers.html") // "file:///android_asset/openlayers.html" ?


        //intent를 사용하여 설정 화면으로 전환하기
        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

    }
}
