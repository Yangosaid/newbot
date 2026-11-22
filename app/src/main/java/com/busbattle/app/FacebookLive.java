package com.busbattle.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Facebook Live through the official Graph API, with the access token of the Page that is live:
 * finds the Page's current live video, then follows its comments and reactions as server-sent events
 * (streaming-graph.facebook.com). Comments become "chat" events (name and photo), reactions arrive as
 * anonymous totals and become "reactions" events. Personal-profile lives cannot be read by apps.
 */
public final class FacebookLive implements LiveSource {

    private static final String GRAPH = "https://graph.facebook.com/v21.0/", STREAM = "https://streaming-graph.facebook.com/";
    private final String input, token;
    private final TikTokLive.Listener out;
    private volatile boolean running;
    private Thread thread, reactThread;
    private volatile HttpURLConnection comments, reactions;

    public FacebookLive(String input, String token, TikTokLive.Listener out) {
        this.input = input == null ? "" : input.trim();
        this.token = token == null ? "" : token.trim();
        this.out = out;
    }

    @Override public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::loop, "facebook-live");
        thread.setDaemon(true);
        thread.start();
    }

    @Override public synchronized void stop() {
        running = false;
        if (thread != null) thread.interrupt();
        if (reactThread != null) reactThread.interrupt();
        close(comments);
        close(reactions);
    }

    /** A live video id typed directly or inside a link (…/videos/123…), or null. */
    static String videoId(String s) {
        if (s.matches("\\d{8,}")) return s;
        Matcher m = Pattern.compile("(?:/videos/|[?&]v=|/live/)(\\d{8,})").matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private void loop() {
        int fails = 0;
        while (running) {
            String why;
            try {
                why = session();
                fails = 0;
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                why = e.getMessage() == null ? e.toString() : e.getMessage();
                fails++;
            }
            if (!running) break;
            int wait = Math.min(60, 8 * Math.max(1, fails));
            out.onLog(why + " · reintento en " + wait + " s");
            status(false, "Sin conexión: " + why);
            try { Thread.sleep(wait * 1000L); } catch (InterruptedException e) { break; }
        }
    }

    private String session() throws Exception {
        if (token.isEmpty()) throw new IOException("Pega el token de tu Página de Facebook en «Token»");
        String vid = videoId(input);
        if (vid == null) {
            String page = input.isEmpty() ? "me" : input.replaceFirst("(?i)^https?://(www\\.|m\\.|web\\.)?facebook\\.com/", "").replaceAll("[/?#].*$", "");
            status(false, "Buscando el directo de la Página…");
            JSONObject r = new JSONObject(get(GRAPH + enc(page) + "/live_videos?broadcast_status=" + enc("[\"LIVE\"]") + "&fields=id,title&limit=1&access_token=" + enc(token)));
            JSONArray d = r.optJSONArray("data");
            if (d == null || d.length() == 0) throw new IOException("La Página no está en directo ahora");
            vid = d.getJSONObject(0).getString("id");
        }
        final String video = vid;
        status(true, "Conectado al directo de Facebook");
        out.onLog("Directo de Facebook " + video);
        reactThread = new Thread(() -> followReactions(video), "facebook-reactions");
        reactThread.setDaemon(true);
        reactThread.start();
        /* comments: blocks until the stream ends or fails */
        comments = open(STREAM + video + "/live_comments?access_token=" + enc(token) + "&comment_rate=ten_per_second&fields=" + enc("from{name,id,picture},message"));
        try (BufferedReader br = new BufferedReader(new InputStreamReader(comments.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = br.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                try {
                    JSONObject c = new JSONObject(line.substring(5).trim());
                    String text = c.optString("message", "");
                    JSONObject from = c.optJSONObject("from");
                    JSONObject who = new JSONObject();
                    if (from != null) {
                        String pic = "";
                        JSONObject p = from.optJSONObject("picture");
                        if (p != null && p.optJSONObject("data") != null) pic = p.getJSONObject("data").optString("url", "");
                        who.put("user", "fb:" + from.optString("id")).put("nick", from.optString("name", "Fan")).put("avatar", pic);
                    } else who.put("user", "fb:anon:" + c.optString("id")).put("nick", "Fan").put("avatar", "");
                    if (!text.isEmpty()) out.onEvent(who.put("type", "chat").put("text", text).toString());
                } catch (Exception ignored) {}
            }
        } finally {
            close(comments);
            if (reactThread != null) reactThread.interrupt();
        }
        return "El directo de Facebook terminó o se cortó";
    }

    private void followReactions(String video) {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                reactions = open(STREAM + video + "/live_reactions?access_token=" + enc(token) + "&fields=reaction_stream");
                try (BufferedReader br = new BufferedReader(new InputStreamReader(reactions.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while (running && (line = br.readLine()) != null) {
                        if (!line.startsWith("data:")) continue;
                        JSONArray rs = new JSONObject(line.substring(5).trim()).optJSONArray("reaction_stream");
                        if (rs == null) continue;
                        JSONObject by = new JSONObject();
                        int total = 0;
                        for (int i = 0; i < rs.length(); i++) {
                            JSONObject r = rs.getJSONObject(i);
                            int v = r.optInt("value", 0);
                            if (v <= 0) continue;
                            by.put(r.optString("key", "LIKE").toLowerCase(), v);
                            total += v;
                        }
                        if (total > 0) out.onEvent(new JSONObject().put("type", "reactions").put("count", total).put("by", by).toString());
                    }
                }
            } catch (Exception e) {
                if (!running) return;
                try { Thread.sleep(5000); } catch (InterruptedException ie) { return; }
            } finally { close(reactions); }
        }
    }

    // ---------------------------------------------------------------- http

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(120000);
        c.setRequestProperty("Accept", "text/event-stream");
        int code = c.getResponseCode();
        if (code >= 400) throw new IOException(error(c));
        return c;
    }

    private static String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        try {
            if (c.getResponseCode() >= 400) throw new IOException(error(c));
            return read(c.getInputStream());
        } finally { c.disconnect(); }
    }

    /** Facebook's error message, in plain words when it is the usual token problem. */
    private static String error(HttpURLConnection c) {
        try {
            String body = read(c.getErrorStream());
            JSONObject e = new JSONObject(body).optJSONObject("error");
            if (e != null) {
                int code = e.optInt("code");
                if (code == 190) return "El token caducó o no es válido: genera uno nuevo";
                if (code == 10 || code == 200) return "Al token le faltan permisos (pages_read_engagement, pages_read_user_content)";
                return e.optString("message", "Error de Facebook");
            }
            return "HTTP " + c.getResponseCode();
        } catch (Exception x) { return "Error de conexión con Facebook"; }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String l;
            while ((l = br.readLine()) != null) sb.append(l).append('\n');
        }
        return sb.toString();
    }

    private static void close(HttpURLConnection c) { try { if (c != null) c.disconnect(); } catch (Exception ignored) {} }

    private static String enc(String s) { try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; } }

    private void status(boolean ok, String text) {
        try { out.onEvent(new JSONObject().put("type", "status").put("ok", ok).put("text", text).toString()); } catch (Exception ignored) {}
    }
}
