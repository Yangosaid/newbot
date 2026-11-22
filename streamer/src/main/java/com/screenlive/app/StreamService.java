package com.screenlive.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.sources.audio.AudioSource;
import com.pedro.encoder.input.sources.audio.InternalAudioSource;
import com.pedro.encoder.input.sources.audio.MicrophoneSource;
import com.pedro.encoder.input.sources.audio.MixAudioSource;
import com.pedro.encoder.input.sources.audio.SilenceAudioSource;
import com.pedro.encoder.input.sources.video.ScreenSource;
import com.pedro.library.generic.GenericStream;

import java.util.Locale;

/**
 * Captures the whole screen (and the game sound and/or the microphone) and sends it live over RTMP/RTMPS to
 * YouTube, Facebook, Twitch… It runs as a foreground service, so the stream keeps going while another app (the game)
 * is on screen. Stop it from the app or from the notification.
 */
public class StreamService extends Service implements ConnectChecker {

    static final String ACTION_START = "com.screenlive.START", ACTION_STOP = "com.screenlive.STOP";
    static final int AUDIO_NONE = 0, AUDIO_MIC = 1, AUDIO_GAME = 2, AUDIO_MIX = 3;
    private static final String CHANNEL = "live";
    private static final int NOTIFY_ID = 7;

    interface Listener { void onState(String state, String text); }

    static volatile Listener listener;
    static volatile String state = "idle", stateText = "";
    static volatile long liveSince;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private GenericStream stream;
    private MediaProjection projection;
    private PowerManager.WakeLock wake;
    private String label = "";
    private boolean stopping;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!"live".equals(state)) return;
            notifyText("🔴 مباشر " + elapsed() + " · " + label);
            ui.postDelayed(this, 1000);
        }
    };

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) { stopAll("تم إيقاف البث"); return START_NOT_STICKY; }
        if (stream != null) return START_NOT_STICKY;
        stopping = false;
        int audio = intent.getIntExtra("audio", AUDIO_MIX);
        if (audio == AUDIO_GAME || audio == AUDIO_MIX) { if (Build.VERSION.SDK_INT < 29) audio = AUDIO_MIC; }
        label = intent.getStringExtra("label");
        if (label == null) label = "";

        /* Android 10+: the service must be in the foreground with the media-projection type before the projection is created */
        Notification n = buildNotification("جارٍ الاتصال…");
        if (Build.VERSION.SDK_INT >= 29) {
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            if (audio != AUDIO_NONE && Build.VERSION.SDK_INT >= 30) type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            startForeground(NOTIFY_ID, n, type);
        } else startForeground(NOTIFY_ID, n);

        try {
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent data = intent.getParcelableExtra("data");
            projection = mpm.getMediaProjection(intent.getIntExtra("code", 0), data);
            if (projection == null) throw new IllegalStateException("no projection");

            AudioSource as;
            if (audio == AUDIO_MIC) as = new MicrophoneSource(MediaRecorder.AudioSource.DEFAULT);
            else if (audio == AUDIO_GAME) as = new InternalAudioSource(projection, null);
            else if (audio == AUDIO_MIX) as = new MixAudioSource(projection, null, MediaRecorder.AudioSource.DEFAULT);
            else as = new SilenceAudioSource();

            int w = intent.getIntExtra("w", 1280), h = intent.getIntExtra("h", 720), fps = intent.getIntExtra("fps", 30);
            int bitrate = intent.getIntExtra("bitrate", 3500 * 1000);
            boolean vertical = intent.getBooleanExtra("vertical", true);

            stream = new GenericStream(this, this, new ScreenSource(this, projection), as);
            /* the screen only produces frames when it changes: force a steady frame rate */
            stream.getGlInterface().setForceRender(true, fps);
            boolean ok = stream.prepareVideo(w, h, bitrate, fps, 2, vertical ? 90 : 0)
                    && stream.prepareAudio(44100, true, 128 * 1000, audio == AUDIO_MIC, audio == AUDIO_MIC);
            if (!ok) { stopAll("هذا الهاتف لا يدعم هذه الجودة، جرّب جودة أقل"); return START_NOT_STICKY; }
            stream.getStreamClient().setReTries(40);

            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "screenlive:stream");
            wake.acquire(6 * 60 * 60 * 1000L);

            setState("connecting", "جارٍ الاتصال بالخادم…");
            stream.startStream(intent.getStringExtra("url"));
        } catch (Exception e) {
            stopAll("تعذّر بدء البث: " + e.getMessage());
        }
        return START_NOT_STICKY;
    }

    /* ------------------------------------------------------------ connection events */

    @Override public void onConnectionStarted(String url) { setState("connecting", "جارٍ الاتصال بالخادم…"); }

    @Override public void onConnectionSuccess() {
        liveSince = System.currentTimeMillis();
        setState("live", "🔴 أنت على الهواء");
        ui.removeCallbacks(ticker);
        ui.post(ticker);
    }

    @Override public void onConnectionFailed(String reason) {
        if (stopping || stream == null) return;
        if (stream.getStreamClient().reTry(3000L, reason, null)) setState("reconnecting", "انقطع الاتصال، إعادة المحاولة… (" + reason + ")");
        else stopAll("فشل الاتصال: " + reason + "\nتأكد من مفتاح البث وأن البث مفعّل على حسابك.");
    }

    @Override public void onDisconnect() { if (!stopping) setState("reconnecting", "انقطع الاتصال…"); }

    @Override public void onAuthError() { stopAll("رفض الخادم مفتاح البث. انسخ المفتاح من جديد."); }

    @Override public void onAuthSuccess() { }

    /* ------------------------------------------------------------ helpers */

    private void stopAll(String why) {
        stopping = true;
        ui.removeCallbacks(ticker);
        try { if (stream != null) { if (stream.isStreaming()) stream.stopStream(); stream.release(); } } catch (Exception ignored) {}
        stream = null;
        try { if (projection != null) projection.stop(); } catch (Exception ignored) {}
        projection = null;
        try { if (wake != null && wake.isHeld()) wake.release(); } catch (Exception ignored) {}
        setState("idle", why);
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        if (stream != null) stopAll("تم إيقاف البث");
        super.onDestroy();
    }

    private void setState(final String s, final String text) {
        state = s;
        stateText = text;
        if (!"live".equals(s) && !"idle".equals(s)) notifyText(text);
        ui.post(() -> { Listener l = listener; if (l != null) l.onState(s, text); });
    }

    static String elapsed() {
        long t = Math.max(0, (System.currentTimeMillis() - liveSince) / 1000);
        return String.format(Locale.US, "%02d:%02d:%02d", t / 3600, (t / 60) % 60, t % 60);
    }

    private Notification buildNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null)
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "البث المباشر", NotificationManager.IMPORTANCE_LOW));
        int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), fl);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, StreamService.class).setAction(ACTION_STOP), fl);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.presence_video_online).setContentTitle("بث الشاشة").setContentText(text)
                .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause, "إيقاف البث", stop).build());
        return b.build();
    }

    private void notifyText(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try { nm.notify(NOTIFY_ID, buildNotification(text)); } catch (Exception ignored) {}
    }
}
