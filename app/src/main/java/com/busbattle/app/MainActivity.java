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
    private LiveSource live;
    static final boolean YOUTUBE = "youtube".equals(BuildConfig.FLAVOR);
    private final Handler ui = new Handler(Looper.getMainLooper());
    private volatile boolean pageReady;
    private String lastStatus = "{\"type\":\"status\",\"ok\":false,\"text\":\"Escribe tu usuario de TikTok en Conexión\"}";
    private final ArrayDeque<String> logLines = new ArrayDeque<>();
    private ValueCallback<Uri[]> fileCallback;
    /* Messages to the page are batched: one evaluateJavascript every 60 ms instead of one per gift / tap / log line. */
    private final java.util.concurrent.ConcurrentLinkedQueue<String> outQ = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private volatile String audioMsg;
    private final java.util.concurrent.atomic.AtomicInteger queued = new java.util.concurrent.atomic.AtomicInteger();
    private final Runnable flusher = new Runnable() {
        @Override public void run() { flush(); ui.postDelayed(this, 60); }
    };
    private MusicSync music;
    private String musicSource = "output";
    private static final int REQ_AUDIO = 11;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28)
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        prefs = getSharedPreferences("bus", MODE_PRIVATE);
        highRefreshRate();
        // Keep the GPU at a steady clock during a long live (only on phones that support it).
        if (Build.VERSION.SDK_INT >= 24) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && pm.isSustainedPerformanceModeSupported()) getWindow().setSustainedPerformanceMode(true);
        }

        web = new WebView(this);
        web.setBackgroundColor(0xff070b18);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
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
        ui.postDelayed(flusher, 60);
    }

    // ------------------------------------------------------------ TikTok connection

    private synchronized void startLive() {
        if (live != null) live.stop();
        String user = prefs.getString("user", ""), key = prefs.getString("key", "");
        if (user.trim().isEmpty()) { live = null; return; }
        TikTokLive.Listener l = new TikTokLive.Listener() {
            @Override public void onEvent(String json) {
                if (json.contains("\"type\":\"status\"")) lastStatus = json;
                // a flood of taps must never delay gifts: drop taps when the page is behind
                if (queued.get() > 600 && json.contains("\"type\":\"like\"")) return;
                js("window.__bus&&window.__bus(" + safe(json) + ")");
            }
            @Override public void onLog(String line) { log(line); }
        };
        if (YOUTUBE) {
            log("Directo de YouTube: " + user.trim());
            live = new YouTubeChat(user, l);
        } else {
            log("Usuario @" + TikTokLive.normalize(user) + (key.isEmpty() ? ", sin clave Euler" : ", con clave Euler"));
            live = new TikTokLive(user, key, l);
        }
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
        outQ.add(code);
        queued.incrementAndGet();
    }

    private void flush() {
        if (!pageReady || web == null) { outQ.clear(); queued.set(0); return; }
        StringBuilder b = new StringBuilder();
        String c;
        int n = 0;
        while (n < 400 && (c = outQ.poll()) != null) { b.append(c).append(';'); n++; queued.decrementAndGet(); }
        String a = audioMsg;
        audioMsg = null;
        if (a != null) b.append(a);
        if (b.length() > 0) web.evaluateJavascript(b.toString(), null);
    }

    // ------------------------------------------------------------ music the user plays (Spotify…) → lights

    private void startMusic(String source) {
        musicSource = source;
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        if (music == null) music = new MusicSync(new MusicSync.Out() {
            @Override public void level(float b, float m, float h, boolean beat) {
                audioMsg = "window.__busAudio&&window.__busAudio(" + fmt(b) + "," + fmt(m) + "," + fmt(h) + "," + (beat ? 1 : 0) + ")";
                if (beat) js("window.__busBeat&&window.__busBeat()");
            }
            @Override public void state(String st) { js("window.__busAudioState&&window.__busAudioState(" + JSONObject.quote(st) + ")"); }
        });
        music.start(source);
    }

    private static String fmt(float v) { return String.valueOf(Math.round(v * 1000) / 1000f); }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req != REQ_AUDIO) return;
        if (res.length > 0 && res[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) startMusic(musicSource);
        else js("window.__busAudioState&&window.__busAudioState(\"denied\")");
    }

    final class Bridge {
        @JavascriptInterface public String getUser() { return prefs.getString("user", ""); }
        @JavascriptInterface public String getKey() { return prefs.getString("key", ""); }

        /** Lights follow the music the phone plays: source "output" (any app) or "mic"; on=false stops listening. */
        @JavascriptInterface public void musicSync(final boolean on, final String source) {
            ui.post(() -> { if (on) startMusic(source == null ? "output" : source); else if (music != null) music.stop(); });
        }

        /** "tiktok" or "youtube": the page adapts its labels and scoring. */
        @JavascriptInterface public String getSource() { return YOUTUBE ? "youtube" : "tiktok"; }

        /** Default teams and rules for this edition (assets/preset.json), or "". */
        @JavascriptInterface public String getPreset() {
            try (java.io.InputStream in = getAssets().open("preset.json")) {
                java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
                return o.toString("UTF-8");
            } catch (Exception e) { return ""; }
        }

        /**
         * Fetches a profile picture natively and hands it to the page as a data: URL. WebGL can only use pictures
         * that are not cross-origin, and TikTok / YouTube avatar servers do not all allow that.
         */
        @JavascriptInterface public void image(final String url, final String id) {
            if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) return;
            new Thread(() -> {
                String data = "";
                try {
                    java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 Chrome/129 Mobile Safari/537.36");
                    if (c.getResponseCode() == 200) {
                        String type = c.getContentType();
                        type = type != null && type.startsWith("image/") ? type.split(";")[0] : "image/jpeg";
                        try (java.io.InputStream in = c.getInputStream()) {
                            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
                            byte[] b = new byte[16384];
                            int n;
                            while ((n = in.read(b)) > 0 && o.size() < 3_000_000) o.write(b, 0, n);
                            data = "data:" + type + ";base64," + android.util.Base64.encodeToString(o.toByteArray(), android.util.Base64.NO_WRAP);
                        }
                    }
                    c.disconnect();
                } catch (Exception ignored) {}
                final String code = "window.__busImg&&window.__busImg(" + JSONObject.quote(id) + "," + JSONObject.quote(data) + ")";
                ui.post(() -> { if (web != null) web.evaluateJavascript(code, null); });
            }, "avatar").start();
        }

        @JavascriptInterface public void connect(String user, String key) {
            prefs.edit().putString("user", YOUTUBE ? user.trim() : TikTokLive.normalize(user)).putString("key", key == null ? "" : key.trim()).apply();
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

    /** Ask for the display's fastest mode at the current resolution (120 Hz on phones like the Honor 400). */
    private void highRefreshRate() {
        if (Build.VERSION.SDK_INT < 23) return;
        try {
            android.view.Display d = Build.VERSION.SDK_INT >= 30 ? getDisplay() : getWindowManager().getDefaultDisplay();
            if (d == null) return;
            android.view.Display.Mode cur = d.getMode(), best = cur;
            for (android.view.Display.Mode m : d.getSupportedModes())
                if (m.getPhysicalWidth() == cur.getPhysicalWidth() && m.getPhysicalHeight() == cur.getPhysicalHeight()
                        && m.getRefreshRate() > best.getRefreshRate()) best = m;
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.preferredDisplayModeId = best.getModeId();
            if (Build.VERSION.SDK_INT >= 30) lp.preferredRefreshRate = best.getRefreshRate();
            getWindow().setAttributes(lp);
        } catch (Exception ignored) {}
    }

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
        ui.removeCallbacks(flusher);
        if (music != null) music.stop();
        if (live != null) live.stop();
        live = null;
        pageReady = false;
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
