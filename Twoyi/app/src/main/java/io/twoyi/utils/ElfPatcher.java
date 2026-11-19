/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal ELF64 (little-endian, AArch64) helper: finds a function symbol and overwrites its
 * first instructions with "mov w0, #0; ret", i.e. makes it return 0 immediately.
 *
 * Pure Java (no Android APIs) so it can be unit-tested on a desktop JVM.
 */
public final class ElfPatcher {

    /** mov w0, #0 ; ret */
    static final byte[] RETURN_ZERO = {
            0x00, 0x00, (byte) 0x80, 0x52,
            (byte) 0xc0, 0x03, 0x5f, (byte) 0xd6,
    };

    public enum Result { PATCHED, ALREADY_PATCHED, SYMBOL_NOT_FOUND, NOT_ELF, ERROR }

    private ElfPatcher() {
    }

    /** File offset of a defined function symbol, or -1 (-2 if not an ELF64 LE file). */
    public static long findSymbolFileOffset(File so, String symbol) throws IOException {
        byte[] all;
        try (RandomAccessFile raf = new RandomAccessFile(so, "r")) {
            all = new byte[(int) raf.length()];
            raf.readFully(all);
        }
        if (!isElf64(all)) return -2;
        long vaddr = findSymbolVaddr(all, symbol.getBytes(StandardCharsets.US_ASCII), true);
        if (vaddr < 0) return -1;
        return vaddrToFileOffset(all, vaddr);
    }

    private static boolean isElf64(byte[] all) {
        return all.length >= 64 && all[0] == 0x7f && all[1] == 'E' && all[2] == 'L' && all[3] == 'F'
                && all[4] == 2 && all[5] == 1;
    }

    /**
     * Searches .symtab/.dynsym; if not found, the xz-compressed ELF in .gnu_debugdata
     * ("MiniDebugInfo", where Android keeps local symbols such as PowerHAL::vsyncHint).
     */
    private static long findSymbolVaddr(byte[] all, byte[] want, boolean allowDebugData) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        long shoff = b.getLong(0x28);
        int shentsize = b.getShort(0x3a) & 0xffff;
        int shnum = b.getShort(0x3c) & 0xffff;
        int shstrndx = b.getShort(0x3e) & 0xffff;
        if (shoff <= 0 || shoff + (long) shnum * shentsize > all.length) return -1;

        for (int i = 0; i < shnum; i++) {
            int sh = (int) (shoff + (long) i * shentsize);
            int type = b.getInt(sh + 4);
            if (type != 2 /* SHT_SYMTAB */ && type != 11 /* SHT_DYNSYM */) continue;
            long symOff = b.getLong(sh + 0x18);
            long symSize = b.getLong(sh + 0x20);
            int link = b.getInt(sh + 0x28);
            long entSize = b.getLong(sh + 0x38);
            if (entSize == 0) entSize = 24;
            if (symOff + symSize > all.length) continue;
            int strSh = (int) (shoff + (long) link * shentsize);
            long strOff = b.getLong(strSh + 0x18);

            for (long s = symOff; s + entSize <= symOff + symSize; s += entSize) {
                int nameIdx = b.getInt((int) s);
                int shndx = b.getShort((int) s + 6) & 0xffff;
                long value = b.getLong((int) s + 8);
                if (shndx == 0 || value == 0) continue; // undefined
                if (nameEquals(all, (int) (strOff + nameIdx), want)) {
                    return value;
                }
            }
        }

        if (!allowDebugData || shstrndx >= shnum) return -1;
        int strSh = (int) (shoff + (long) shstrndx * shentsize);
        long shstrOff = b.getLong(strSh + 0x18);
        byte[] dbgName = ".gnu_debugdata".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < shnum; i++) {
            int sh = (int) (shoff + (long) i * shentsize);
            if (!nameEquals(all, (int) (shstrOff + b.getInt(sh)), dbgName)) continue;
            long off = b.getLong(sh + 0x18);
            long size = b.getLong(sh + 0x20);
            if (off + size > all.length) return -1;
            byte[] inner;
            try (java.io.InputStream in = new org.tukaani.xz.XZInputStream(
                    new java.io.ByteArrayInputStream(all, (int) off, (int) size))) {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
                inner = bos.toByteArray();
            }
            // Symbol values in MiniDebugInfo use the same virtual addresses as the main file.
            return isElf64(inner) ? findSymbolVaddr(inner, want, false) : -1;
        }
        return -1;
    }

    /** Virtual address -> file offset through the PT_LOAD segment containing it, or -1. */
    static long vaddrToFileOffset(byte[] all, long vaddr) {
        ByteBuffer b = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        long phoff = b.getLong(0x20);
        int phentsize = b.getShort(0x36) & 0xffff;
        int phnum = b.getShort(0x38) & 0xffff;
        for (int i = 0; i < phnum; i++) {
            int ph = (int) (phoff + (long) i * phentsize);
            if (b.getInt(ph) != 1 /* PT_LOAD */) continue;
            long pOffset = b.getLong(ph + 0x08);
            long pVaddr = b.getLong(ph + 0x10);
            long pFilesz = b.getLong(ph + 0x20);
            if (vaddr >= pVaddr && vaddr < pVaddr + pFilesz) {
                return vaddr - pVaddr + pOffset;
            }
        }
        return -1;
    }

    private static boolean nameEquals(byte[] all, int at, byte[] want) {
        if (at < 0 || at + want.length >= all.length) return false;
        for (int i = 0; i < want.length; i++) {
            if (all[at + i] != want[i]) return false;
        }
        return all[at + want.length] == 0;
    }

    /**
     * Fallback when the symbol table is stripped: patch a known function offset, but only if the
     * code there looks like what the crash traces showed (a function prologue at {@code funcOff}
     * and a call instruction right before {@code callRetOff}).
     */
    public static Result patchReturnZeroAtVerified(File so, long funcVaddr, long callRetVaddr) {
        try (RandomAccessFile raf = new RandomAccessFile(so, "rw")) {
            byte[] all = new byte[(int) raf.length()];
            raf.readFully(all);
            if (!isElf64(all)) return Result.NOT_ELF;
            // Addresses from crash traces are virtual addresses, not file offsets.
            long funcOff = vaddrToFileOffset(all, funcVaddr);
            long callRetOff = vaddrToFileOffset(all, callRetVaddr);
            if (funcOff < 0 || callRetOff < 4) return Result.SYMBOL_NOT_FOUND;
            if (raf.length() < callRetOff + 4) return Result.SYMBOL_NOT_FOUND;
            byte[] cur = new byte[RETURN_ZERO.length];
            raf.seek(funcOff);
            raf.readFully(cur);
            if (java.util.Arrays.equals(cur, RETURN_ZERO)) return Result.ALREADY_PATCHED;
            int first = ByteBuffer.wrap(cur).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
            byte[] w = new byte[4];
            raf.seek(callRetOff - 4);
            raf.readFully(w);
            int call = ByteBuffer.wrap(w).order(ByteOrder.LITTLE_ENDIAN).getInt(0);
            boolean isCall = (call & 0xFC000000) == 0x94000000 /* BL */ || (call & 0xFFFFFC1F) == 0xD63F0000 /* BLR */;
            boolean isPrologue = (first & 0xFFC07FFF) == 0xA9807BFD /* stp x29,x30,[sp,#-n]! */
                    || (first & 0xFF8003FF) == 0xD10003FF /* sub sp,sp,#n */
                    || first == 0xD503233F /* paciasp */
                    || (first & 0xFFC003E0) == 0xA98003E0 /* stp xN,xM,[sp,#-n]! */;
            if (!isCall || !isPrologue) return Result.SYMBOL_NOT_FOUND;
            raf.seek(funcOff);
            raf.write(RETURN_ZERO);
            return Result.PATCHED;
        } catch (Throwable e) {
            return Result.ERROR;
        }
    }

    /** Makes {@code symbol} in {@code so} return 0 immediately. Idempotent. */
    public static Result patchReturnZero(File so, String symbol) {
        try {
            long off = findSymbolFileOffset(so, symbol);
            if (off == -2) return Result.NOT_ELF;
            if (off < 0) return Result.SYMBOL_NOT_FOUND;
            try (RandomAccessFile raf = new RandomAccessFile(so, "rw")) {
                byte[] cur = new byte[RETURN_ZERO.length];
                raf.seek(off);
                raf.readFully(cur);
                if (java.util.Arrays.equals(cur, RETURN_ZERO)) return Result.ALREADY_PATCHED;
                raf.seek(off);
                raf.write(RETURN_ZERO);
            }
            return Result.PATCHED;
        } catch (Throwable e) {
            return Result.ERROR;
        }
    }
}
