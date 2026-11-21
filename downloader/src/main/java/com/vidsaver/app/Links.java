package com.vidsaver.app;

import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds a TikTok or YouTube link in any shared text and turns it into a clean address for yt-dlp. */
public final class Links {
    private Links() {}

    public static final class Link {
        public final String url, platform, kind, id;
        Link(String url, String platform, String kind, String id) { this.url = url; this.platform = platform; this.kind = kind; this.id = id; }
        /** Arabic description shown under the input box. */
        public String label() {
            String p = platform.equals("youtube") ? "يوتيوب" : platform.equals("tiktok") ? "تيك توك" : "موقع آخر";
            String k;
            switch (kind) {
                case "short": k = "شورتس"; break;
                case "video": k = "فيديو"; break;
                case "live": k = "بث مباشر"; break;
                case "music": k = "يوتيوب ميوزك"; break;
                case "playlist": k = "قائمة تشغيل (ستُحمَّل كل الفيديوهات)"; break;
                case "photo": k = "منشور صور"; break;
                case "short-link": k = "رابط مختصر"; break;
                default: k = "رابط";
            }
            return p + " · " + k + (id.isEmpty() ? "" : " · " + id);
        }
        public boolean playlist() { return kind.equals("playlist"); }
    }

    private static final Pattern URL = Pattern.compile(
            "(?i)\\b((?:https?://)?(?:[a-z0-9-]+\\.)*(?:youtube\\.com|youtu\\.be|youtube-nocookie\\.com|tiktok\\.com|tiktokv\\.com)(?:[/?#][^\\s<>\"'«»]*)?)"
            + "|\\b(https?://[^\\s<>\"'«»]+)");
    private static final Pattern YT_ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    /** Returns the first usable link in the text, or null. */
    public static Link find(String text) {
        if (text == null) return null;
        Matcher m = URL.matcher(text);
        while (m.find()) {
            String raw = m.group(1) != null ? m.group(1) : m.group(2);
            raw = raw.replaceAll("[).,;:!?،。」』>\\]]+$", "");
            if (!raw.toLowerCase(Locale.ROOT).startsWith("http")) raw = "https://" + raw;
            Link l = classify(raw);
            if (l != null) return l;
        }
        return null;
    }

    static Link classify(String url) {
        String rest = url.replaceFirst("(?i)^https?://", "");
        int slash = indexOfAny(rest, "/?#");
        String host = (slash < 0 ? rest : rest.substring(0, slash)).toLowerCase(Locale.ROOT);
        int colon = host.indexOf(':');
        if (colon >= 0) host = host.substring(0, colon);
        String path = slash < 0 ? "" : rest.substring(slash);
        if (host.isEmpty() || !host.contains(".")) return null;

        if (host.equals("youtu.be") || host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com")) return youtube(url, host, path);
        if (host.endsWith("tiktok.com") || host.endsWith("tiktokv.com")) return tiktok(url, host, path);
        return new Link(url, "other", "link", "");
    }

    private static Link youtube(String url, String host, String path) {
        String p = path, query = "";
        int q = p.indexOf('?');
        if (q >= 0) { query = p.substring(q + 1); p = p.substring(0, q); }
        int h = p.indexOf('#');
        if (h >= 0) p = p.substring(0, h);
        String[] seg = p.replaceAll("^/+", "").split("/");
        String first = seg.length > 0 ? seg[0] : "", second = seg.length > 1 ? seg[1] : "";

        // attribution_link?u=/watch%3Fv%3D...  → read the inner address
        if (first.equals("attribution_link")) {
            String u = param(query, "u");
            if (u != null) return youtube(url, host, u.startsWith("/") ? u : "/" + u);
        }
        String id = null, kind = "video";
        if (host.equals("youtu.be")) id = first;
        else if (first.equals("shorts")) { id = second; kind = "short"; }
        else if (first.equals("live")) { id = second; kind = "live"; }
        else if (first.equals("embed") || first.equals("v") || first.equals("e")) id = second;
        else if (first.equals("watch") || first.isEmpty()) id = param(query, "v");
        if (id != null && YT_ID.matcher(id).matches()) {
            boolean music = host.startsWith("music.");
            String clean = kind.equals("short") ? "https://www.youtube.com/shorts/" + id : "https://www.youtube.com/watch?v=" + id;
            return new Link(clean, "youtube", music ? "music" : kind, id);
        }
        String list = param(query, "list");
        if (list != null && !list.isEmpty()) return new Link("https://www.youtube.com/playlist?list=" + list, "youtube", "playlist", list);
        return new Link(url, "youtube", "link", "");   // channel, search… yt-dlp decides
    }

    private static Link tiktok(String url, String host, String path) {
        String p = path;
        int q = indexOfAny(p, "?#");
        if (q >= 0) p = p.substring(0, q);
        Matcher m = Pattern.compile("^/@([^/]+)/(video|photo)/(\\d+)").matcher(p);
        if (m.find()) {
            boolean photo = m.group(2).equals("photo");
            return new Link("https://www.tiktok.com/@" + m.group(1) + "/" + m.group(2) + "/" + m.group(3), "tiktok", photo ? "photo" : "video", m.group(3));
        }
        m = Pattern.compile("^/(?:v|embed(?:/v2)?|share/video)/(\\d+)").matcher(p);
        if (m.find()) return new Link("https://www.tiktok.com/@_/video/" + m.group(1), "tiktok", "video", m.group(1));
        // vm.tiktok.com/XXXX, vt.tiktok.com/XXXX, tiktok.com/t/XXXX: short links, yt-dlp follows the redirect
        if (host.startsWith("vm.") || host.startsWith("vt.") || p.startsWith("/t/"))
            return new Link(url.replaceFirst("[?#].*$", ""), "tiktok", "short-link", "");
        return new Link(url, "tiktok", "link", "");
    }

    private static String param(String query, String name) {
        for (String kv : query.split("&")) {
            int e = kv.indexOf('=');
            String k = e < 0 ? kv : kv.substring(0, e);
            if (k.equals(name)) {
                try { return URLDecoder.decode(e < 0 ? "" : kv.substring(e + 1), "UTF-8"); } catch (Exception ex) { return kv.substring(e + 1); }
            }
        }
        return null;
    }

    private static int indexOfAny(String s, String chars) {
        for (int i = 0; i < s.length(); i++) if (chars.indexOf(s.charAt(i)) >= 0) return i;
        return -1;
    }
}
