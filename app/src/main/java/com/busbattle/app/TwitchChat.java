package com.busbattle.app;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLSocketFactory;

/**
 * Twitch chat, read-only and anonymous (no account, no token): Twitch IRC over TLS with message tags.
 * Messages become "chat" events; Bits, subscriptions, gifted subs and raids become "gift" events the games
 * already understand (1 bit ≈ 1 coin; Tier 1/2/3 sub ≈ 500/1000/2500).
 */
public final class TwitchChat implements LiveSource {

    private final String channel;
    private final TikTokLive.Listener out;
    private volatile boolean running;
    private Thread thread;
    private volatile Socket socket;

    public TwitchChat(String input, TikTokLive.Listener out) {
        this.channel = normalize(input);
        this.out = out;
    }

    /** "https://twitch.tv/Name", "@Name" or "Name" → "name". */
    static String normalize(String s) {
        if (s == null) return "";
        s = s.trim().replaceFirst("(?i)^https?://(www\\.|m\\.)?twitch\\.tv/", "").replaceAll("[/?#].*$", "").replaceFirst("^[@#]", "");
        return s.toLowerCase();
    }

    @Override public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::loop, "twitch-chat");
        thread.setDaemon(true);
        thread.start();
    }

    @Override public synchronized void stop() {
        running = false;
        if (thread != null) thread.interrupt();
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }

    private void loop() {
        int fails = 0;
        while (running) {
            String why;
            try { why = session(); fails = 0; }
            catch (Exception e) { why = e.getMessage() == null ? e.toString() : e.getMessage(); fails++; }
            if (!running) break;
            int wait = Math.min(60, 5 * Math.max(1, fails));
            out.onLog(why + " · reintento en " + wait + " s");
            status(false, "Sin conexión: " + why);
            try { Thread.sleep(wait * 1000L); } catch (InterruptedException e) { break; }
        }
    }

    private String session() throws Exception {
        if (channel.isEmpty()) throw new IOException("Escribe el nombre de tu canal de Twitch");
        Socket s = SSLSocketFactory.getDefault().createSocket();
        socket = s;
        s.connect(new InetSocketAddress("irc.chat.twitch.tv", 6697), 15000);
        s.setSoTimeout(6 * 60 * 1000);
        OutputStream os = s.getOutputStream();
        send(os, "CAP REQ :twitch.tv/tags twitch.tv/commands");
        send(os, "PASS SCHMOOPIIE");
        send(os, "NICK justinfan" + (10000 + (int) (Math.random() * 80000)));
        send(os, "JOIN #" + channel);
        status(true, "Conectado al chat de #" + channel);
        out.onLog("Chat de Twitch #" + channel);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = br.readLine()) != null) {
                if (line.startsWith("PING")) { send(os, "PONG" + line.substring(4)); continue; }
                try { handle(line); } catch (Exception ignored) {}
            }
        } finally { try { s.close(); } catch (Exception ignored) {} }
        return "El chat de Twitch se cortó";
    }

    private void handle(String line) throws Exception {
        Map<String, String> tags = new HashMap<>();
        if (line.startsWith("@")) {
            int sp = line.indexOf(' ');
            for (String kv : line.substring(1, sp).split(";")) {
                int eq = kv.indexOf('=');
                if (eq > 0) tags.put(kv.substring(0, eq), unescape(kv.substring(eq + 1)));
            }
            line = line.substring(sp + 1);
        }
        String[] parts = line.split(" ", 4);
        if (parts.length < 3) return;
        String cmd = parts[1];
        String text = parts.length >= 4 && parts[3].startsWith(":") ? parts[3].substring(1) : "";
        String login = parts[0].startsWith(":") && parts[0].contains("!") ? parts[0].substring(1, parts[0].indexOf('!')) : tags.getOrDefault("login", "");
        if (login.isEmpty()) login = tags.getOrDefault("login", "fan");
        String nick = tags.getOrDefault("display-name", login);
        if (nick.isEmpty()) nick = login;
        JSONObject who = new JSONObject().put("user", "tw:" + login).put("nick", nick).put("avatar", "https://unavatar.io/twitch/" + login);

        if ("PRIVMSG".equals(cmd)) {
            int bits = parseInt(tags.get("bits"));
            if (bits > 0) out.onEvent(new JSONObject(who.toString()).put("type", "gift").put("gift", "Bits").put("label", "💎 " + bits + " bits").put("count", 1).put("coins", bits).toString());
            String clean = text.replaceAll("(?i)\\b(cheer|biblethump|cheerwhal|corgo|uni|showlove|party|seemsgood|pride|kappa|frankerz|heyguys|dansgame|elegiggle|trihard|kreygasm|4head|swiftrage|notlikethis|failfish|vohiyo|pjsalt|mrdestructoid|bday|ripcheer|shamrock)\\d+\\b", "").trim();
            if (!clean.isEmpty()) out.onEvent(new JSONObject(who.toString()).put("type", "chat").put("text", clean).toString());
        } else if ("USERNOTICE".equals(cmd)) {
            String id = tags.getOrDefault("msg-id", "");
            String plan = tags.getOrDefault("msg-param-sub-plan", "1000");
            int tier = "3000".equals(plan) ? 2500 : "2000".equals(plan) ? 1000 : 500;
            if ("sub".equals(id) || "resub".equals(id)) {
                int months = parseInt(tags.get("msg-param-cumulative-months"));
                out.onEvent(new JSONObject(who.toString()).put("type", "gift").put("gift", "Suscripción").put("label", "⭐ Suscripción" + (months > 1 ? " · " + months + " meses" : "")).put("count", 1).put("coins", tier).toString());
            } else if ("submysterygift".equals(id)) {
                int n = Math.max(1, parseInt(tags.get("msg-param-mass-gift-count")));
                out.onEvent(new JSONObject(who.toString()).put("type", "gift").put("gift", "Subs regaladas").put("label", "🎁 " + n + " subs regaladas").put("count", n).put("coins", tier).toString());
            } else if ("subgift".equals(id) && tags.get("msg-param-origin-id") == null) {
                out.onEvent(new JSONObject(who.toString()).put("type", "gift").put("gift", "Sub regalada").put("label", "🎁 Sub regalada").put("count", 1).put("coins", tier).toString());
            } else if ("raid".equals(id)) {
                int viewers = parseInt(tags.get("msg-param-viewerCount"));
                out.onEvent(new JSONObject(who.toString()).put("type", "gift").put("gift", "Raid").put("label", "🚀 Raid de " + viewers).put("count", 1).put("coins", Math.min(1500, 100 + viewers * 10)).toString());
            }
            if (!text.isEmpty()) out.onEvent(new JSONObject(who.toString()).put("type", "chat").put("text", text).toString());
        }
    }

    private static String unescape(String v) {
        return v.replace("\\s", " ").replace("\\:", ";").replace("\\\\", "\\").replace("\\r", "").replace("\\n", "");
    }

    private static int parseInt(String s) { try { return s == null ? 0 : Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }

    private static void send(OutputStream os, String line) throws IOException {
        os.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.flush();
    }

    private void status(boolean ok, String text) {
        try { out.onEvent(new JSONObject().put("type", "status").put("ok", ok).put("text", text).toString()); } catch (Exception ignored) {}
    }
}
