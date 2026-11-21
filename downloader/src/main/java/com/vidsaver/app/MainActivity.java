package com.vidsaver.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity implements Downloader.Listener {

    private EditText url;
    private TextView detected, version, jobTitle, jobDetail, empty;
    private RadioGroup mode;
    private View job;
    private ProgressBar bar;
    private Button go;
    private JSONArray items = new JSONArray();
    private final Adapter adapter = new Adapter();
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);
        prefs = getSharedPreferences("ui", MODE_PRIVATE);
        url = findViewById(R.id.url);
        detected = findViewById(R.id.detected);
        version = findViewById(R.id.version);
        mode = findViewById(R.id.mode);
        job = findViewById(R.id.job);
        jobTitle = findViewById(R.id.jobTitle);
        jobDetail = findViewById(R.id.jobDetail);
        bar = findViewById(R.id.bar);
        go = findViewById(R.id.go);
        empty = findViewById(R.id.empty);
        ListView list = findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setEmptyView(empty);

        int m = prefs.getInt("mode", R.id.mBest);
        if (findViewById(m) != null) mode.check(m);
        mode.setOnCheckedChangeListener((g, id) -> prefs.edit().putInt("mode", id).apply());

        url.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { showDetected(); }
        });
        findViewById(R.id.paste).setOnClickListener(v -> { String c = clip(); if (c != null) url.setText(c); else toast("الحافظة فارغة"); });
        go.setOnClickListener(v -> start());
        findViewById(R.id.cancel).setOnClickListener(v -> Downloader.cancel());
        findViewById(R.id.update).setOnClickListener(v -> { Downloader.prepare(this); Downloader.updateEngine(this, true); });

        list.setOnItemClickListener((p, v, pos, id) -> open(items.optJSONObject(pos)));
        list.setOnItemLongClickListener((p, v, pos, id) -> { more(pos); return true; });

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

        Downloader.prepare(this);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /** Links shared from TikTok / YouTube ("Share → VidSaver") or opened with the app. */
    private void handleIntent(Intent i) {
        if (i == null) return;
        String text = null;
        if (Intent.ACTION_SEND.equals(i.getAction())) text = i.getStringExtra(Intent.EXTRA_TEXT);
        else if (Intent.ACTION_VIEW.equals(i.getAction()) && i.getData() != null) text = i.getData().toString();
        if (text == null) return;
        Links.Link l = Links.find(text);
        if (l == null) { toast("لم أجد رابطاً في النص المُشارَك"); return; }
        url.setText(l.url);
        if (prefs.getBoolean("autoStart", true)) start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Downloader.setListener(this);
        refresh();
        String v = Downloader.engineVersion(this);
        version.setText(v == null ? "يدعم كل صيغ روابط تيك توك ويوتيوب وشورتس" : "محرك yt-dlp " + v);
        // Offer the copied link right away.
        if (url.getText().length() == 0) {
            String c = clip();
            if (c != null && Links.find(c) != null) url.setText(Links.find(c).url);
        }
    }

    @Override
    protected void onPause() {
        Downloader.setListener(null);
        super.onPause();
    }

    private void start() {
        Links.Link l = Links.find(url.getText().toString());
        if (l == null) { toast("الصق رابطاً صحيحاً أولاً"); return; }
        int id = mode.getCheckedRadioButtonId();
        String m = id == R.id.mMp3 ? Downloader.MODE_MP3 : id == R.id.mMp4 ? Downloader.MODE_MP4 : Downloader.MODE_BEST;
        Downloader.enqueue(this, l, m);
        toast("أُضيف إلى التحميل");
        url.setText("");
    }

    private void showDetected() {
        String t = url.getText().toString().trim();
        if (t.isEmpty()) { detected.setText(""); return; }
        Links.Link l = Links.find(t);
        detected.setText(l == null ? "✗ لم يُتعرَّف على رابط" : "✓ " + l.label());
        detected.setTextColor(getColor(l == null ? R.color.bad : R.color.ok));
    }

    private String clip() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return null;
        ClipData d = cm.getPrimaryClip();
        if (d == null || d.getItemCount() == 0) return null;
        CharSequence s = d.getItemAt(0).coerceToText(this);
        return s == null || s.length() == 0 ? null : s.toString();
    }

    // ---------------------------------------------------------------- downloader callbacks

    @Override
    public void onState(Downloader.State s) {
        job.setVisibility(s.busy || s.queued > 0 ? View.VISIBLE : View.GONE);
        jobTitle.setText(s.busy ? s.title : "في الانتظار…");
        String extra = s.queued > 1 ? " · في الطابور: " + (s.queued - 1) : "";
        jobDetail.setText(s.detail + extra);
        bar.setIndeterminate(s.percent < 0);
        if (s.percent >= 0) bar.setProgress(s.percent);
    }

    @Override public void onHistoryChanged() { refresh(); }

    @Override public void onMessage(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        String v = Downloader.engineVersion(this);
        if (v != null) version.setText("محرك yt-dlp " + v);
    }

    // ---------------------------------------------------------------- history list

    private void refresh() {
        items = Downloader.history(this);
        adapter.notifyDataSetChanged();
    }

    private void open(JSONObject o) {
        if (o == null) return;
        Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(o.optString("uri")),
                Downloader.mimeOf(o.optString("name"), "audio".equals(o.optString("type")))).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivity(i); } catch (ActivityNotFoundException | SecurityException e) { toast("لا يوجد تطبيق لتشغيل الملف، أو حُذف الملف"); }
    }

    private void more(final int pos) {
        final JSONObject o = items.optJSONObject(pos);
        if (o == null) return;
        new AlertDialog.Builder(this).setTitle(o.optString("name"))
                .setItems(new String[]{"تشغيل", "مشاركة", "إزالة من القائمة"}, (d, w) -> {
                    if (w == 0) open(o);
                    else if (w == 1) {
                        Intent s = new Intent(Intent.ACTION_SEND).setType(Downloader.mimeOf(o.optString("name"), "audio".equals(o.optString("type"))))
                                .putExtra(Intent.EXTRA_STREAM, Uri.parse(o.optString("uri"))).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try { startActivity(Intent.createChooser(s, "مشاركة")); } catch (Exception e) { toast("تعذّرت المشاركة"); }
                    } else { Downloader.removeHistory(this, pos); refresh(); }
                }).show();
    }

    private final class Adapter extends BaseAdapter {
        @Override public int getCount() { return items.length(); }
        @Override public Object getItem(int p) { return items.optJSONObject(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int p, View v, ViewGroup parent) {
            if (v == null) v = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            JSONObject o = items.optJSONObject(p);
            TextView a = v.findViewById(android.R.id.text1), b = v.findViewById(android.R.id.text2);
            a.setTextColor(getColor(R.color.ink));
            b.setTextColor(getColor(R.color.dim));
            if (o != null) {
                a.setText(("audio".equals(o.optString("type")) ? "♪ " : "▶ ") + o.optString("name"));
                b.setText(Formatter.formatShortFileSize(MainActivity.this, o.optLong("size")) + " · "
                        + DateUtils.getRelativeTimeSpanString(o.optLong("time"), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            }
            return v;
        }
    }

    private void toast(String t) { Toast.makeText(this, t, Toast.LENGTH_SHORT).show(); }
}
