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
    private static final int REQ_CAM = 12;
    private android.webkit.PermissionRequest camRequest;
    private boolean pickingVideo;
    /* the sound of the host's "busy" video, played natively WITHOUT asking for audio focus, so Spotify keeps playing alongside */
    private android.media.MediaPlayer busyPlayer;

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
            /* the host's own camera bubble in the hall: the page asks for the front camera (video only) */
            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest req) {
                ui.post(() -> {
                    boolean video = false;
                    for (String r : req.getResources()) if (android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) video = true;
                    if (!video) { req.deny(); return; }
                    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        if (camRequest != null) camRequest.deny();
                        camRequest = req;
                        requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQ_CAM);
                        return;
                    }
                    req.grant(new String[]{android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                });
            }
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                String type = "image/*";
                for (String t : p.getAcceptTypes()) if (t != null && t.startsWith("video")) type = "video/*";
                pickingVideo = type.startsWith("video");
                Intent i = new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType(type);
                try { startActivityForResult(Intent.createChooser(i, type.startsWith("video") ? "Vídeo" : "Imagen"), PICK_FILE); }
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
                if (queued.get() > 300 && json.contains("\"type\":\"join\"")) return;
                if (queued.get() > 600 && json.contains("\"type\":\"like\"")) return;
                js("window.__bus&&window.__bus(" + safe(json) + ")");
                if (!json.contains("\"type\":\"status\"")) FloatOverlay.get(MainActivity.this).event(safe(json));
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
        if (req == REQ_CAM) {
            if (camRequest != null) {
                if (res.length > 0 && res[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) camRequest.grant(new String[]{android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                else camRequest.deny();
                camRequest = null;
            }
            return;
        }
        if (req != REQ_AUDIO) return;
        if (res.length > 0 && res[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) startMusic(musicSource);
        else js("window.__busAudioState&&window.__busAudioState(\"denied\")");
    }

    final class Bridge {
        @JavascriptInterface public String getUser() { return prefs.getString("user", ""); }
        /** True while another app (Spotify, YouTube Music…) is playing music, so the hall never starts sounds that would pause it. */
        @JavascriptInterface public boolean otherMusic() {
            try { android.media.AudioManager am = (android.media.AudioManager) getSystemService(AUDIO_SERVICE); return am != null && am.isMusicActive(); } catch (Exception e) { return false; }
        }
        @JavascriptInterface public String getKey() { return prefs.getString("key", ""); }

        /** Lights follow the music the phone plays: source "output" (any app) or "mic"; on=false stops listening. */
        @JavascriptInterface public void musicSync(final boolean on, final String source) {
            ui.post(() -> { if (on) startMusic(source == null ? "output" : source); else if (music != null) music.stop(); });
        }

        /** Saves the thank-you poster (data:image/png) to Pictures/BusBattle and optionally opens the share sheet. */
        @JavascriptInterface public void savePoster(final String dataUrl, final boolean share) {
            new Thread(() -> {
                try {
                    String b64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
                    byte[] png = android.util.Base64.decode(b64, android.util.Base64.DEFAULT);
                    String name = "top10_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".png";
                    // MediaStore on every version: a content:// link that other apps can read (file:// shares crash on Android 7+)
                    android.content.ContentValues v = new android.content.ContentValues();
                    v.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
                    v.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png");
                    if (Build.VERSION.SDK_INT >= 29) v.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/BusBattle");
                    Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) throw new IllegalStateException("no uri");
                    try (java.io.OutputStream o = getContentResolver().openOutputStream(uri)) { if (o == null) throw new IllegalStateException("no stream"); o.write(png); }
                    final Uri u = uri;
                    ui.post(() -> {
                        android.widget.Toast.makeText(MainActivity.this, "Guardado en Fotos › BusBattle", android.widget.Toast.LENGTH_SHORT).show();
                        if (share && u != null) {
                            Intent i = new Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, u).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try { startActivity(Intent.createChooser(i, "Compartir")); } catch (Exception ignored) {}
                        }
                    });
                } catch (Exception e) {
                    ui.post(() -> android.widget.Toast.makeText(MainActivity.this, "No se pudo guardar: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show());
                }
            }, "poster").start();
        }

        /** "tiktok" or "youtube": the page adapts its labels and scoring. */
        /** Floating stars over any app: on=true shows them (asks Android for the "display over other apps" permission the first time). */
        @JavascriptInterface public String floatShow(final boolean on, final String opts) {
            if (on && !FloatOverlay.allowed(MainActivity.this)) {
                prefs.edit().putString("float_opts", opts == null ? "{}" : opts).putBoolean("float_want", true).apply();
                ui.post(() -> {
                    try { startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
                    catch (Exception e) { try { startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)); } catch (Exception ignored) {} }
                });
                return "noperm";
            }
            prefs.edit().putString("float_opts", opts == null ? "{}" : opts).putBoolean("float_want", on).apply();
            ui.post(() -> { if (on) FloatOverlay.get(MainActivity.this).show(opts); else FloatOverlay.get(MainActivity.this).hide(); });
            return on ? "on" : "off";
        }
        @JavascriptInterface public boolean hasBusyAudio() { return busyFile().exists(); }
        @JavascriptInterface public void busyAudio(final boolean on, final double vol, final double atSec) { ui.post(() -> busyAudio(on, (float) vol, (float) atSec)); }
        @JavascriptInterface public void busyDelete() { ui.post(() -> { stopBusyAudio(); //noinspection ResultOfMethodCallIgnored
            busyFile().delete(); }); }
        @JavascriptInterface public void floatTest() { ui.post(() -> FloatOverlay.get(MainActivity.this).test()); }
        @JavascriptInterface public String floatState() {
            if (FloatOverlay.get(MainActivity.this).isShown()) return "on";
            return FloatOverlay.allowed(MainActivity.this) ? "off" : "noperm";
        }

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
        if (u != null && pickingVideo) copyBusyVideo(u);
        fileCallback.onReceiveValue(u == null ? null : new Uri[]{u});
        fileCallback = null;
    }

    private java.io.File busyFile() { return new java.io.File(getFilesDir(), "busy_video"); }
    private void copyBusyVideo(final Uri u) {
        stopBusyAudio();
        new Thread(() -> {
            java.io.File tmp = new java.io.File(getFilesDir(), "busy_video.part");
            try (java.io.InputStream in = getContentResolver().openInputStream(u); java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
                byte[] buf = new byte[65536]; int n;
                while (in != null && (n = in.read(buf)) > 0) out.write(buf, 0, n);
            } catch (Exception e) { tmp.delete(); log("No se pudo copiar el vídeo: " + e); return; }
            //noinspection ResultOfMethodCallIgnored
            busyFile().delete();
            boolean ok = tmp.renameTo(busyFile());
            log(ok ? "Vídeo listo para sonar junto a Spotify (" + (busyFile().length() / 1024) + " KB)" : "No se pudo guardar el vídeo");
        }, "copy-video").start();
    }
    private void stopBusyAudio() {
        if (busyPlayer != null) { try { busyPlayer.stop(); } catch (Exception ignored) {} busyPlayer.release(); busyPlayer = null; }
    }
    /** Volume 0..1; 0 or off stops it. Started at the page's position so it matches the picture. No audio focus request on purpose. */
    private void busyAudio(boolean on, float vol, float atSec) {
        if (!on || vol <= 0 || !busyFile().exists()) { stopBusyAudio(); return; }
        try {
            if (busyPlayer == null) {
                busyPlayer = new android.media.MediaPlayer();
                busyPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE).build());
                /* the media player runs in another process that cannot open the app's private files by path: hand it an open file */
                try (java.io.FileInputStream fis = new java.io.FileInputStream(busyFile())) { busyPlayer.setDataSource(fis.getFD()); }
                busyPlayer.setLooping(true);
                busyPlayer.prepare();
                busyPlayer.seekTo(Math.max(0, (int) (atSec * 1000)));
                busyPlayer.start();
            } else if (atSec >= 0 && Math.abs(busyPlayer.getCurrentPosition() - atSec * 1000) > 400) busyPlayer.seekTo((int) (atSec * 1000));
            busyPlayer.setVolume(vol, vol);
        } catch (Exception e) { log("Sonido del vídeo: " + e); stopBusyAudio(); }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopBusyAudio();   // the bubble is only seen while the app is on screen
    }

    @Override
    public void onBackPressed() {
        // Close the settings sheet first; leave the app only from the game screen.
        web.evaluateJavascript("(function(){var s=document.getElementById('sheet');if(s&&!s.hidden){document.getElementById('close').click();return 1}return 0})()",
                v -> { if (!"1".equals(v)) MainActivity.super.onBackPressed(); });
    }

    /* back from Android's "display over other apps" screen (or a new start): bring the floating stars back if they were on */
    @Override
    protected void onResume() {
        super.onResume();
        if (prefs.getBoolean("float_want", false) && FloatOverlay.allowed(this) && !FloatOverlay.get(this).isShown())
            FloatOverlay.get(this).show(prefs.getString("float_opts", "{}"));
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacks(flusher);
        FloatOverlay.get(this).hide();
        stopBusyAudio();
        if (music != null) music.stop();
        if (live != null) live.stop();
        live = null;
        pageReady = false;
        if (web != null) { web.destroy(); web = null; }
        super.onDestroy();
    }
}
