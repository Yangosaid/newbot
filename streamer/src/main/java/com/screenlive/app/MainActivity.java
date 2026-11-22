package com.screenlive.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

/** One screen: pick the platform, paste the stream key, choose quality / orientation / sound, and go live. */
public class MainActivity extends Activity {

    private static final int REQ_PERMS = 1, REQ_CAPTURE = 2;

    /* platform: name, default server, where to find the key */
    private static final String[][] PLATFORMS = {
            {"YouTube", "rtmp://a.rtmp.youtube.com/live2", "YouTube Studio ← «بث مباشر» (Go live) ← «البث» (Stream) ← انسخ «مفتاح البث». أول مرة قد يحتاج يوتيوب 24 ساعة لتفعيل البث.", "https://studio.youtube.com"},
            {"Facebook", "rtmps://live-api-s.facebook.com:443/rtmp/", "Facebook ← Live Producer ← «برنامج البث» (Streaming software) ← انسخ «مفتاح البث». فعّل «مفتاح بث دائم» حتى لا تنسخه كل مرة.", "https://www.facebook.com/live/producer"},
            {"Twitch", "rtmp://live.twitch.tv/app", "Twitch ← لوحة صانع المحتوى ← الإعدادات ← البث ← انسخ «مفتاح البث الأساسي».", "https://dashboard.twitch.tv/settings/stream"},
            {"Kick", "rtmps://fa723fc1b171.global-contribute.live-video.net/app", "Kick ← Dashboard ← Settings ← Stream key: انسخ «Stream URL» و«Stream key» (إذا اختلف الرابط ضعه في خانة الخادم).", "https://kick.com/dashboard/settings/stream"},
            {"TikTok", "", "TikTok يعطي رابط الخادم ومفتاح البث فقط لبعض الحسابات (LIVE Studio / مفتاح البث). الصق الرابط والمفتاح كما يظهران لك.", "https://www.tiktok.com/live/creators"},
            {"مخصص", "", "ضع رابط خادم RTMP أو RTMPS ومفتاح البث.", ""}};
    /* quality: label, width, height, fps, bitrate */
    private static final Object[][] QUALITY = {
            {"480p · 30 إطار · 2 ميغابت", 854, 480, 30, 2000},
            {"720p · 30 إطار · 3.5 ميغابت (مستحسن)", 1280, 720, 30, 3500},
            {"720p · 60 إطار · 5 ميغابت", 1280, 720, 60, 5000},
            {"1080p · 30 إطار · 6 ميغابت", 1920, 1080, 30, 6000}};
    private static final String[] AUDIO = {"صوت اللعبة + الميكروفون", "صوت اللعبة فقط", "الميكروفون فقط", "بدون صوت"};
    private static final int[] AUDIO_CODE = {StreamService.AUDIO_MIX, StreamService.AUDIO_GAME, StreamService.AUDIO_MIC, StreamService.AUDIO_NONE};

    private SharedPreferences prefs;
    private Spinner platform, quality, orient, audio;
    private EditText server, key;
    private TextView help, status, timer;
    private Button go;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if ("live".equals(StreamService.state)) timer.setText("⏱ " + StreamService.elapsed());
            ui.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("live", MODE_PRIVATE);
        getWindow().setStatusBarColor(Color.parseColor("#120c18"));
        buildUi();
        StreamService.listener = this::paint;
        paint(StreamService.state, StreamService.stateText);
        ui.post(tick);
    }

    @Override protected void onDestroy() { StreamService.listener = null; ui.removeCallbacks(tick); super.onDestroy(); }

    /* ------------------------------------------------------------ UI (built in code, right-to-left) */

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.parseColor("#120c18"));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int p = dp(18);
        col.setPadding(p, dp(28), p, dp(28));
        sv.addView(col);
        setContentView(sv);

        TextView title = text("🔴 بث الشاشة", 28, Color.WHITE, true);
        col.addView(title);
        col.addView(text("بُثّ شاشة هاتفك (والألعاب) مباشرة إلى يوتيوب أو فيسبوك أو غيرهما.", 14, Color.parseColor("#c9bcd6"), false));

        col.addView(label("المنصة"));
        List<String> names = new ArrayList<>();
        for (String[] pl : PLATFORMS) names.add(pl[0]);
        platform = spinner(names);
        col.addView(platform);

        help = text("", 13, Color.parseColor("#c9bcd6"), false);
        help.setPadding(0, dp(6), 0, 0);
        col.addView(help);
        Button openSite = button("فتح صفحة المفتاح", false);
        openSite.setOnClickListener(v -> {
            String u = PLATFORMS[platform.getSelectedItemPosition()][3];
            if (!u.isEmpty()) try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ignored) {}
        });
        col.addView(openSite);

        col.addView(label("رابط الخادم (Server URL)"));
        server = field(false);
        col.addView(server);
        col.addView(label("مفتاح البث (Stream key)"));
        key = field(true);
        col.addView(key);
        CheckBox show = new CheckBox(this);
        show.setText("إظهار المفتاح");
        show.setTextColor(Color.parseColor("#c9bcd6"));
        show.setOnCheckedChangeListener((c, on) -> key.setInputType(InputType.TYPE_CLASS_TEXT | (on ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD)));
        col.addView(show);

        col.addView(label("الجودة"));
        List<String> q = new ArrayList<>();
        for (Object[] o : QUALITY) q.add((String) o[0]);
        quality = spinner(q);
        quality.setSelection(prefs.getInt("q", 1));
        col.addView(quality);

        col.addView(label("اتجاه البث"));
        List<String> o = new ArrayList<>();
        o.add("عمودي (ألعاب الهاتف مثل Clash Royale)");
        o.add("أفقي (ألعاب بالعرض)");
        orient = spinner(o);
        orient.setSelection(prefs.getInt("o", 0));
        col.addView(orient);

        col.addView(label("الصوت"));
        List<String> a = new ArrayList<>();
        for (String s : AUDIO) a.add(s);
        audio = spinner(a);
        audio.setSelection(prefs.getInt("a", Build.VERSION.SDK_INT >= 29 ? 0 : 2));
        col.addView(audio);
        if (Build.VERSION.SDK_INT < 29) col.addView(text("صوت اللعبة يحتاج أندرويد 10 أو أحدث؛ في هاتفك يُستعمل الميكروفون.", 12, Color.parseColor("#ffb48a"), false));

        go = button("▶  ابدأ البث", true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        lp.topMargin = dp(22);
        go.setLayoutParams(lp);
        go.setOnClickListener(v -> { if ("idle".equals(StreamService.state)) start(); else stop(); });
        col.addView(go);

        status = text("", 15, Color.WHITE, true);
        status.setPadding(0, dp(12), 0, 0);
        col.addView(status);
        timer = text("", 22, Color.parseColor("#ff5a6a"), true);
        col.addView(timer);

        col.addView(text("\nبعد البدء اخرج إلى اللعبة أو أي تطبيق: البث يستمر في الخلفية. أوقفه من هنا أو من الإشعار.\n"
                + "بعض التطبيقات تمنع تسجيل صوتها؛ عندها يصل صوت الميكروفون فقط.\n"
                + "اشحن الهاتف واستعمل الواي فاي لبث طويل.", 13, Color.parseColor("#9d90ad"), false));

        platform.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) { loadPlatform(pos); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        int last = prefs.getInt("p", 0);
        platform.setSelection(last);
        loadPlatform(last);
    }

    private void loadPlatform(int pos) {
        String[] pl = PLATFORMS[pos];
        help.setText(pl[2]);
        server.setText(prefs.getString("server" + pos, pl[1]));
        key.setText(prefs.getString("key" + pos, ""));
    }

    private void savePrefs() {
        int pos = platform.getSelectedItemPosition();
        prefs.edit().putInt("p", pos).putString("server" + pos, server.getText().toString().trim()).putString("key" + pos, key.getText().toString().trim())
                .putInt("q", quality.getSelectedItemPosition()).putInt("o", orient.getSelectedItemPosition()).putInt("a", audio.getSelectedItemPosition()).apply();
    }

    /* ------------------------------------------------------------ start / stop */

    private void start() {
        String s = server.getText().toString().trim(), k = key.getText().toString().trim();
        if (s.isEmpty() || !(s.startsWith("rtmp://") || s.startsWith("rtmps://"))) { paint("idle", "اكتب رابط الخادم (يبدأ بـ rtmp:// أو rtmps://)"); return; }
        if (k.isEmpty()) { paint("idle", "الصق مفتاح البث أولاً"); return; }
        savePrefs();
        List<String> need = new ArrayList<>();
        if (AUDIO_CODE[audio.getSelectedItemPosition()] != StreamService.AUDIO_NONE && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.RECORD_AUDIO);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!need.isEmpty()) { requestPermissions(need.toArray(new String[0]), REQ_PERMS); return; }
        askCapture();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req != REQ_PERMS) return;
        for (int i = 0; i < perms.length; i++)
            if (Manifest.permission.RECORD_AUDIO.equals(perms[i]) && res[i] != PackageManager.PERMISSION_GRANTED) audio.setSelection(3);   /* no mic: stream without sound */
        askCapture();
    }

    private void askCapture() {
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_CAPTURE) return;
        if (res != RESULT_OK || data == null) { paint("idle", "لم يُسمح بتسجيل الشاشة"); return; }
        Object[] q = QUALITY[quality.getSelectedItemPosition()];
        String s = server.getText().toString().trim(), k = key.getText().toString().trim();
        String url = s.endsWith("/") ? s + k : s + "/" + k;
        Intent i = new Intent(this, StreamService.class).setAction(StreamService.ACTION_START)
                .putExtra("code", res).putExtra("data", data).putExtra("url", url)
                .putExtra("w", (int) q[1]).putExtra("h", (int) q[2]).putExtra("fps", (int) q[3]).putExtra("bitrate", (int) q[4] * 1000)
                .putExtra("vertical", orient.getSelectedItemPosition() == 0).putExtra("audio", AUDIO_CODE[audio.getSelectedItemPosition()])
                .putExtra("label", PLATFORMS[platform.getSelectedItemPosition()][0] + " · " + ((String) q[0]).split(" ·")[0]);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        paint("connecting", "جارٍ الاتصال…");
    }

    private void stop() { startService(new Intent(this, StreamService.class).setAction(StreamService.ACTION_STOP)); }

    private void paint(String state, String text) {
        boolean idle = "idle".equals(state);
        go.setText(idle ? "▶  ابدأ البث" : "■  إيقاف البث");
        ((GradientDrawable) go.getBackground()).setColor(Color.parseColor(idle ? "#e1253a" : "#3a2f45"));
        status.setText(text == null ? "" : text);
        status.setTextColor(Color.parseColor("live".equals(state) ? "#7dffb0" : "reconnecting".equals(state) ? "#ffd36a" : "#ffffff"));
        if (!"live".equals(state)) timer.setText("");
        for (View v : new View[]{platform, server, key, quality, orient, audio}) v.setEnabled(idle);
    }

    /* ------------------------------------------------------------ small view helpers */

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        return t;
    }

    private TextView label(String s) {
        TextView t = text(s, 14, Color.parseColor("#ffb36a"), true);
        t.setPadding(0, dp(16), 0, dp(4));
        return t;
    }

    private EditText field(boolean secret) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.parseColor("#6d6080"));
        e.setTextDirection(View.TEXT_DIRECTION_LTR);
        e.setInputType(InputType.TYPE_CLASS_TEXT | (secret ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_VARIATION_URI));
        e.setBackground(round("#22182c", 10));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    private Spinner spinner(List<String> items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            @Override public View getView(int pos, View cv, ViewGroup parent) { TextView t = (TextView) super.getView(pos, cv, parent); t.setTextColor(Color.WHITE); t.setTextSize(15); return t; }
        };
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(ad);
        s.setBackground(round("#22182c", 10));
        s.setPadding(dp(8), dp(6), dp(8), dp(6));
        return s;
    }

    private Button button(String s, boolean primary) {
        Button btn = new Button(this);
        btn.setText(s);
        btn.setAllCaps(false);
        btn.setTextSize(primary ? 19 : 14);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextColor(Color.WHITE);
        btn.setBackground(round(primary ? "#e1253a" : "#2c2138", 14));
        return btn;
    }

    private GradientDrawable round(String color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(dp(r));
        return g;
    }
}
