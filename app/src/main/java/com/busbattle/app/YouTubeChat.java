package com.busbattle.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Reads a YouTube live chat the way the YouTube web page does (no API key, no quota) and reports each comment,
 * Super Chat, Super Sticker and membership as the same JSON events the game already understands.
 */
public final class YouTubeChat implements LiveSource {

    String base = "https://www.youtube.com";   // overridable for tests
    int retrySeconds = 15;

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String input;
    private final TikTokLive.Listener out;
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build();
    private final LinkedHashSet<String> seen = new LinkedHashSet<>();
    private volatile boolean running;
    private Thread thread;

    public YouTubeChat(String input, TikTokLive.Listener out) {
        this.input = input == null ? "" : input.trim();
        this.out = out;
    }

    @Override public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::loop, "youtube-chat");
        thread.setDaemon(true);
        thread.start();
    }

    @Override public synchronized void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    // ---------------------------------------------------------------- what the user typed

    /** An 11-character video id, or null when the input names a channel. */
    static String videoId(String s) {
        s = s.trim();
        if (s.matches("[A-Za-z0-9_-]{11}")) return s;
        Matcher m = Pattern.compile("(?:youtu\\.be/|[?&]v=|/(?:live|shorts|embed)/)([A-Za-z0-9_-]{11})").matcher(s);
        return m.find() ? m.group(1) : null;
    }

    /** The channel path for "/live": "@handle", "channel/UC…", "c/name" or "user/name". */
    static String channelPath(String s) {
        s = s.trim().replaceFirst("(?i)^https?://", "").replaceFirst("(?i)^(www\\.|m\\.)?youtube\\.com/?", "");
        s = s.replaceAll("[?#].*$", "").replaceAll("/+$", "").replaceAll("/(live|streams|videos|featured)$", "");
        if (s.isEmpty()) return null;
        if (s.startsWith("@") || s.startsWith("channel/") || s.startsWith("c/") || s.startsWith("user/")) return s;
        if (s.startsWith("UC") && s.length() == 24) return "channel/" + s;
        if (!s.contains("/")) return "@" + s;
        return null;
    }

    // ---------------------------------------------------------------- main loop

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
            int wait = Math.min(60, retrySeconds * Math.max(1, fails));
            log(why + " · reintento en " + wait + " s");
            status(false, "Sin conexión: " + cut(why, 140));
            try { Thread.sleep(wait * 1000L); } catch (InterruptedException e) { break; }
        }
    }

    private String session() throws Exception {
        String vid = videoId(input);
        if (vid == null) {
            String ch = channelPath(input);
            if (ch == null) throw new IOException("Escribe tu canal (@usuario) o el enlace del directo");
            status(false, "Buscando el directo de " + ch);
            String page = get(base + "/" + ch + "/live");
            Matcher m = Pattern.compile("\"currentVideoEndpoint\":\\{.*?\"videoId\":\"([A-Za-z0-9_-]{11})\"", Pattern.DOTALL).matcher(page);
            if (m.find()) vid = m.group(1);
            else {
                m = Pattern.compile("<link rel=\"canonical\" href=\"https://www\\.youtube\\.com/watch\\?v=([A-Za-z0-9_-]{11})\"").matcher(page);
                if (m.find()) vid = m.group(1);
            }
            if (vid == null) throw new IOException("El canal " + ch + " no está en directo ahora");
        }
        status(false, "Abriendo el chat del directo " + vid);
        String page = get(base + "/live_chat?is_popout=1&v=" + vid);
        String key = find(page, "\"INNERTUBE_API_KEY\":\"([^\"]+)\"");
        String ver = find(page, "\"INNERTUBE_CLIENT_VERSION\":\"([^\"]+)\"");
        Matcher dm = Pattern.compile("(?:window\\[\"ytInitialData\"\\]|var ytInitialData)\\s*=\\s*(\\{.*?\\});\\s*</script>", Pattern.DOTALL).matcher(page);
        if (key == null || ver == null || !dm.find())
            throw new IOException("Este vídeo no tiene chat en directo (¿ha terminado el directo o el chat está desactivado?)");
        JSONObject chat = new JSONObject(dm.group(1)).optJSONObject("contents");
        chat = chat == null ? null : chat.optJSONObject("liveChatRenderer");
        if (chat == null) throw new IOException("Este vídeo no tiene chat en directo");
        String cont = liveChatContinuation(chat);
        if (cont == null) throw new IOException("No se encontró el chat en directo");

        log("Chat del directo " + vid + " abierto");
        status(true, "Conectado al chat de YouTube");
        boolean first = true;
        while (running) {
            JSONObject body = new JSONObject()
                    .put("context", new JSONObject().put("client", new JSONObject().put("clientName", "WEB").put("clientVersion", ver).put("hl", "es").put("gl", "ES")))
                    .put("continuation", cont);
            JSONObject res = new JSONObject(post(base + "/youtubei/v1/live_chat/get_live_chat?key=" + key + "&prettyPrint=false", body.toString()));
            JSONObject lc = res.optJSONObject("continuationContents");
            lc = lc == null ? null : lc.optJSONObject("liveChatContinuation");
            if (lc == null) return "El directo ha terminado";
            JSONArray actions = lc.optJSONArray("actions");
            // The first answer repeats recent history: remember it so old comments do not score.
            if (actions != null) for (int i = 0; i < actions.length(); i++) handle(actions.optJSONObject(i), first);
            first = false;
            JSONArray cs = lc.optJSONArray("continuations");
            JSONObject next = cs == null ? null : cs.optJSONObject(0);
            if (next == null) return "El directo ha terminado";
            JSONObject data = null;
            for (Iterator<String> it = next.keys(); it.hasNext(); ) { JSONObject o = next.optJSONObject(it.next()); if (o != null && o.has("continuation")) { data = o; break; } }
            if (data == null) return "El directo ha terminado";
            cont = data.getString("continuation");
            long wait = Math.max(1000, Math.min(5000, data.optLong("timeoutMs", 3000)));
            Thread.sleep(wait);
        }
        return "Detenido";
    }

    /** Prefer "Live chat" (every message) over "Top chat" (filtered by YouTube). */
    static String liveChatContinuation(JSONObject chat) {
        try {
            JSONArray items = chat.getJSONObject("header").getJSONObject("liveChatHeaderRenderer").getJSONObject("viewSelector")
                    .getJSONObject("sortFilterSubMenuRenderer").getJSONArray("subMenuItems");
            JSONObject last = items.getJSONObject(items.length() - 1).getJSONObject("continuation");
            for (Iterator<String> it = last.keys(); it.hasNext(); ) {
                JSONObject o = last.optJSONObject(it.next());
                if (o != null && o.has("continuation")) return o.getString("continuation");
            }
        } catch (Exception ignored) {}
        JSONArray cs = chat.optJSONArray("continuations");
        JSONObject c = cs == null ? null : cs.optJSONObject(0);
        if (c != null) for (Iterator<String> it = c.keys(); it.hasNext(); ) {
            JSONObject o = c.optJSONObject(it.next());
            if (o != null && o.has("continuation")) return o.optString("continuation");
        }
        return null;
    }

    // ---------------------------------------------------------------- chat items

    void handle(JSONObject action, boolean history) {
        if (action == null) return;
        JSONObject add = action.optJSONObject("addChatItemAction");
        JSONObject item = add == null ? null : add.optJSONObject("item");
        if (item == null) return;
        String type = item.keys().hasNext() ? item.keys().next() : "";
        JSONObject r = item.optJSONObject(type);
        if (r == null) return;
        String id = r.optString("id", "");
        if (!id.isEmpty()) {
            synchronized (seen) {
                if (!seen.add(id)) return;
                if (seen.size() > 3000) { Iterator<String> it = seen.iterator(); for (int i = 0; i < 1000; i++) { it.next(); it.remove(); } }
            }
        }
        if (history) return;
        try {
            JSONObject who = who(r);
            String text = runs(r.optJSONObject("message"));
            switch (type) {
                case "liveChatTextMessageRenderer":
                    send(who.put("type", "chat").put("text", text));
                    break;
                case "liveChatPaidMessageRenderer":
                case "liveChatPaidStickerRenderer": {
                    String amount = simple(r.optJSONObject("purchaseAmountText"));
                    int buses = busesFor(amount);
                    log("Super Chat de " + who.optString("nick") + ": " + amount + " → " + buses + " buses");
                    if (!text.isEmpty()) send(new JSONObject(who.toString()).put("type", "chat").put("text", text));
                    send(who.put("type", "gift").put("gift", "Super Chat").put("label", "Super Chat " + amount).put("count", 1).put("coins", buses));
                    break;
                }
                case "liveChatMembershipItemRenderer": {
                    log("Nuevo miembro: " + who.optString("nick"));
                    send(who.put("type", "gift").put("gift", "Miembro").put("label", "Nuevo miembro").put("count", 1).put("coins", 50));
                    break;
                }
                case "liveChatSponsorshipsGiftPurchaseAnnouncementRenderer": {
                    JSONObject h = r.optJSONObject("header");
                    h = h == null ? null : h.optJSONObject("liveChatSponsorshipsHeaderRenderer");
                    JSONObject w2 = h == null ? who : who(h);
                    String t = h == null ? "" : runs(h.optJSONObject("primaryText"));
                    Matcher m = Pattern.compile("(\\d+)").matcher(t);
                    int n = m.find() ? Integer.parseInt(m.group(1)) : 1;
                    log("Regala " + n + " membresías: " + w2.optString("nick"));
                    send(w2.put("type", "gift").put("gift", "Membresías").put("label", n + " membresías regaladas").put("count", n).put("coins", 50));
                    break;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            log("Mensaje no leído: " + e.getMessage());
        }
    }

    private static JSONObject who(JSONObject r) throws Exception {
        String nick = simple(r.optJSONObject("authorName"));
        String chan = r.optString("authorExternalChannelId", "");
        String avatar = "";
        JSONObject ph = r.optJSONObject("authorPhoto");
        JSONArray th = ph == null ? null : ph.optJSONArray("thumbnails");
        if (th != null && th.length() > 0) avatar = th.optJSONObject(th.length() - 1).optString("url", "");
        if (avatar.startsWith("//")) avatar = "https:" + avatar;
        if (!avatar.startsWith("https://")) avatar = "";
        String shown = nick.startsWith("@") ? nick.substring(1) : nick;
        return new JSONObject().put("user", chan.isEmpty() ? shown : chan).put("nick", shown.isEmpty() ? "Fan" : shown).put("avatar", avatar);
    }

    static String runs(JSONObject msg) {
        if (msg == null) return "";
        if (msg.has("simpleText")) return msg.optString("simpleText");
        JSONArray rs = msg.optJSONArray("runs");
        if (rs == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < rs.length(); i++) {
            JSONObject x = rs.optJSONObject(i);
            if (x == null) continue;
            if (x.has("text")) b.append(x.optString("text"));
            else if (x.has("emoji")) {
                JSONObject e = x.optJSONObject("emoji");
                boolean custom = e.optBoolean("isCustomEmoji", false);
                JSONArray sc = e.optJSONArray("shortcuts");
                b.append(custom && sc != null && sc.length() > 0 ? " " + sc.optString(0) + " " : e.optString("emojiId", ""));
            }
        }
        return b.toString();
    }

    private static String simple(JSONObject o) { return o == null ? "" : o.has("simpleText") ? o.optString("simpleText") : runs(o); }

    /** Super Chat value in buses: about 10 buses per euro, whatever the currency. */
    static int busesFor(String amount) {
        if (amount == null) return 10;
        String a = amount.replace(' ', ' ').trim();
        Matcher m = Pattern.compile("(\\d[\\d.,\\s]*)").matcher(a);
        if (!m.find()) return 10;
        String num = m.group(1).replaceAll("\\s", "");
        // "1.234,56" / "1,234.56" / "5,00" / "5.00"
        int lc = num.lastIndexOf(','), ld = num.lastIndexOf('.');
        if (lc > ld) num = num.replace(".", "").replace(',', '.');
        else num = num.replace(",", "");
        double v;
        try { v = Double.parseDouble(num); } catch (Exception e) { return 10; }
        String cur = a.replace(m.group(1), "").trim().toUpperCase(Locale.ROOT);
        double eur = v * rate(cur);
        return (int) Math.max(1, Math.round(eur * 10));
    }

    private static double rate(String c) {
        if (c.contains("€") || c.contains("EUR")) return 1;
        if (c.contains("£") || c.contains("GBP")) return 1.17;
        if (c.contains("MX")) return 0.05;
        if (c.contains("ARS")) return 0.001;
        if (c.contains("COP")) return 0.00023;
        if (c.contains("CLP")) return 0.001;
        if (c.contains("PEN") || c.contains("S/")) return 0.25;
        if (c.contains("MAD") || c.contains("DH")) return 0.092;
        if (c.contains("SAR") || c.contains("AED")) return 0.25;
        if (c.contains("¥") || c.contains("JPY")) return 0.006;
        if (c.contains("₹") || c.contains("INR")) return 0.011;
        if (c.contains("R$") || c.contains("BRL")) return 0.17;
        if (c.contains("CA$") || c.contains("CAD")) return 0.67;
        if (c.contains("A$") || c.contains("AUD")) return 0.6;
        if (c.contains("CHF")) return 1.05;
        if (c.contains("$") || c.contains("USD")) return 0.92;
        return 1;
    }

    // ---------------------------------------------------------------- HTTP

    private String get(String url) throws IOException {
        Request req = new Request.Builder().url(url).header("User-Agent", UA)
                .header("Accept-Language", "es-ES,es;q=0.9").header("Cookie", "SOCS=CAI; CONSENT=YES+").build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() >= 400) throw new IOException("YouTube respondió " + res.code());
            return res.body() == null ? "" : res.body().string();
        }
    }

    private String post(String url, String json) throws IOException {
        Request req = new Request.Builder().url(url).header("User-Agent", UA).header("Origin", "https://www.youtube.com")
                .header("Cookie", "SOCS=CAI; CONSENT=YES+").post(RequestBody.create(json, JSON)).build();
        try (Response res = http.newCall(req).execute()) {
            if (res.code() >= 400) throw new IOException("YouTube respondió " + res.code() + " al leer el chat");
            return res.body() == null ? "{}" : res.body().string();
        }
    }

    private static String find(String s, String re) {
        Matcher m = Pattern.compile(re).matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static String cut(String s, int n) { s = s == null ? "" : s.replace('\n', ' '); return s.length() > n ? s.substring(0, n) + "…" : s; }

    private void send(JSONObject o) { out.onEvent(o.toString()); }

    private void status(boolean ok, String text) {
        try { send(new JSONObject().put("type", "status").put("ok", ok).put("text", text)); } catch (Exception ignored) {}
    }

    private void log(String line) { out.onLog(line); }
}
