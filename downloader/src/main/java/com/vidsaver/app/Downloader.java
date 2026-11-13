package com.vidsaver.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

/**
 * Foreground service that runs yt-dlp jobs one after another, then copies each finished file
 * into the phone's public Movies/VidSaver or Music/VidSaver folder.
 */
public class Downloader extends Service {

    public static final String MODE_BEST = "best", MODE_MP4 = "mp4", MODE_MP3 = "mp3";
    private static final String CHANNEL = "downloads";
    private static final int NOTE_ID = 1;

    /** What the screen shows about the running job. */
    public static final class State {
        public boolean busy;
        public String title = "", detail = "";
        public int percent = -1;
        public int queued;
    }

    public interface Listener {
        void onState(State s);
        void onHistoryChanged();
        void onMessage(String text);
    }

    private static Listener listener;
    private static final State state = new State();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static final ExecutorService worker = Executors.newSingleThreadExecutor();
    private static final AtomicInteger pending = new AtomicInteger();
    private static volatile String currentId;
    private static volatile boolean ready;
    private static final CountDownLatch initDone = new CountDownLatch(1);
    private static volatile String initError;

    public static void setListener(Listener l) {
        listener = l;
        if (l != null) l.onState(copy());
    }

    // ------------------------------------------------------------------ public API

    /** Unpacks Python, yt-dlp and FFmpeg once, then updates yt-dlp at most once a day. */
    public static void prepare(final Context ctx) {
        final Context app = ctx.getApplicationContext();
        if (ready || initDone.getCount() == 0) return;
        synchronized (Downloader.class) {
            if (ready) return;
            ready = true;
        }
        worker.execute(() -> {
            try {
                status(true, "تجهيز محرك التحميل…", "أول تشغيل فقط", -1);
                YoutubeDL.getInstance().init(app);
                FFmpeg.getInstance().init(app);
            } catch (Throwable e) {
                initError = String.valueOf(e.getMessage());
            } finally {
                initDone.countDown();
            }
            if (initError == null) updateEngine(app, false);
            idle();
        });
    }

    /** Updates yt-dlp (the part that knows how YouTube and TikTok work). */
    public static void updateEngine(final Context ctx, boolean force) {
        final Context app = ctx.getApplicationContext();
        SharedPreferences p = app.getSharedPreferences("dl", MODE_PRIVATE);
        long last = p.getLong("updated", 0);
        if (!force && System.currentTimeMillis() - last < 20 * 3600_000L) return;
        Runnable r = () -> {
            try {
                status(true, "تحديث محرك التحميل…", "", -1);
                YoutubeDL.UpdateStatus s = YoutubeDL.getInstance().updateYoutubeDL(app, YoutubeDL.UpdateChannel._STABLE);
                p.edit().putLong("updated", System.currentTimeMillis()).apply();
                String v = YoutubeDL.getInstance().versionName(app);
                if (force) message(s == YoutubeDL.UpdateStatus.DONE ? "تم التحديث إلى " + v : "المحرك محدّث (" + v + ")");
            } catch (Throwable e) {
                if (force) message("تعذّر التحديث: " + e.getMessage());
            }
        };
        if (force) worker.execute(() -> { r.run(); idle(); });
        else r.run();
    }

    public static String engineVersion(Context ctx) {
        try { return YoutubeDL.getInstance().versionName(ctx.getApplicationContext()); } catch (Throwable e) { return null; }
    }

    /** Queues a download. */
    public static void enqueue(Context ctx, Links.Link link, String mode) {
        prepare(ctx);
        Intent i = new Intent(ctx, Downloader.class).putExtra("url", link.url).putExtra("mode", mode)
                .putExtra("playlist", link.playlist()).putExtra("platform", link.platform);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i);
        pending.incrementAndGet();
        state.queued = pending.get();
        post();
    }

    public static void cancel() {
        String id = currentId;
        if (id != null) YoutubeDL.getInstance().destroyProcessById(id);
    }

    // ------------------------------------------------------------------ service

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundNote("جاري التحميل…", -1);
        if (intent == null) { maybeStop(); return START_NOT_STICKY; }
        final String url = intent.getStringExtra("url"), mode = intent.getStringExtra("mode");
        final boolean playlist = intent.getBooleanExtra("playlist", false);
        final String platform = intent.getStringExtra("platform");
        worker.execute(() -> {
            try { run(url, mode, playlist, platform); }
            finally {
                pending.decrementAndGet();
                state.queued = Math.max(0, pending.get());
                idle();
                main.post(this::maybeStop);
            }
        });
        return START_NOT_STICKY;
    }

    private void maybeStop() {
        if (pending.get() <= 0) {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
            stopSelf();
        }
    }

    private void run(String url, String mode, boolean playlist, String platform) {
        try { initDone.await(); } catch (InterruptedException e) { return; }
        if (initError != null) { message("تعذّر تشغيل محرك التحميل: " + initError); return; }

        File dir = new File(getCacheDir(), "dl/" + System.currentTimeMillis());
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        String id = "job" + System.nanoTime();
        currentId = id;
        status(true, "جاري جلب معلومات الرابط…", url, -1);

        YoutubeDLRequest r = new YoutubeDLRequest(url);
        r.addOption("-o", dir.getAbsolutePath() + "/%(title).70B [%(id)s].%(ext)s");
        r.addOption(playlist ? "--yes-playlist" : "--no-playlist");
        r.addOption("--no-mtime");
        r.addOption("--no-part");
        r.addOption("--newline");
        r.addOption("--embed-metadata");
        r.addOption("--concurrent-fragments", 4);
        r.addOption("--retries", 5);
        r.addOption("--fragment-retries", 10);
        if (MODE_MP3.equals(mode)) {
            r.addOption("-f", "ba/b");
            r.addOption("-x");
            r.addOption("--audio-format", "mp3");
            r.addOption("--audio-quality", "0");
            r.addOption("--embed-thumbnail");
        } else if (MODE_MP4.equals(mode)) {
            // Plays everywhere: H.264 video + AAC audio, best resolution available in that codec.
            r.addOption("-f", "bv*+ba/b");
            r.addOption("-S", "vcodec:h264,res,fps,acodec:m4a");
            r.addOption("--merge-output-format", "mp4");
        } else {
            // Highest quality the site offers (4K/8K, 60 fps, HDR when available), merged with the best audio.
            r.addOption("-f", "bv*+ba/b");
            r.addOption("-S", "res,fps,hdr:12,vbr,abr");
            r.addOption("--merge-output-format", "mp4/mkv");
        }

        final String[] title = {url};
        try {
            YoutubeDL.getInstance().execute(r, id, false, (Float progress, Long eta, String line) -> {
                String t = parseTitle(line);
                if (t != null) title[0] = t;
                int pct = progress == null || progress < 0 ? -1 : Math.round(progress);
                String detail = pct >= 0 ? pct + "%" + (eta != null && eta > 0 ? " · متبقٍ " + fmtEta(eta) : "") : shortLine(line);
                status(true, title[0], detail, pct);
                return kotlin.Unit.INSTANCE;
            });
        } catch (YoutubeDL.CanceledException e) {
            message("أُلغي التحميل");
            deleteDir(dir);
            return;
        } catch (Throwable e) {
            message(explain(String.valueOf(e.getMessage()), platform, url));
            deleteDir(dir);
            return;
        } finally {
            currentId = null;
        }

        File[] files = dir.listFiles();
        List<File> done = new ArrayList<>();
        if (files != null) for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && f.length() > 0 && !n.endsWith(".part") && !n.endsWith(".ytdl") && !n.endsWith(".json")
                    && !n.endsWith(".jpg") && !n.endsWith(".webp") && !n.endsWith(".png")) done.add(f);
        }
        if (done.isEmpty()) { message("انتهى بدون ملف. جرّب «تحديث المحرك» ثم أعد المحاولة."); deleteDir(dir); return; }
        int saved = 0;
        for (File f : done) {
            status(true, "حفظ في المعرض…", f.getName(), 100);
            Uri u = publish(f, MODE_MP3.equals(mode));
            if (u != null) { saved++; addHistory(this, f.getName(), u.toString(), MODE_MP3.equals(mode) ? "audio" : "video", f.length()); }
        }
        deleteDir(dir);
        message(saved > 0 ? (saved == 1 ? "تم الحفظ: " + done.get(0).getName() : "تم حفظ " + saved + " ملفات") : "تعذّر حفظ الملف في الذاكرة");
        Listener l = listener;
        if (l != null) main.post(l::onHistoryChanged);
    }

    // ------------------------------------------------------------------ saving to the gallery

    private Uri publish(File f, boolean audio) {
        String name = f.getName();
        String mime = mimeOf(name, audio);
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver cr = getContentResolver();
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, (audio ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_MOVIES) + "/VidSaver");
                v.put(MediaStore.MediaColumns.IS_PENDING, 1);
                Uri col = audio ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        : MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri u = cr.insert(col, v);
                if (u == null) return null;
                try (InputStream in = new FileInputStream(f); OutputStream out = cr.openOutputStream(u)) {
                    if (out == null) throw new IllegalStateException("no stream");
                    copy(in, out);
                } catch (Exception e) {
                    cr.delete(u, null, null);
                    throw e;
                }
                v.clear();
                v.put(MediaStore.MediaColumns.IS_PENDING, 0);
                cr.update(u, v, null, null);
                return u;
            } else {
                File base = Environment.getExternalStoragePublicDirectory(audio ? Environment.DIRECTORY_MUSIC : Environment.DIRECTORY_MOVIES);
                File outDir = new File(base, "VidSaver");
                //noinspection ResultOfMethodCallIgnored
                outDir.mkdirs();
                File dest = new File(outDir, name);
                try (InputStream in = new FileInputStream(f); OutputStream out = new java.io.FileOutputStream(dest)) { copy(in, out); }
                final Uri[] result = {null};
                final CountDownLatch l = new CountDownLatch(1);
                MediaScannerConnection.scanFile(this, new String[]{dest.getAbsolutePath()}, new String[]{mime}, (p, uri) -> { result[0] = uri; l.countDown(); });
                l.await(10, java.util.concurrent.TimeUnit.SECONDS);
                return result[0] != null ? result[0] : Uri.fromFile(dest);
            }
        } catch (Exception e) {
            message("خطأ في الحفظ: " + e.getMessage());
            return null;
        }
    }

    static String mimeOf(String name, boolean audio) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".mp3")) return "audio/mpeg";
        if (n.endsWith(".m4a")) return "audio/mp4";
        if (n.endsWith(".opus") || n.endsWith(".ogg")) return "audio/ogg";
        if (n.endsWith(".mkv")) return "video/x-matroska";
        if (n.endsWith(".webm")) return audio ? "audio/webm" : "video/webm";
        return audio ? "audio/mpeg" : "video/mp4";
    }

    // ------------------------------------------------------------------ history

    public static JSONArray history(Context ctx) {
        try { return new JSONArray(ctx.getSharedPreferences("dl", MODE_PRIVATE).getString("history", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private static synchronized void addHistory(Context ctx, String name, String uri, String type, long size) {
        try {
            JSONArray old = history(ctx), a = new JSONArray();
            a.put(new JSONObject().put("name", name).put("uri", uri).put("type", type).put("size", size).put("time", System.currentTimeMillis()));
            for (int i = 0; i < old.length() && a.length() < 100; i++) a.put(old.get(i));
            ctx.getSharedPreferences("dl", MODE_PRIVATE).edit().putString("history", a.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static synchronized void removeHistory(Context ctx, int index) {
        JSONArray old = history(ctx), a = new JSONArray();
        for (int i = 0; i < old.length(); i++) if (i != index) a.put(old.opt(i));
        ctx.getSharedPreferences("dl", MODE_PRIVATE).edit().putString("history", a.toString()).apply();
    }

    // ------------------------------------------------------------------ messages

    static String explain(String err, String platform, String url) {
        String e = err == null ? "" : err;
        String last = lastErrorLine(e);
        String l = e.toLowerCase(Locale.ROOT);
        if (l.contains("sign in to confirm") || l.contains("not a bot"))
            return "يوتيوب طلب التحقق من أنك لست روبوتاً. اضغط «تحديث المحرك» ثم أعد المحاولة، أو جرّب بعد قليل.";
        if (l.contains("private video") || l.contains("this video is private")) return "الفيديو خاص ولا يمكن تحميله.";
        if (l.contains("members-only") || l.contains("join this channel")) return "الفيديو للأعضاء فقط.";
        if (l.contains("age") && l.contains("confirm")) return "الفيديو مقيّد بالعمر ويحتاج تسجيل دخول.";
        if (l.contains("unsupported url") && url.contains("/photo/")) return "منشورات الصور في تيك توك غير مدعومة بعد، الفيديوهات فقط.";
        if (l.contains("unsupported url")) return "الرابط غير مدعوم.";
        if (l.contains("unable to download webpage") || l.contains("failed to resolve") || l.contains("timed out") || l.contains("network"))
            return "مشكلة في الاتصال بالإنترنت. تحقق من الشبكة وأعد المحاولة.";
        if (l.contains("ffmpeg")) return "تعذّر دمج الصوت والصورة: " + last;
        if (l.contains("no space left")) return "لا توجد مساحة كافية في الهاتف.";
        return "فشل التحميل" + ("tiktok".equals(platform) || "youtube".equals(platform) ? " (جرّب «تحديث المحرك»)" : "") + ": " + last;
    }

    private static String lastErrorLine(String e) {
        String[] lines = e.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) if (lines[i].contains("ERROR")) return lines[i].replace("ERROR:", "").trim();
        for (int i = lines.length - 1; i >= 0; i--) if (!lines[i].trim().isEmpty()) return lines[i].trim();
        return e;
    }

    static String parseTitle(String line) {
        if (line == null) return null;
        String p = null;
        int i = line.indexOf("Destination: ");
        if (i >= 0) p = line.substring(i + "Destination: ".length());
        else {
            i = line.indexOf("Merging formats into \"");
            if (i >= 0) p = line.substring(i + "Merging formats into \"".length()).replaceAll("\"$", "");
        }
        if (p == null) return null;
        String n = new File(p.trim()).getName();
        n = n.replaceAll("\\.f\\d+(\\.\\w+)$", "$1").replaceAll("\\.[A-Za-z0-9]{2,4}$", "").replaceAll(" \\[[^\\]]+\\]$", "");
        return n.isEmpty() ? null : n;
    }

    private static String shortLine(String line) {
        if (line == null) return "";
        line = line.replaceAll("^\\[[a-z:A-Z]+\\]\\s*", "");
        return line.length() > 80 ? line.substring(0, 80) + "…" : line;
    }

    private static String fmtEta(long s) { return s >= 60 ? (s / 60) + " د " + (s % 60) + " ث" : s + " ث"; }

    // ------------------------------------------------------------------ plumbing

    private static State copy() {
        State s = new State();
        s.busy = state.busy; s.title = state.title; s.detail = state.detail; s.percent = state.percent; s.queued = state.queued;
        return s;
    }

    private static long lastPost;

    private static void status(boolean busy, String title, String detail, int pct) {
        state.busy = busy; state.title = title; state.detail = detail; state.percent = pct;
        long now = System.currentTimeMillis();
        if (now - lastPost < 250 && pct >= 0 && pct < 100) return;   // keep the UI calm
        lastPost = now;
        post();
        Service s = instance;
        if (s instanceof Downloader) ((Downloader) s).updateNote(title, pct);
    }

    private static void idle() { status(false, "", "", -1); }

    private static void post() {
        final State s = copy();
        main.post(() -> { Listener l = listener; if (l != null) l.onState(s); });
    }

    private static void message(final String text) {
        main.post(() -> { Listener l = listener; if (l != null) l.onMessage(text); });
    }

    private static Service instance;

    @Override public void onCreate() { super.onCreate(); instance = this; }
    @Override public void onDestroy() { instance = null; super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }

    private Notification.Builder note() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            if (nm.getNotificationChannel(CHANNEL) == null)
                nm.createNotificationChannel(new NotificationChannel(CHANNEL, "التحميلات", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, CHANNEL);
        } else b = new Notification.Builder(this);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        return b.setSmallIcon(android.R.drawable.stat_sys_download).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true);
    }

    private void startForegroundNote(String title, int pct) {
        Notification n = note().setContentTitle(title).setProgress(100, Math.max(0, pct), pct < 0).build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTE_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(NOTE_ID, n);
    }

    private void updateNote(String title, int pct) {
        if (title == null || title.isEmpty()) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTE_ID, note().setContentTitle(title).setContentText(pct >= 0 ? pct + "%" : "")
                .setProgress(100, Math.max(0, pct), pct < 0).build());
    }

    private static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private static void deleteDir(File d) {
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) { if (f.isDirectory()) deleteDir(f); else //noinspection ResultOfMethodCallIgnored
            f.delete(); }
        //noinspection ResultOfMethodCallIgnored
        d.delete();
    }
}
