package com.busbattle.app;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Connects to a TikTok LIVE room and reports gifts and comments as the same JSON events the old Termux bridge sent.
 * It follows the protocol of the tiktok-live-connector library (v2.5): the room id comes from TikTok, the WebSocket
 * address is signed by Euler Stream, and the push frames are protobuf.
 */
public final class TikTokLive {

    public interface Listener {
        /** A JSON event for the game: {type:'status'|'gift'|'chat', ...}. Called on a background thread. */
        void onEvent(String json);
        /** One line for the on-screen log. Called on a background thread. */
        void onLog(String line);
    }

    // Overridable so the connection can be tested against a local fake server.
    String webBase = "https://www.tiktok.com";
    String eulerBase = "https://api.eulerstream.com";
    int retrySeconds = 15;

    static final String UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/128.0.2739.79";
    private static final String SIGN_UA = "tiktok-live-connector/2.5.0 android";
    private static final String[] SESSION_COOKIES = {"sessionid", "sessionid_ss", "sid_tt", "sid_guard", "tt-target-idc"};

    private final String user, key;
    private final Listener out;
    private final OkHttpClient http;
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final Map<String, long[]> streaks = new HashMap<>();   // key -> {count, time}
    private final SecureRandom rnd = new SecureRandom();
    private final String deviceId;
    private volatile boolean running;
    private volatile WebSocket ws;
    private Thread thread;
    private boolean seenGift;

    public TikTokLive(String user, String key, Listener out) {
        this.user = normalize(user);
        this.key = key == null ? "" : key.trim();
        this.out = out;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
                .pingInterval(0, TimeUnit.SECONDS).build();
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < 19; i++) d.append(rnd.nextInt(10));
        deviceId = d.toString();
    }

    static String normalize(String u) {
        u = u == null ? "" : u.trim();
        u = u.replace("https://www.tiktok.com/", "").replace("/live", "").replace("@", "");
        int q = u.indexOf('?');
        if (q >= 0) u = u.substring(0, q);
        return u.replace("/", "").trim();
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::loop, "tiktok-live");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        WebSocket w = ws;
        if (w != null) w.cancel();
        if (thread != null) thread.interrupt();
    }

    // ---------------------------------------------------------------- main loop

    private void loop() {
        int fails = 0;
        while (running) {
            String why;
            try {
                status(false, "Buscando el directo de @" + user);
                String roomId = roomId();
                log("Sala " + roomId + ": pidiendo la conexión firmada");
                why = session(roomId);
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
            hint(why);
            status(false, "Sin conexión: " + cut(why, 140));
            try { Thread.sleep(wait * 1000L); } catch (InterruptedException e) { break; }
        }
    }

    private void hint(String why) {
        if (why.matches("(?is).*(429|limit|too many).*"))
            log("» Euler Stream limitó las conexiones gratuitas. Crea una clave gratis en eulerstream.com y ponla en Conexión.");
        else if (why.matches("(?is).*(no está en directo|offline|room id).*"))
            log("» Comprueba que el directo ya ha empezado y que el usuario @" + user + " es correcto.");
    }

    // ---------------------------------------------------------------- room id

    private String roomId() throws Exception {
        List<String> errs = new ArrayList<>();
        try {
            String html = get(webBase + "/@" + enc(user) + "/live", "text/html,application/json,application/protobuf");
            Matcher m = Pattern.compile("<script id=\"SIGI_STATE\" type=\"application/json\">(.*?)</script>", Pattern.DOTALL).matcher(html);
            if (!m.find()) throw new IOException("TikTok no devolvió los datos de la página (¿captcha?)");
            JSONObject info = new JSONObject(m.group(1)).optJSONObject("LiveRoom");
            info = info == null ? null : info.optJSONObject("liveRoomUserInfo");
            if (info == null) throw new IOException("página sin LiveRoom");
            JSONObject lr = info.optJSONObject("liveRoom");
            if (lr != null && lr.optInt("status", 0) == 4) throw new Offline();
            String id = info.optJSONObject("user") == null ? "" : info.optJSONObject("user").optString("roomId", "");
            if (id.isEmpty() || id.equals("0")) throw new IOException("página sin roomId");
            return id;
        } catch (Offline e) { throw e; } catch (Exception e) { errs.add("web: " + e.getMessage()); }

        try {
            Map<String, String> q = webParams();
            q.put("uniqueId", user);
            q.put("sourceType", "54");
            JSONObject j = new JSONObject(get(webBase + "/api-live/user/room/?" + query(q), "application/json"));
            if (j.optInt("statusCode", 0) != 0) throw new IOException("API " + j.optInt("statusCode") + " " + j.optString("message"));
            JSONObject data = j.optJSONObject("data");
            JSONObject lr = data == null ? null : data.optJSONObject("liveRoom");
            if (lr != null && lr.optInt("status", 0) == 4) throw new Offline();
            String id = data == null || data.optJSONObject("user") == null ? "" : data.optJSONObject("user").optString("roomId", "");
            if (id.isEmpty() || id.equals("0")) throw new IOException("API sin roomId");
            return id;
        } catch (Offline e) { throw e; } catch (Exception e) { errs.add("api: " + e.getMessage()); }

        try {
            Request.Builder r = new Request.Builder().url(eulerBase + "/webcast/anchors/" + enc(user) + "/room_id" + keyQuery("?"))
                    .header("Accept", "application/json").header("User-Agent", SIGN_UA);
            if (!key.isEmpty()) r.header("x-api-key", key);
            try (Response res = http.newCall(r.build()).execute()) {
                String body = res.body() == null ? "" : res.body().string();
                JSONObject j = body.startsWith("{") ? new JSONObject(body) : new JSONObject();
                int code = j.optInt("code", res.code());
                if (code == 401 || code == 402 || code == 403) throw new IOException("Euler sin permiso (" + code + ")");
                if (res.code() == 429 || code == 429) throw new IOException("Euler 429: demasiadas conexiones");
                if (j.has("is_live") && !j.optBoolean("is_live")) throw new Offline();
                String id = j.optString("room_id", "");
                if (id.isEmpty() || id.equals("0")) throw new IOException("Euler sin room_id " + cut(j.optString("message"), 80));
                return id;
            }
        } catch (Offline e) { throw e; } catch (Exception e) { errs.add("euler: " + e.getMessage()); }

        throw new IOException("No se encontró la sala de @" + user + " (" + String.join(" | ", errs) + ")");
    }

    static final class Offline extends IOException {
        Offline() { super("El usuario no está en directo ahora"); }
    }

    // ---------------------------------------------------------------- one connected session

    /** Runs one connection until it closes; returns why it closed. */
    private String session(String roomId) throws Exception {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("client", "ttlive-node");
        q.put("user_agent", UA);
        q.put("client_enter", "true");
        q.put("platform", "web");
        if (!key.isEmpty()) q.put("apiKey", key);
        Request.Builder rb = new Request.Builder().url(eulerBase + "/webcast/rooms/" + enc(roomId) + "/connect?" + query(q))
                .header("Accept", "application/protobuf,application/json").header("User-Agent", SIGN_UA);
        if (!key.isEmpty()) rb.header("x-api-key", key);

        Fetch first;
        String room = roomId;
        try (Response res = http.newCall(rb.build()).execute()) {
            byte[] body = res.body() == null ? new byte[0] : res.body().bytes();
            if (res.code() == 429) throw new IOException("Euler 429: demasiadas conexiones, inténtalo luego " + cut(jsonMessage(body), 120));
            if (res.code() == 402) throw new IOException("Euler 402: función de pago " + cut(jsonMessage(body), 120));
            if (res.code() != 200) throw new IOException("Euler respondió " + res.code() + " " + cut(new String(body, StandardCharsets.UTF_8), 160));
            String c = res.header("x-set-tt-cookie");
            if (c == null || c.isEmpty()) throw new IOException("Euler no devolvió cookies " + cut(String.valueOf(res.header("handshake-msg")), 100));
            cookie(c);
            String rid = res.header("x-room-id");
            if (rid != null && !rid.isEmpty()) room = rid;
            first = Fetch.parse(body);   // processInitialData=false: its messages (gifts sent before we joined) are skipped
        }
        if (first.cursor.isEmpty()) throw new IOException("Euler: respuesta sin cursor");
        if (first.pushServer.isEmpty()) throw new IOException("Euler: respuesta sin servidor");

        Map<String, String> p = wsParams();
        p.put("compress", "gzip");
        p.put("room_id", room);
        p.put("internal_ext", first.internalExt);
        p.put("cursor", first.cursor);
        for (Map.Entry<String, String> e : first.routeParams.entrySet()) if (!e.getValue().isEmpty()) p.put(e.getKey(), e.getValue());
        String url = first.pushServer + (first.pushServer.contains("?") ? "&" : "?") + query(p) + "&version_code=270000";

        final String roomFinal = room;
        final CountDownLatch closed = new CountDownLatch(1);
        final CountDownLatch opened = new CountDownLatch(1);
        final String[] why = {"Conexión cerrada"};
        final ScheduledExecutorService hb = Executors.newSingleThreadScheduledExecutor();
        final long[] seq = {1};

        Request wr = new Request.Builder().url(toHttp(url)).header("User-Agent", UA).header("Cookie", anonymousCookies()).build();
        ws = http.newWebSocket(wr, new WebSocketListener() {
            @Override public void onOpen(WebSocket w, Response r) {
                opened.countDown();
                byte[] enter = new Proto.Writer().varint(1, Long.parseLong(roomFinal)).string(2, "").string(3, "").varint(4, 12)
                        .string(5, "audience").string(6, "").varint(7, 0).varint(8, uniq()).string(9, "0").toBytes();
                w.send(ByteString.of(frame(0, "im_enter_room", enter)));
                hb.scheduleAtFixedRate(() -> {
                    byte[] beat = new Proto.Writer().varint(1, Long.parseLong(roomFinal)).varint(2, seq[0]++).toBytes();
                    w.send(ByteString.of(frame(0, "hb", beat)));
                }, 10, 10, TimeUnit.SECONDS);
                log("Conectado a la sala " + roomFinal);
                status(true, "Conectado al directo de @" + user);
            }
            @Override public void onMessage(WebSocket w, ByteString bytes) {
                try { onFrame(w, bytes.toByteArray(), why); } catch (Exception e) { log("Mensaje no leído: " + e.getMessage()); }
            }
            @Override public void onClosing(WebSocket w, int code, String reason) { w.close(1000, null); }
            @Override public void onClosed(WebSocket w, int code, String reason) {
                if (why[0].equals("Conexión cerrada")) why[0] = "TikTok cerró la conexión (" + code + (reason.isEmpty() ? "" : " " + reason) + ")";
                closed.countDown();
            }
            @Override public void onFailure(WebSocket w, Throwable t, Response r) {
                String hm = r == null ? null : r.header("handshake-msg");
                why[0] = "WebSocket: " + (hm != null ? hm : r != null ? "HTTP " + r.code() : String.valueOf(t.getMessage()));
                opened.countDown();
                closed.countDown();
            }
        });
        try {
            if (!opened.await(20, TimeUnit.SECONDS)) { ws.cancel(); throw new IOException("Tiempo agotado al abrir el WebSocket"); }
            closed.await();
        } finally {
            hb.shutdownNow();
            WebSocket w = ws;
            ws = null;
            if (w != null) w.cancel();
        }
        return why[0];
    }

    private void onFrame(WebSocket w, byte[] data, String[] why) throws IOException {
        String encoding = "", type = "";
        long logId = 0;
        byte[] payload = null;
        Proto.Reader r = new Proto.Reader(data);
        while (r.next()) {
            if (r.field == 2 && r.wire == 0) logId = r.varint();
            else if (r.field == 6 && r.wire == 2) encoding = r.string();
            else if (r.field == 7 && r.wire == 2) type = r.string();
            else if (r.field == 8 && r.wire == 2) payload = r.bytes();
            else r.skip();
        }
        if (!"pb".equals(encoding) || payload == null || payload.length == 0) return;
        if (payload.length > 2 && (payload[0] & 0xff) == 31 && (payload[1] & 0xff) == 139 && payload[2] == 8) payload = gunzip(payload);
        if ("im_enter_room_resp".equals(type)) log("TikTok confirmó la entrada a la sala");
        Fetch f = Fetch.parse(payload);
        if (f.needAck && logId != 0 && !f.internalExt.isEmpty())
            w.send(ByteString.of(frame(logId, "ack", f.internalExt.getBytes(StandardCharsets.UTF_8))));
        for (Object[] m : f.messages) {
            String method = (String) m[0];
            byte[] body = (byte[]) m[1];
            try {
                if ("WebcastGiftMessage".equals(method)) onGift(body);
                else if ("WebcastChatMessage".equals(method)) onChat(body);
                else if ("WebcastControlMessage".equals(method)) {
                    Proto.Reader c = new Proto.Reader(body);
                    long action = 0;
                    while (c.next()) { if (c.field == 2 && c.wire == 0) action = c.varint(); else c.skip(); }
                    if (action == 3 || action == 4) { why[0] = "El directo ha terminado"; w.close(1000, null); }
                }
            } catch (Exception e) { log(method + " no leído: " + e.getMessage()); }
        }
    }

    // ---------------------------------------------------------------- gifts and comments

    void onGift(byte[] body) {
        long giftId = 0, groupId = 0;
        int repeat = 0, repeatEnd = 0;
        Who who = null;
        boolean hasGift = false;
        String name = "";
        int type = 0, diamonds = 0;
        Proto.Reader r = new Proto.Reader(body);
        while (r.next()) {
            if (r.field == 2 && r.wire == 0) giftId = r.varint();
            else if (r.field == 5 && r.wire == 0) repeat = (int) r.varint();
            else if (r.field == 7 && r.wire == 2) who = Who.parse(r.bytes());
            else if (r.field == 9 && r.wire == 0) repeatEnd = (int) r.varint();
            else if (r.field == 11 && r.wire == 0) groupId = r.varint();
            else if (r.field == 15 && r.wire == 2) {
                hasGift = true;
                Proto.Reader g = new Proto.Reader(r.bytes());
                while (g.next()) {
                    if (g.field == 11 && g.wire == 0) type = (int) g.varint();
                    else if (g.field == 12 && g.wire == 0) diamonds = (int) g.varint();
                    else if (g.field == 16 && g.wire == 2) name = g.string();
                    else g.skip();
                }
            } else r.skip();
        }
        if (name.isEmpty() && giftId == 0) return;
        if (who == null) who = new Who();
        if (name.isEmpty()) name = "gift " + giftId;
        int count = Math.max(1, repeat);
        boolean end = repeatEnd > 0;
        int coins = Math.max(1, diamonds);
        if (!seenGift) { seenGift = true; log("Primer regalo recibido: " + name); }

        // Streak gifts arrive as running totals (1, 2, 3 ... then a final event repeating the last total).
        // Forward only the increase each time, so buses appear while the streak is still going.
        int delta;
        if (hasGift && type != 1) delta = count;
        else {
            long now = System.currentTimeMillis();
            synchronized (streaks) {
                Iterator<Map.Entry<String, long[]>> it = streaks.entrySet().iterator();
                while (it.hasNext()) if (now - it.next().getValue()[1] > 30000) it.remove();
                String k = (who.user.isEmpty() ? who.nick : who.user) + "|" + (giftId != 0 ? String.valueOf(giftId) : name) + "|" + groupId;
                long[] prev = streaks.get(k);
                int last = prev != null && now - prev[1] < 20000 ? (int) prev[0] : 0;
                if (count > last) delta = count - last;
                else if (count < last || (!hasGift && !end)) delta = count;
                else delta = 0;
                if (end) streaks.remove(k); else streaks.put(k, new long[]{count, now});
            }
        }
        log("Regalo " + who.nick + ": " + name + " x" + count + (end ? " fin" : "") + " → +" + delta);
        if (delta <= 0) return;
        try {
            JSONObject o = who.json().put("type", "gift").put("gift", name).put("count", delta).put("coins", coins);
            out.onEvent(o.toString());
        } catch (Exception ignored) {}
    }

    void onChat(byte[] body) {
        Who who = null;
        String text = "";
        Proto.Reader r = new Proto.Reader(body);
        while (r.next()) {
            if (r.field == 2 && r.wire == 2) who = Who.parse(r.bytes());
            else if (r.field == 3 && r.wire == 2) text = r.string();
            else r.skip();
        }
        if (who == null) who = new Who();
        try { out.onEvent(who.json().put("type", "chat").put("text", text).toString()); } catch (Exception ignored) {}
    }

    static final class Who {
        String user = "", nick = "", avatar = "";

        static Who parse(byte[] b) {
            Who w = new Who();
            String id = "", display = "", thumb = "", medium = "";
            Proto.Reader r = new Proto.Reader(b);
            while (r.next()) {
                if (r.field == 1 && r.wire == 0) id = Long.toUnsignedString(r.varint());
                else if (r.field == 3 && r.wire == 2) w.nick = r.string();
                else if (r.field == 9 && r.wire == 2) thumb = firstUrl(r.bytes());
                else if (r.field == 10 && r.wire == 2) medium = firstUrl(r.bytes());
                else if (r.field == 38 && r.wire == 2) display = r.string();
                else r.skip();
            }
            w.user = !display.isEmpty() ? display : id;
            if (w.nick.isEmpty()) w.nick = display;
            String a = !thumb.isEmpty() ? thumb : medium;
            w.avatar = a.startsWith("http://") || a.startsWith("https://") ? a : "";
            return w;
        }

        private static String firstUrl(byte[] image) {
            Proto.Reader r = new Proto.Reader(image);
            while (r.next()) { if (r.field == 1 && r.wire == 2) return r.string(); r.skip(); }
            return "";
        }

        JSONObject json() throws Exception {
            return new JSONObject().put("user", user).put("nick", nick).put("avatar", avatar);
        }
    }

    // ---------------------------------------------------------------- protobuf containers

    static final class Fetch {
        final List<Object[]> messages = new ArrayList<>();
        String cursor = "", internalExt = "", pushServer = "";
        boolean needAck;
        final Map<String, String> routeParams = new LinkedHashMap<>();

        static Fetch parse(byte[] b) {
            Fetch f = new Fetch();
            Proto.Reader r = new Proto.Reader(b);
            while (r.next()) {
                if (r.field == 1 && r.wire == 2) {
                    Proto.Reader m = new Proto.Reader(r.bytes());
                    String method = "";
                    byte[] payload = new byte[0];
                    while (m.next()) {
                        if (m.field == 1 && m.wire == 2) method = m.string();
                        else if (m.field == 2 && m.wire == 2) payload = m.bytes();
                        else m.skip();
                    }
                    f.messages.add(new Object[]{method, payload});
                } else if (r.field == 2 && r.wire == 2) f.cursor = r.string();
                else if (r.field == 5 && r.wire == 2) f.internalExt = r.string();
                else if (r.field == 7 && r.wire == 2) {
                    Proto.Reader e = new Proto.Reader(r.bytes());
                    String k = "", v = "";
                    while (e.next()) {
                        if (e.field == 1 && e.wire == 2) k = e.string();
                        else if (e.field == 2 && e.wire == 2) v = e.string();
                        else e.skip();
                    }
                    if (!k.isEmpty()) f.routeParams.put(k, v);
                } else if (r.field == 9 && r.wire == 0) f.needAck = r.varint() != 0;
                else if (r.field == 10 && r.wire == 2) f.pushServer = r.string();
                else r.skip();
            }
            return f;
        }
    }

    static byte[] frame(long logId, String type, byte[] payload) {
        return new Proto.Writer().varint(2, logId).string(6, "pb").string(7, type).bytes(8, payload).toBytes();
    }

    private long uniq() {
        long v = rnd.nextLong() & Long.MAX_VALUE;
        return v == 0 ? 1 : v;
    }

    // ---------------------------------------------------------------- HTTP helpers

    private Map<String, String> webParams() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("aid", "1988");
        m.put("app_language", "en");
        m.put("app_name", "tiktok_web");
        m.put("browser_language", "en-DE");
        m.put("browser_name", "Mozilla");
        m.put("browser_online", "true");
        m.put("browser_platform", "MacIntel");
        m.put("browser_version", UA.substring(UA.indexOf('/') + 1));
        m.put("cookie_enabled", "true");
        m.put("device_platform", "web_pc");
        m.put("focus_state", "true");
        m.put("from_page", "user");
        m.put("history_len", "10");
        m.put("is_fullscreen", "false");
        m.put("is_page_visible", "true");
        m.put("screen_height", "1080");
        m.put("screen_width", "1920");
        m.put("tz_name", "Europe/Berlin");
        m.put("referer", "https://www.tiktok.com/");
        m.put("root_referer", "https://www.tiktok.com/");
        m.put("channel", "tiktok_web");
        m.put("data_collection_enabled", "true");
        m.put("os", "mac");
        m.put("priority_region", "DE");
        m.put("region", "DE");
        m.put("user_is_login", "true");
        m.put("webcast_language", "en");
        m.put("device_id", deviceId);
        return m;
    }

    private Map<String, String> wsParams() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("version_code", "180800");
        m.put("aid", "1988");
        m.put("app_language", "en");
        m.put("app_name", "tiktok_web");
        m.put("browser_platform", "MacIntel");
        m.put("browser_language", "en-DE");
        m.put("browser_name", "Mozilla");
        m.put("browser_version", UA.substring(UA.indexOf('/') + 1));
        m.put("browser_online", "true");
        m.put("cookie_enabled", "true");
        m.put("tz_name", "Europe/Berlin");
        m.put("device_platform", "web");
        m.put("identity", "audience");
        m.put("live_id", "12");
        m.put("webcast_language", "en");
        m.put("ws_direct", "0");
        m.put("sup_ws_ds_opt", "1");
        m.put("update_version_code", "2.0.0");
        m.put("did_rule", "3");
        m.put("screen_height", "1080");
        m.put("screen_width", "1920");
        m.put("heartbeat_duration", "0");
        m.put("resp_content_type", "protobuf");
        m.put("history_comment_count", "6");
        m.put("client_enter", "1");
        m.put("last_rtt", String.valueOf(100 + rnd.nextDouble() * 100));
        m.put("User-Agent", UA);
        return m;
    }

    private String get(String url, String accept) throws IOException {
        Request req = new Request.Builder().url(url)
                .header("User-Agent", UA).header("Accept", accept).header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://www.tiktok.com/").header("Origin", "https://www.tiktok.com")
                .header("Cookie", "tt-target-idc=useast1a" + (cookies.isEmpty() ? "" : "; " + allCookies()))
                .build();
        try (Response res = http.newCall(req).execute()) {
            for (String c : res.headers("Set-Cookie")) cookie(c);
            if (res.code() >= 400) throw new IOException("HTTP " + res.code());
            return res.body() == null ? "" : res.body().string();
        }
    }

    private void cookie(String setCookie) {
        String first = setCookie.split(";", 2)[0];
        int eq = first.indexOf('=');
        if (eq <= 0) return;
        try {
            cookies.put(java.net.URLDecoder.decode(first.substring(0, eq), "UTF-8"), java.net.URLDecoder.decode(first.substring(eq + 1), "UTF-8"));
        } catch (Exception e) {
            cookies.put(first.substring(0, eq), first.substring(eq + 1));
        }
    }

    private String allCookies() {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, String> e : cookies.entrySet()) {
            if (s.length() > 0) s.append("; ");
            s.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        return s.toString();
    }

    private String anonymousCookies() {
        StringBuilder s = new StringBuilder();
        outer:
        for (Map.Entry<String, String> e : cookies.entrySet()) {
            for (String n : SESSION_COOKIES) if (n.equals(e.getKey())) continue outer;
            if (s.length() > 0) s.append("; ");
            s.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        return s.toString();
    }

    private String keyQuery(String sep) { return key.isEmpty() ? "" : sep + "apiKey=" + enc(key); }

    private static HttpUrl toHttp(String wsUrl) {
        String u = wsUrl.startsWith("wss://") ? "https://" + wsUrl.substring(6) : wsUrl.startsWith("ws://") ? "http://" + wsUrl.substring(5) : wsUrl;
        HttpUrl h = HttpUrl.parse(u);
        if (h == null) throw new IllegalArgumentException("Dirección de WebSocket no válida");
        return h;
    }

    static String query(Map<String, String> m) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (s.length() > 0) s.append('&');
            s.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        return s.toString();
    }

    static String enc(String s) {
        try { return URLEncoder.encode(s == null ? "" : s, "UTF-8"); } catch (Exception e) { return ""; }
    }

    private static byte[] gunzip(byte[] b) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(b)); ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            return o.toByteArray();
        }
    }

    private static String jsonMessage(byte[] body) {
        try { return new JSONObject(new String(body, StandardCharsets.UTF_8)).optString("message", ""); } catch (Exception e) { return ""; }
    }

    private static String cut(String s, int n) { s = s == null ? "" : s.replace('\n', ' '); return s.length() > n ? s.substring(0, n) + "…" : s; }

    private void status(boolean ok, String text) {
        try { out.onEvent(new JSONObject().put("type", "status").put("ok", ok).put("text", text).toString()); } catch (Exception ignored) {}
    }

    private void log(String line) { out.onLog(line); }
}
