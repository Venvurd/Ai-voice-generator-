package com.example

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

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
                        
                        // Register Javascript Interface for native downloads
                        addJavascriptInterface(WebAppInterface(this@MainActivity), "AndroidInterface")
                        
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

class WebAppInterface(private val activity: Activity) {

    @JavascriptInterface
    fun downloadWav(base64Data: String, fileName: String) {
        activity.runOnUiThread {
            try {
                // Decode base64 data
                val cleanBase64 = if (base64Data.contains(",")) {
                    base64Data.split(",")[1]
                } else {
                    base64Data
                }
                val audioBytes = Base64.decode(cleanBase64, Base64.DEFAULT)

                // Save to downloads directory
                val savedUri = saveWavFile(audioBytes, fileName)
                
                if (savedUri != null) {
                    Toast.makeText(activity, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
                    // Prompt user with Share sheet
                    shareAudioFile(savedUri)
                } else {
                    Toast.makeText(activity, "Failed to save file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(activity, "Error saving audio: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveWavFile(bytes: ByteArray, name: String): Uri? {
        val resolver = activity.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                }
                return uri
            }
        } else {
            // Fallback for older APIs (Android 9 and below)
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, name)
                FileOutputStream(file).use { out ->
                    out.write(bytes)
                }
                // Return FileProvider URI for sharing
                return FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun shareAudioFile(uri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(shareIntent, "Save or Share WAV Audio"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
