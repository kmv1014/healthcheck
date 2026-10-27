package com.parivarhealth.app;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Parivar Health: the family health records page, bundled in assets/index.html,
 * shown full screen. The app adds what a plain web page can't do on a phone:
 * a working file picker, "Share to Parivar Health" from WhatsApp, and a bridge
 * that sends AI requests to the Claude API with the user's own key.
 */
public class MainActivity extends Activity {

    private static final int FILE_REQUEST = 1001;
    private static final int MAX_SHARE_BYTES = 15 * 1024 * 1024;
    private static final String API_URL = "https://api.anthropic.com/v1/messages";

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private boolean pageReady = false;
    private String pendingShareJs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                if (pendingShareJs != null) {
                    String js = pendingShareJs;
                    pendingShareJs = null;
                    web.evaluateJavascript(js, null);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;

                String type = "*/*";
                String[] accept = params.getAcceptTypes();
                if (accept != null) {
                    String all = String.join(",", accept).toLowerCase();
                    if (all.contains("image/")) type = "image/*";
                    else if (all.contains("pdf")) type = "application/pdf";
                }
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType(type);
                try {
                    startActivityForResult(Intent.createChooser(pick, "Choose a report"), FILE_REQUEST);
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "No app found to pick files", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

        web.loadUrl("file:///android_asset/index.html");
        handleShare(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShare(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_REQUEST) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("(window.__back && window.__back()) ? '1' : '0'", value -> {
            if (value == null || !value.contains("1")) finish();
        });
    }

    /** A PDF or photo shared from WhatsApp (or any app) into Parivar Health. */
    @SuppressWarnings("deprecation")
    private void handleShare(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        final Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri == null) return;
        final String intentType = intent.getType();
        intent.setAction(Intent.ACTION_MAIN); // don't re-import on rotation

        new Thread(() -> {
            String name = "shared-report";
            try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0 && c.getString(idx) != null) name = c.getString(idx);
                }
            } catch (Exception ignored) {
            }
            String mime = getContentResolver().getType(uri);
            if (mime == null) mime = intentType != null ? intentType : "application/octet-stream";

            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new Exception("no data");
                byte[] bytes = readBytes(is, MAX_SHARE_BYTES);
                if (bytes == null) {
                    runOnUiThread(() -> Toast.makeText(this, "That file is too large (over 15 MB)", Toast.LENGTH_LONG).show());
                    return;
                }
                String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                final String js = "window.__sharedFile(" + JSONObject.quote(name) + ","
                        + JSONObject.quote(mime) + "," + JSONObject.quote(b64) + ")";
                runOnUiThread(() -> {
                    if (pageReady) web.evaluateJavascript(js, null);
                    else pendingShareJs = js;
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Couldn't open the shared file", Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static byte[] readBytes(InputStream is, int max) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        int total = 0;
        while ((n = is.read(buf)) != -1) {
            total += n;
            if (max > 0 && total > max) return null;
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** Called from the page's JavaScript as window.AndroidBridge. */
    private class Bridge {
        @JavascriptInterface
        public void post(final String id, final String apiKey, final String body) {
            new Thread(() -> {
                boolean ok = false;
                String result;
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(API_URL).openConnection();
                    c.setRequestMethod("POST");
                    c.setConnectTimeout(30000);
                    c.setReadTimeout(180000);
                    c.setDoOutput(true);
                    c.setRequestProperty("content-type", "application/json");
                    c.setRequestProperty("x-api-key", apiKey == null ? "" : apiKey);
                    c.setRequestProperty("anthropic-version", "2023-06-01");
                    try (OutputStream os = c.getOutputStream()) {
                        os.write(body.getBytes(StandardCharsets.UTF_8));
                    }
                    int code = c.getResponseCode();
                    ok = code >= 200 && code < 300;
                    InputStream is = ok ? c.getInputStream() : c.getErrorStream();
                    if (is == null) {
                        result = "HTTP " + code;
                    } else {
                        try (InputStream in = is) {
                            result = new String(readBytes(in, 0), StandardCharsets.UTF_8);
                        }
                    }
                } catch (Exception e) {
                    result = "Couldn't reach Claude. Check your internet connection. (" + e.getMessage() + ")";
                }
                final String js = "window.__nativeReply(" + JSONObject.quote(id) + "," + ok + ","
                        + JSONObject.quote(result) + ")";
                web.post(() -> web.evaluateJavascript(js, null));
            }).start();
        }
    }
}
