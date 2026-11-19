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

    /** File offset of a defined function symbol, or -1. */
    public static long findSymbolFileOffset(File so, String symbol) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(so, "r")) {
            byte[] all = new byte[(int) raf.length()];
            raf.readFully(all);
            ByteBuffer b = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);

            if (all.length < 64 || b.getInt(0) != 0x464c457f /* \x7fELF */ || all[4] != 2 /* 64-bit */ || all[5] != 1 /* LE */) {
                return -2;
            }
            long phoff = b.getLong(0x20);
            long shoff = b.getLong(0x28);
            int phentsize = b.getShort(0x36) & 0xffff;
            int phnum = b.getShort(0x38) & 0xffff;
            int shentsize = b.getShort(0x3a) & 0xffff;
            int shnum = b.getShort(0x3c) & 0xffff;

            byte[] want = symbol.getBytes(StandardCharsets.US_ASCII);
            long vaddr = -1;

            for (int i = 0; i < shnum && vaddr < 0; i++) {
                int sh = (int) (shoff + (long) i * shentsize);
                int type = b.getInt(sh + 4);
                if (type != 2 /* SHT_SYMTAB */ && type != 11 /* SHT_DYNSYM */) continue;
                long symOff = b.getLong(sh + 0x18);
                long symSize = b.getLong(sh + 0x20);
                int link = b.getInt(sh + 0x28);
                long entSize = b.getLong(sh + 0x38);
                if (entSize == 0) entSize = 24;
                int strSh = (int) (shoff + (long) link * shentsize);
                long strOff = b.getLong(strSh + 0x18);

                for (long s = symOff; s + entSize <= symOff + symSize; s += entSize) {
                    int nameIdx = b.getInt((int) s);
                    int shndx = b.getShort((int) s + 6) & 0xffff;
                    long value = b.getLong((int) s + 8);
                    if (shndx == 0 || value == 0) continue; // undefined
                    if (nameEquals(all, (int) (strOff + nameIdx), want)) {
                        vaddr = value;
                        break;
                    }
                }
            }
            if (vaddr < 0) return -1;

            // vaddr -> file offset through the PT_LOAD segment that contains it.
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
    public static Result patchReturnZeroAtVerified(File so, long funcOff, long callRetOff) {
        try (RandomAccessFile raf = new RandomAccessFile(so, "rw")) {
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
