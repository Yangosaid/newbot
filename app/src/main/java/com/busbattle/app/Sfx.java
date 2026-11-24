package com.busbattle.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.speech.tts.TextToSpeech;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Short sound effects (assets/sfx/*.wav) played by Android's SoundPool. SoundPool never asks for the audio focus, so
 * Spotify keeps playing underneath. The painter's voice uses the phone's text-to-speech, which only lowers the music a
 * moment while it talks.
 */
final class Sfx {
    private final Context ctx;
    private SoundPool pool;
    private final Map<String, Integer> ids = new HashMap<>();
    private final Map<Integer, Boolean> loaded = new HashMap<>();
    private TextToSpeech tts;
    private boolean ttsReady;
    private String pendingText, pendingLang;

    Sfx(Context c) { ctx = c.getApplicationContext(); }

    private SoundPool pool() {
        if (pool == null) {
            pool = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            pool.setOnLoadCompleteListener((p, id, status) -> { synchronized (loaded) { loaded.put(id, status == 0); } });
        }
        return pool;
    }

    /** Loads the named sounds ahead of time (comma separated), so the first play isn't late. */
    synchronized void preload(String names) {
        for (String n : names.split(",")) id(n.trim());
    }

    private Integer id(String name) {
        if (name.isEmpty() || !name.matches("[a-z0-9]+")) return null;
        Integer id = ids.get(name);
        if (id != null) return id;
        try (AssetFileDescriptor fd = ctx.getAssets().openFd("sfx/" + name + ".wav")) {
            id = pool().load(fd, 1);
            ids.put(name, id);
            return id;
        } catch (Exception e) { return null; }
    }

    synchronized void play(String name, float vol, float rate) {
        Integer id = id(name);
        if (id == null) return;
        synchronized (loaded) { if (!Boolean.TRUE.equals(loaded.get(id))) return; }   // still loading: skip rather than play late
        float v = Math.max(0f, Math.min(1f, vol));
        pool().play(id, v, v, 1, 0, Math.max(0.5f, Math.min(2f, rate)));
    }

    synchronized void say(String text, String lang) {
        if (text == null || text.isEmpty()) return;
        if (tts == null) {
            pendingText = text; pendingLang = lang;
            tts = new TextToSpeech(ctx, status -> {
                synchronized (Sfx.this) {
                    ttsReady = status == TextToSpeech.SUCCESS;
                    if (ttsReady && pendingText != null) { speak(pendingText, pendingLang); pendingText = null; }
                }
            });
            return;
        }
        if (!ttsReady) { pendingText = text; pendingLang = lang; return; }
        speak(text, lang);
    }

    private void speak(String text, String lang) {
        try {
            Locale l = "ar".equals(lang) ? new Locale("ar") : "en".equals(lang) ? Locale.US : new Locale("es", "ES");
            if (tts.isLanguageAvailable(l) >= TextToSpeech.LANG_AVAILABLE) tts.setLanguage(l);
            tts.setPitch(1.15f);
            tts.setSpeechRate(1.05f);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "painter");
        } catch (Exception ignored) {}
    }

    synchronized void release() {
        if (pool != null) { pool.release(); pool = null; ids.clear(); synchronized (loaded) { loaded.clear(); } }
        if (tts != null) { try { tts.stop(); tts.shutdown(); } catch (Exception ignored) {} tts = null; ttsReady = false; }
    }
}
