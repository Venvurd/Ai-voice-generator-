package com.example

import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read the HTML content from the assets folder
        val htmlContent = try {
            assets.open("index.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "<html><body style='background-color:#0f172a;color:#f8fafc;padding:24px;font-family:sans-serif;'>" +
                    "<h3>Error loading app asset:</h3><p>${e.message}</p></body></html>"
        }

        setContent {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {}
                        webChromeClient = WebChromeClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                        }
                        
                        // Load the HTML with a mock secure HTTPS origin to ensure all modern web APIs,
                        // localStorage, IndexedDB, and ES modules work flawlessly.
                        loadDataWithBaseURL(
                            "https://local.app/",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
