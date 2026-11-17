package com.busbattle.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

/**
 * Floating stars over any app (the game the host plays, YouTube, anything): tappers' photos float up like bubbles and
 * every gift slides in as a card with the supporter's photo, name and gift. The window never takes touches, so the game
 * below keeps working; a small ✥ handle moves it.
 */
final class FloatOverlay {
    private static FloatOverlay inst;
    static synchronized FloatOverlay get(Context c) { if (inst == null) inst = new FloatOverlay(c.getApplicationContext()); return inst; }

    private final Context ctx;
    private final WindowManager wm;
    private final SharedPreferences prefs;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private WebView web;
    private TextView handle;
    private WindowManager.LayoutParams lp, hp;
    private volatile boolean shown, ready;
    private String opts = "{}";
    private final java.util.concurrent.ConcurrentLinkedQueue<String> q = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final Runnable flusher = new Runnable() { @Override public void run() { flush(); if (shown) ui.postDelayed(this, 80); } };

    private FloatOverlay(Context c) {
        ctx = c;
        wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        prefs = c.getSharedPreferences("bus", Context.MODE_PRIVATE);
    }

    static boolean allowed(Context c) { return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c); }
    boolean isShown() { return shown; }

    private int dp(float v) { return Math.round(v * ctx.getResources().getDisplayMetrics().density); }
    private int type() { return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    void show(String options) {
        if (options != null) opts = options;
        if (shown) { push("window.__floatCfg&&window.__floatCfg(" + opts + ")"); return; }
        if (!allowed(ctx)) return;
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        float scale = 1f;
        try { scale = (float) new org.json.JSONObject(opts).optDouble("size", 1.0); } catch (Exception ignored) {}
        int w = dp(210 * scale), h = Math.min(dm.heightPixels * 2 / 3, dp(430 * scale));

        web = new WebView(ctx);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new android.webkit.WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                ready = true;
                v.evaluateJavascript("window.__floatCfg&&window.__floatCfg(" + opts + ")", null);
            }
        });
        web.loadUrl("file:///android_asset/float.html");

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        lp = new WindowManager.LayoutParams(w, h, type(), flags, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = prefs.getInt("float_x", dm.widthPixels - w - dp(8));
        lp.y = prefs.getInt("float_y", dm.heightPixels / 6);
        clampPos(dm);
        if (Build.VERSION.SDK_INT >= 31) lp.alpha = 0.8f;   // Android 12+ only lets touches pass through a window up to 80% opaque
        wm.addView(web, lp);

        /* the small handle: drag to move the stars, double tap to hide them for a moment */
        handle = new TextView(ctx);
        handle.setText("✥");
        handle.setTextColor(Color.WHITE);
        handle.setTextSize(13);
        handle.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0x66140d2a);
        bg.setStroke(dp(1), 0x55ffffff);
        handle.setBackground(bg);
        hp = new WindowManager.LayoutParams(dp(28), dp(28), type(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        hp.gravity = Gravity.TOP | Gravity.START;
        placeHandle();
        handle.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; boolean moved; long lastUp;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: sx = e.getRawX(); sy = e.getRawY(); ox = lp.x; oy = lp.y; moved = false; return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - sx, dy = e.getRawY() - sy;
                        if (!moved && Math.hypot(dx, dy) < dp(6)) return true;
                        moved = true; lp.x = ox + (int) dx; lp.y = oy + (int) dy; clampPos(ctx.getResources().getDisplayMetrics());
                        try { wm.updateViewLayout(web, lp); } catch (Exception ignored) {}
                        placeHandle(); return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) { prefs.edit().putInt("float_x", lp.x).putInt("float_y", lp.y).apply(); return true; }
                        long now = System.currentTimeMillis();
                        if (now - lastUp < 350) { web.setVisibility(web.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE); lastUp = 0; }
                        else lastUp = now;
                        return true;
                }
                return false;
            }
        });
        wm.addView(handle, hp);
        shown = true;
        prefs.edit().putBoolean("float_on", true).apply();
        ui.post(flusher);
    }

    private void clampPos(DisplayMetrics dm) {
        lp.x = Math.max(-lp.width / 2, Math.min(dm.widthPixels - lp.width / 2, lp.x));
        lp.y = Math.max(0, Math.min(dm.heightPixels - dp(60), lp.y));
    }
    private void placeHandle() {
        hp.x = lp.x + lp.width - dp(28);
        hp.y = lp.y;
        if (handle != null && handle.isAttachedToWindow()) try { wm.updateViewLayout(handle, hp); } catch (Exception ignored) {}
    }

    void hide() {
        if (!shown) return;
        shown = false; ready = false; q.clear();
        prefs.edit().putBoolean("float_on", false).apply();
        try { wm.removeView(handle); } catch (Exception ignored) {}
        try { wm.removeView(web); } catch (Exception ignored) {}
        if (web != null) web.destroy();
        web = null; handle = null;
    }

    /** About 20 s of made-up taps and gifts, so the host can check the stars over another app without a live. */
    void test() {
        final String[] names = {"Fatima", "Carlos", "Sara", "Youssef", "Lucia", "Omar", "Maria", "Hamza"};
        final int[] coins = {1, 1, 5, 30, 1, 99, 1, 500};
        for (int i = 0; i < 60; i++) {
            final int k = i;
            ui.postDelayed(() -> {
                try {
                    org.json.JSONObject e = new org.json.JSONObject().put("user", "test" + (k % 8)).put("nick", names[k % 8]);
                    if (k % 6 == 5) e.put("type", "gift").put("gift", "Regalo").put("coins", coins[(k / 6) % 8]).put("count", 1);
                    else e.put("type", "like").put("count", 1 + (k * 7) % 15);
                    event(e.toString());
                } catch (Exception ignored) {}
            }, 600L + i * 330L);
        }
    }

    /** A live event (same JSON as the games get). Taps are dropped first if the page falls behind. */
    void event(String json) {
        if (!shown) return;
        if (q.size() > 150 && !json.contains("\"type\":\"gift\"")) return;
        q.add("window.__bus&&window.__bus(" + json + ")");
    }
    private void push(String code) { if (shown) q.add(code); }
    private void flush() {
        if (!ready || web == null || q.isEmpty()) return;
        StringBuilder b = new StringBuilder();
        String c; int n = 0;
        while (n < 120 && (c = q.poll()) != null) { b.append(c).append(';'); n++; }
        web.evaluateJavascript(b.toString(), null);
    }
}
