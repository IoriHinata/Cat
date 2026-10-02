package com.animalcollector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

/**
 * Deliberately thin Android host. UI and game state are local WebView assets;
 * this activity owns lifecycle, Android permissions, and asset publication only.
 */
public final class MainActivity extends AppCompatActivity {
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final String APP_URL = "https://appassets.androidplatform.net/assets/index.html";

    private WebView webView;
    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), this::sendCameraPermission);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        webView.addJavascriptInterface(new DeviceApi(), "NativeDevice");
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Do not navigate away from the origin that owns the privileged JS bridge.
                return !ASSET_HOST.equals(request.getUrl().getHost());
            }
        });
        webView.loadUrl(APP_URL);
        setContentView(webView);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("NativeDevice");
            webView.destroy();
        }
        super.onDestroy();
    }

    private void requestCameraPermission() {
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            sendCameraPermission(true);
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void sendCameraPermission(boolean granted) {
        if (webView != null) {
            webView.evaluateJavascript("window.onNativeCameraPermission(" + granted + ");", null);
        }
    }

    /** The entire trusted JavaScript-to-device API surface. */
    public final class DeviceApi {
        @JavascriptInterface
        public void requestCameraPermission() {
            runOnUiThread(MainActivity.this::requestCameraPermission);
        }
    }
}
