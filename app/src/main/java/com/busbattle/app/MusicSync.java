package com.busbattle.app;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;

/**
 * Listens to the music the phone is playing (Spotify, YouTube, anything) and reports bass / mid / high levels and beats,
 * so the hall's lights and dancers follow the real song.
 * Source "output": Android's visualizer on the global output mix (no microphone sound, works with any app on most phones).
 * Source "mic": the microphone, as a fallback for phones that block the visualizer.
 */
final class MusicSync {

    interface Out {
        /** Levels 0..1, about 30 times a second; beat is true on a detected kick. */
        void level(float bass, float mid, float high, boolean beat);
        void state(String s);
    }

    private final Out out;
    private Visualizer vis;
    private AudioRecord rec;
    private Thread micThread;
    private volatile boolean on;

    // normalisation and beat detection state
    private float maxB = 1e-3f, maxM = 1e-3f, maxH = 1e-3f;
    private final float[] hist = new float[43];
    private int hi;
    private long lastBeat;

    MusicSync(Out out) { this.out = out; }

    synchronized void start(String source) {
        stop();
        on = true;
        if (!"mic".equals(source) && startVisualizer()) { out.state("output"); return; }
        if (startMic()) { out.state("mic"); return; }
        on = false;
        out.state("fail");
    }

    synchronized void stop() {
        on = false;
        if (vis != null) { try { vis.setEnabled(false); vis.release(); } catch (Exception ignored) {} vis = null; }
        if (rec != null) { try { rec.stop(); } catch (Exception ignored) {} try { rec.release(); } catch (Exception ignored) {} rec = null; }
        if (micThread != null) { micThread.interrupt(); micThread = null; }
    }

    // ------------------------------------------------------------ output mix (Visualizer, session 0)

    private boolean startVisualizer() {
        try {
            Visualizer v = new Visualizer(0);
            int[] range = Visualizer.getCaptureSizeRange();
            v.setCaptureSize(Math.min(1024, range[1]));
            final int rate = v.getSamplingRate() / 1000;   // Hz
            int captureRate = Math.min(Visualizer.getMaxCaptureRate(), 30000);
            v.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override public void onWaveFormDataCapture(Visualizer vz, byte[] w, int sr) {}
                @Override public void onFftDataCapture(Visualizer vz, byte[] fft, int sr) { if (on) analyseFft(fft, rate); }
            }, captureRate, false, true);
            v.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
            v.setEnabled(true);
            vis = v;
            return true;
        } catch (Throwable e) {
            if (vis != null) try { vis.release(); } catch (Exception ignored) {}
            vis = null;
            return false;
        }
    }

    private void analyseFft(byte[] fft, int rate) {
        int n = fft.length / 2;
        if (n < 8 || rate <= 0) return;
        float binHz = rate / 2f / n, b = 0, m = 0, h = 0;
        for (int k = 1; k < n; k++) {
            float re = fft[2 * k], im = fft[2 * k + 1], mag = (float) Math.sqrt(re * re + im * im), f = k * binHz;
            if (f < 150) b += mag; else if (f < 2000) m += mag; else if (f < 9000) h += mag * 0.6f;
        }
        emit(b, m, h);
    }

    // ------------------------------------------------------------ microphone fallback

    private boolean startMic() {
        try {
            final int sr = 22050;
            int min = AudioRecord.getMinBufferSize(sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            final AudioRecord r = new AudioRecord(MediaRecorder.AudioSource.MIC, sr, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 4096));
            if (r.getState() != AudioRecord.STATE_INITIALIZED) { r.release(); return false; }
            r.startRecording();
            rec = r;
            micThread = new Thread(() -> {
                short[] buf = new short[735];   // ~33 ms
                float lp = 0, lp2 = 0, hp = 0, prev = 0;
                float aL = (float) (1 - Math.exp(-2 * Math.PI * 150 / sr)), aM = (float) (1 - Math.exp(-2 * Math.PI * 2000 / sr));
                while (on && !Thread.currentThread().isInterrupted()) {
                    int got = r.read(buf, 0, buf.length);
                    if (got <= 0) continue;
                    float b = 0, m = 0, h = 0;
                    for (int i = 0; i < got; i++) {
                        float x = buf[i] / 32768f;
                        lp += aL * (x - lp);          // bass: below ~150 Hz
                        lp2 += aM * (x - lp2);        // below ~2 kHz
                        hp = x - lp2;                 // highs: above ~2 kHz
                        b += lp * lp; m += (lp2 - lp) * (lp2 - lp); h += hp * hp;
                        prev = x;
                    }
                    emit((float) Math.sqrt(b / got), (float) Math.sqrt(m / got), (float) Math.sqrt(h / got));
                }
            }, "music-mic");
            micThread.start();
            return true;
        } catch (Throwable e) {
            rec = null;
            return false;
        }
    }

    // ------------------------------------------------------------ shared: normalise, detect beats, report

    private void emit(float b, float m, float h) {
        // slow-decaying peaks keep each band in 0..1 whatever the volume
        maxB = Math.max(b, maxB * 0.997f); maxM = Math.max(m, maxM * 0.997f); maxH = Math.max(h, maxH * 0.997f);
        float nb = clamp(b / maxB), nm = clamp(m / maxM), nh = clamp(h / maxH);
        // beat: bass clearly above its average of the last ~1.4 s, at most every 280 ms
        float avg = 0;
        for (float v : hist) avg += v;
        avg /= hist.length;
        hist[hi] = nb; hi = (hi + 1) % hist.length;
        long now = System.currentTimeMillis();
        boolean beat = nb > 0.35f && nb > avg * 1.35f && now - lastBeat > 280;
        if (beat) lastBeat = now;
        out.level(nb, nm, nh, beat);
    }

    private static float clamp(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
}
