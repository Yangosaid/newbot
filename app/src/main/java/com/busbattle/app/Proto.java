package com.busbattle.app;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Just enough protobuf to read and write the few TikTok LIVE messages the game needs. */
final class Proto {
    private Proto() {}

    static final class Reader {
        private final byte[] b;
        private int p;
        private final int end;
        int field, wire;

        Reader(byte[] b) { this(b, 0, b == null ? 0 : b.length); }
        Reader(byte[] b, int off, int len) { this.b = b; this.p = off; this.end = off + len; }

        /** Advances to the next field; false at the end of the message. */
        boolean next() {
            if (p >= end) return false;
            long tag = varint();
            field = (int) (tag >>> 3);
            wire = (int) (tag & 7);
            return true;
        }

        long varint() {
            long r = 0;
            for (int s = 0; s < 64; s += 7) {
                if (p >= end) throw new IllegalStateException("truncated varint");
                int x = b[p++] & 0xff;
                r |= (long) (x & 0x7f) << s;
                if ((x & 0x80) == 0) return r;
            }
            throw new IllegalStateException("bad varint");
        }

        byte[] bytes() {
            int n = (int) varint();
            if (n < 0 || p + n > end) throw new IllegalStateException("truncated bytes");
            byte[] o = new byte[n];
            System.arraycopy(b, p, o, 0, n);
            p += n;
            return o;
        }

        String string() { return new String(bytes(), StandardCharsets.UTF_8); }

        void skip() {
            switch (wire) {
                case 0: varint(); break;
                case 1: p += 8; break;
                case 2: { int n = (int) varint(); p += n; break; }
                case 5: p += 4; break;
                default: throw new IllegalStateException("unsupported wire type " + wire);
            }
            if (p > end) throw new IllegalStateException("truncated field");
        }
    }

    static final class Writer {
        private final ByteArrayOutputStream o = new ByteArrayOutputStream();

        private void raw(long v) {
            while ((v & ~0x7fL) != 0) { o.write((int) ((v & 0x7f) | 0x80)); v >>>= 7; }
            o.write((int) v);
        }

        Writer varint(int field, long v) { if (v != 0) { raw((long) field << 3); raw(v); } return this; }
        Writer bool(int field, boolean v) { return varint(field, v ? 1 : 0); }

        Writer bytes(int field, byte[] v) {
            if (v == null || v.length == 0) return this;
            raw(((long) field << 3) | 2);
            raw(v.length);
            o.write(v, 0, v.length);
            return this;
        }

        Writer string(int field, String v) { return v == null ? this : bytes(field, v.getBytes(StandardCharsets.UTF_8)); }

        byte[] toBytes() { return o.toByteArray(); }
    }
}
