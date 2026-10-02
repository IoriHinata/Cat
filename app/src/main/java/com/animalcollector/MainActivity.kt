package com.animalcollector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * Deliberately thin Android host. Game state and UI live in the WebView assets;
 * this activity owns only Android lifecycle, local asset publication and device APIs.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        sendCameraPermission(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            addJavascriptInterface(DeviceApi(), "NativeDevice")
            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ) = assetLoader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // Keep the privileged JavaScript bridge on its trusted asset origin.
                    return request.url.host != ASSET_HOST
                }
            }
            loadUrl(APP_URL)
        }
        setContentView(webView)
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface("NativeDevice")
        webView.destroy()
        super.onDestroy()
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            sendCameraPermission(true)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun sendCameraPermission(granted: Boolean) {
        webView.evaluateJavascript("window.onNativeCameraPermission(${granted});", null)
    }

    inner class DeviceApi {
        @JavascriptInterface
        fun requestCameraPermission() = runOnUiThread { requestCameraPermission() }
    }

    private companion object {
        const val ASSET_HOST = "appassets.androidplatform.net"
        const val APP_URL = "https://appassets.androidplatform.net/assets/index.html"
    }
}
