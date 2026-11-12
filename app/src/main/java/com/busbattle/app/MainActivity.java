package com.busbattle.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/** Full-screen game page with the TikTok LIVE connection built in: no Termux, no browser, no bridge. */
public class MainActivity extends Activity {

    private static final int PICK_FILE = 7;

    private WebView web;
    private SharedPreferences prefs;
    private TikTokLive live;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean pageReady;
    private String lastStatus = "{\"type\":\"status\",\"ok\":false,\"text\":\"Escribe tu usuario de TikTok en Conexión\"}";
    private final ArrayDeque<String> logLines = new ArrayDeque<>();
    private ValueCallback<Uri[]> fileCallback;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28)
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        prefs = getSharedPreferences("bus", MODE_PRIVATE);

        web = new WebView(this);
        web.setBackgroundColor(0xff070b18);
        setContentView(web);
        immersive();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(), "BusApp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (ActivityNotFoundException ignored) {}
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
                try { startActivityForResult(Intent.createChooser(i, "Imagen"), PICK_FILE); }
                catch (ActivityNotFoundException e) { fileCallback = null; return false; }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");

        if (!prefs.getString("user", "").isEmpty()) startLive();
    }

    // ------------------------------------------------------------ TikTok connection

    private synchronized void startLive() {
        if (live != null) live.stop();
        String user = prefs.getString("user", ""), key = prefs.getString("key", "");
        if (TikTokLive.normalize(user).isEmpty()) { live = null; return; }
        log("Usuario @" + TikTokLive.normalize(user) + (key.isEmpty() ? ", sin clave Euler" : ", con clave Euler"));
        live = new TikTokLive(user, key, new TikTokLive.Listener() {
            @Override public void onEvent(String json) {
                if (json.contains("\"type\":\"status\"")) lastStatus = json;
                js("window.__bus&&window.__bus(" + safe(json) + ")");
            }
            @Override public void onLog(String line) { log(line); }
        });
        live.start();
    }

    private void log(String line) {
        String l = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date()) + "  " + line;
        synchronized (logLines) {
            logLines.addLast(l);
            while (logLines.size() > 200) logLines.removeFirst();
        }
        js("window.__busLog&&window.__busLog(" + safe(JSONObject.quote(l)) + ")");
    }

    /** JSON is JavaScript except for these two line separators. */
    private static String safe(String json) { return json.replace("\u2028", "\\u2028").replace("\u2029", "\\u2029"); }

    private void js(final String code) {
        if (!pageReady) return;
        ui.post(() -> { if (web != null) web.evaluateJavascript(code, null); });
    }

    final class Bridge {
        @JavascriptInterface public String getUser() { return prefs.getString("user", ""); }
        @JavascriptInterface public String getKey() { return prefs.getString("key", ""); }

        @JavascriptInterface public void connect(String user, String key) {
            prefs.edit().putString("user", TikTokLive.normalize(user)).putString("key", key == null ? "" : key.trim()).apply();
            synchronized (logLines) { logLines.clear(); }
            startLive();
        }

        /** The page is listening: replay the current status and the log. */
        @JavascriptInterface public void ready() {
            pageReady = true;
            StringBuilder b = new StringBuilder("window.__bus&&window.__bus(" + safe(lastStatus) + ");");
            synchronized (logLines) {
                for (String l : logLines) b.append("window.__busLog&&window.__busLog(").append(safe(JSONObject.quote(l))).append(");");
            }
            final String code = b.toString();
            ui.post(() -> { if (web != null) web.evaluateJavascript(code, null); });
        }
    }

    // ------------------------------------------------------------ activity plumbing

    private void immersive() {
        web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK_FILE || fileCallback == null) return;
        Uri u = res == RESULT_OK && data != null ? data.getData() : null;
        fileCallback.onReceiveValue(u == null ? null : new Uri[]{u});
        fileCallback = null;
    }

    @Override
    public void onBackPressed() {
        // Close the settings sheet first; leave the app only from the game screen.
        web.evaluateJavascript("(function(){var s=document.getElementById('sheet');if(s&&!s.hidden){document.getElementById('close').click();return 1}return 0})()",
                v -> { if (!"1".equals(v)) MainActivity.super.onBackPressed(); });
    }

    @Override
    protected void onDestroy() {
        if (live != null) live.stop();
        live = null;
        pageReady = false;
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
