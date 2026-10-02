package com.animalcollector;

import android.Manifest;
import android.graphics.Bitmap;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.ComponentActivity;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

/**
 * Deliberately thin Android host. UI and game state are local WebView assets;
 * this activity owns lifecycle, Android permissions, and asset publication only.
 */
public final class MainActivity extends ComponentActivity {
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final String APP_URL = "https://appassets.androidplatform.net/assets/index.html";

    private WebView webView;
    private boolean captureAfterPermission;
    private final ActivityResultLauncher<Void> cameraCapture = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), this::publishBitmap);
    private final ActivityResultLauncher<String> cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                sendCameraPermission(granted);
                if (granted && captureAfterPermission) {
                    captureAfterPermission = false;
                    cameraCapture.launch(null);
                }
            });
    private final ActivityResultLauncher<String> galleryPicker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::publishUri);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            createWebView();
        } catch (RuntimeException exception) {
            // A broken system WebView must not turn into an unhandled process crash.
            TextView errorView = new TextView(this);
            errorView.setText(R.string.webview_unavailable);
            errorView.setPadding(48, 48, 48, 48);
            setContentView(errorView);
        }
    }

    private void createWebView() {
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

    private void takePhoto() {
        boolean granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            cameraCapture.launch(null);
        } else {
            captureAfterPermission = true;
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void pickPhoto() {
        galleryPicker.launch("image/*");
    }

    private void publishUri(Uri uri) {
        if (uri == null) {
            publishPhoto(null);
            return;
        }
        try {
            Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            publishBitmap(bitmap);
        } catch (Exception exception) {
            publishPhoto(null);
        }
    }

    private void publishBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            publishPhoto(null);
            return;
        }
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, bytes);
            publishPhoto("data:image/jpeg;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP));
        } catch (RuntimeException exception) {
            publishPhoto(null);
        }
    }

    private void publishPhoto(String dataUrl) {
        if (webView != null) {
            String argument = dataUrl == null ? "null" : "'" + dataUrl + "'";
            webView.evaluateJavascript("window.onNativePhoto(" + argument + ");", null);
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

        @JavascriptInterface
        public void takePhoto() {
            runOnUiThread(MainActivity.this::takePhoto);
        }

        @JavascriptInterface
        public void pickPhoto() {
            runOnUiThread(MainActivity.this::pickPhoto);
        }
    }
}
