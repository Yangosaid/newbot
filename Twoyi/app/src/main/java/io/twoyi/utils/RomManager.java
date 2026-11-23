/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import com.hzy.libp7zip.P7ZipApi;
import com.topjohnwu.superuser.Shell;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

/**
 * @author weishu
 * @date 2021/10/22.
 */

public final class RomManager {

    private static final String TAG = "RomManager";

    private static final String ROOTFS_NAME = "rootfs.7z";

    private static final String ROM_INFO_FILE = "rom.ini";

    private static final String DEFAULT_INFO = "unknown";

    private static final String LOADER_FILE = "libloader.so";

    private static final String CUSTOM_ROM_FILE_NAME = "rootfs_3rd.7z";

    private RomManager() {
    }

    public static void initRootfs(Context context) {
        File propFile = getVendorPropFile(context);
        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();

        Properties properties = new Properties();

        properties.setProperty("persist.sys.language", language);
        properties.setProperty("persist.sys.country", country);

        TimeZone timeZone = TimeZone.getDefault();
        String timeZoneID = timeZone.getID();
        Log.i(TAG, "timezone: " + timeZoneID);
        properties.setProperty("persist.sys.timezone", timeZoneID);

        // Density follows the internal render scale so the UI keeps its physical size.
        int scale = AppKV.getRenderScale(context);
        properties.setProperty("ro.sf.lcd_density", String.valueOf(Math.max(120, DisplayMetrics.DENSITY_DEVICE_STABLE * scale / 100)));

        // Avoid SurfaceFlinger frame stalls inside the container.
        properties.setProperty("debug.sf.disable_backpressure", "1");

        // The renderer implements OpenGL ES 2.0. Without this, Android reports ES 1.0 and apps
        // refuse to start ("this phone cannot show 3D graphics").
        properties.setProperty("ro.opengles.version", "131072");
        properties.setProperty("debug.sf.latch_unsignaled", "1");

        try (Writer writer = new FileWriter(propFile)) {
            properties.store(writer, null);
        } catch (IOException ignored) {
        }
    }

    public static void ensureBootFiles(Context context) {

        // <rootdir>/dev/
        File devDir = new File(getRootfsDir(context), "dev");
        ensureDir(new File(devDir, "input"));
        ensureDir(new File(devDir, "socket"));
        ensureDir(new File(devDir, "maps"));

        ensureDir(new File(context.getDataDir(), "socket"));

        createLoaderSymlink(context);

        killOrphanProcess();

        saveLastKmsg(context);
    }

    private static void createLoaderSymlink(Context context) {
        Path loaderSymlink = new File(context.getDataDir(), "loader64").toPath();
        String loaderPath = getLoaderPath(context);
        try {
            Files.deleteIfExists(loaderSymlink);
            Files.createSymbolicLink(loaderSymlink, Paths.get(loaderPath));
        } catch (IOException e) {
            throw new RuntimeException("symlink loader failed.", e);
        }
    }

    private static void killOrphanProcess() {
        Shell shell = ShellUtil.newSh();
        shell.newJob().add("ps -ef | awk '{if($3==1) print $2}' | xargs kill -9").exec();
    }

    private static void saveLastKmsg(Context context) {
        File lastKmsgFile = LogEvents.getLastKmsgFile(context);
        File kmsgFile = LogEvents.getKmsgFile(context);
        try {
            Files.move(kmsgFile.toPath(), lastKmsgFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    public static class RomInfo {
        public String author = DEFAULT_INFO;
        public String version = DEFAULT_INFO;
        public String desc = DEFAULT_INFO;
        public String md5 = "";
        public long code = 0;

        @Override
        public String toString() {
            return "RomInfo{" +
                    "author='" + author + '\'' +
                    ", version='" + version + '\'' +
                    ", md5='" + md5 + '\'' +
                    ", code=" + code +
                    '}';
        }

        public boolean isValid() {
            return this != DEFAULT_ROM_INFO;
        }
    }

    public static final RomInfo DEFAULT_ROM_INFO = new RomInfo();

    public static boolean romExist(Context context) {
        File initFile = new File(getRootfsDir(context), "init");
        return initFile.exists();
    }

    public static boolean needsUpgrade(Context context) {
        RomInfo currentRomInfo = getCurrentRomInfo(context);
        Log.i(TAG, "current rom: " + currentRomInfo);
        if (currentRomInfo.equals(DEFAULT_ROM_INFO)) {
            return true;
        }

        RomInfo romInfoFromAssets = getRomInfoFromAssets(context);
        Log.i(TAG, "asset rom: " + romInfoFromAssets);
        return romInfoFromAssets.code > currentRomInfo.code;
    }

    public static RomInfo getCurrentRomInfo(Context context) {
        File infoFile = new File(getRootfsDir(context), ROM_INFO_FILE);
        try (FileInputStream inputStream = new FileInputStream(infoFile)) {
            return getRomInfo(inputStream);
        } catch (Throwable e) {
            return DEFAULT_ROM_INFO;
        }
    }

    public static String getLoaderPath(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        return new File(applicationInfo.nativeLibraryDir, LOADER_FILE).getAbsolutePath();
    }

    public static RomInfo getRomInfo(File rom) {
        try (SevenZFile zFile = new SevenZFile(rom)) {

            SevenZArchiveEntry entry;

            while ((entry = zFile.getNextEntry()) != null) {
                if (entry.getName().equals("rootfs/rom.ini")) {
                    byte[] content = new byte[(int) entry.getSize()];
                    zFile.read(content, 0, content.length);
                    ByteArrayInputStream bais = new ByteArrayInputStream(content);
                    return getRomInfo(bais);
                }
            }
        } catch (Throwable e) {
            LogEvents.trackError(e);
        }
        return DEFAULT_ROM_INFO;
    }

    public static RomInfo getRomInfoFromAssets(Context context) {
        AssetManager assets = context.getAssets();
        try (InputStream open = assets.open(ROM_INFO_FILE)) {
            return getRomInfo(open);
        } catch (Throwable ignored) {
        }
        return DEFAULT_ROM_INFO;
    }

    public static void extractRootfs(Context context, boolean romExist, boolean needsUpgrade, boolean forceInstall, boolean use3rdRom) {

        // force remove system dir to avoiding wired issues
        removeSystemPartition(context);
        removeVendorPartition(context);

        if (!romExist) {
            // first init
            extractRootfsInAssets(context);
            return;
        }

        if (forceInstall) {
            if (use3rdRom) {
                // install 3rd rom
                boolean success = extract3rdRootfs(context);
                if (!success) {
                    showRootfsInstallationFailure(context);
                    return;
                }
            } else {
                // factory reset!!
                if (!extractRootfsInAssets(context)) {
                    showRootfsInstallationFailure(context);
                    return;
                }
            }

            // force install finish, reset the state.
            AppKV.setBooleanConfig(context, AppKV.FORCE_ROM_BE_RE_INSTALL, false);
        } else {
            if (use3rdRom) {
                Log.w(TAG, "WTF? 3rd ROM must be force install!");
            }
            if (needsUpgrade) {
                Log.i(TAG, "upgrade factory rom..");
                if (!extractRootfsInAssets(context)) {
                    showRootfsInstallationFailure(context);
                }
            }
        }
    }

    private static void showRootfsInstallationFailure(Context context) {
        // TODO
    }

    public static void reboot(Context context) {
        // The launcher entry is now the mode chooser; restart straight into the root system.
        Intent intent = new Intent(context, io.twoyi.Render2Activity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.getApplicationContext().startActivity(intent);

        shutdown(context);
    }

    public static void shutdown(Context context) {
        try {
            // Otherwise START_STICKY would bring the notification back without a container.
            context.stopService(new Intent(context, io.twoyi.KeepAliveService.class));
        } catch (Throwable ignored) {
        }
        System.exit(0);
        Process.killProcess(Process.myPid());
    }

    public static boolean extract3rdRootfs(Context context) {
        File rootfs3rd = get3rdRootfsFile(context);
        if (!rootfs3rd.exists()) {
            return false;
        }
        int err = extractRootfs(context, rootfs3rd);
        return err == 0;
    }

    public static int extractRootfs(Context context, File rootfs7z) {

        int cpu = Runtime.getRuntime().availableProcessors();
        int ret = P7ZipApi.executeCommand(String.format(Locale.US, "7z x -mmt=%d -aoa '%s' '-o%s'",
                cpu, rootfs7z, context.getDataDir()));
        if (ret == 0) {
            applyCommunityFixesIfNeeded(context);
            applySystemPatches(context);
        }
        return ret;
    }

    public static boolean extractRootfsInAssets(Context context) {

        // read assets
        long t1 = SystemClock.elapsedRealtime();
        File rootfs7z = context.getFileStreamPath(ROOTFS_NAME);
        try (InputStream inputStream = new BufferedInputStream(context.getAssets().open(ROOTFS_NAME));
             OutputStream os = new BufferedOutputStream(new FileOutputStream(rootfs7z))) {
            byte[] buffer = new byte[10240];
            int count;
            while ((count = inputStream.read(buffer)) > 0) {
                os.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long t2 = SystemClock.elapsedRealtime();

        int ret = extractRootfs(context, rootfs7z);

        long t3 = SystemClock.elapsedRealtime();

        Log.i(TAG, "extract rootfs, read assets: " + (t2 - t1) + " un7z: " + (t3 - t2) + "ret: " + ret);

        return ret == 0;
    }

    public static File getRootfsDir(Context context) {
        return new File(context.getDataDir(), "rootfs");
    }

    public static File getRomSdcardDir(Context context) {
        return new File(getRootfsDir(context), "sdcard");
    }

    public static File getVendorDir(Context context) {
        return new File(getRootfsDir(context), "vendor");
    }

    public static File getVendorPropFile(Context context) {
        return new File(getVendorDir(context), "default.prop");
    }

    public static File get3rdRootfsFile(Context context) {
        return context.getFileStreamPath(CUSTOM_ROM_FILE_NAME);
    }

    public static boolean isAndroid12() {
        // Android 12 and newer all have the phantom (child) process killer.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    /** The ROM file kept in app storage (imported by the user or copied from assets). */
    public static File getStoredRomFile(Context context) {
        return context.getFileStreamPath(ROOTFS_NAME);
    }

    /** Whether this APK bundles the official ROM in its assets. */
    public static boolean hasAssetRom(Context context) {
        try (InputStream ignored = context.getAssets().open(ROOTFS_NAME)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void removePartition(Context context, String partition) {
        File rootfsDir = getRootfsDir(context);
        File systemDir = new File(rootfsDir, partition);

        IOUtils.deleteDirectory(systemDir);
    }

    /**
     * Fixes for the closed-source ROM, applied before every boot (idempotent):
     *
     * SurfaceFlinger deadlock: EventThread holds its mutex while PowerHAL::vsyncHint() sends a
     * oneway binder call to system_server. Twoyi's virtual binder sometimes never completes that
     * call (e.g. while system_server restarts), so the mutex is held forever, vsync stops and the
     * whole screen freezes (seen in ANR traces: DispSync and Binder threads blocked on the
     * EventThread lock, "POSIX timer" thread stuck in BpPowerManager::powerHint). The hint only
     * boosts CPU, so make vsyncHint() return immediately.
     */
    public static void applySystemPatches(Context context) {
        StringBuilder status = new StringBuilder();
        File lib64 = new File(getRootfsDir(context), "system/lib64");
        status.append("codecs: ").append(installMissingCodecs(context)).append('\n');
        // Fallback addresses from the 0.5.4 ROM's crash traces (vsyncHint+268 returns from powerHint).
        status.append("libsurfaceflinger PowerHAL::vsyncHint: ").append(patchLib(
                new File(lib64, "libsurfaceflinger.so"), "_ZN7android8PowerHAL9vsyncHintEb", 0x57610, 0x5771c)).append('\n');
        // Second line of defence: make the client side of the power hint a no-op as well.
        // No camera HAL in the container: cameraserver never registers, and
        // Camera.getNumberOfCameras() loops forever waiting for it (froze Opera). Report 0 cameras.
        status.append("libcamera_client getNumberOfCameras: ").append(patchLib(
                new File(lib64, "libcamera_client.so"),
                "_ZN7android10CameraBaseINS_6CameraENS_12CameraTraitsIS1_EEE18getNumberOfCamerasEv", -1, -1)).append('\n');
        status.append("build.prop memory: ").append(tuneBuildProp(context)).append('\n');
        status.append("libpowermanager BpPowerManager::powerHint: ").append(patchLib(
                new File(lib64, "libpowermanager.so"), "_ZN7android14BpPowerManager9powerHintEii", -1, -1)).append('\n');
        Log.i(TAG, "system patches:\n" + status);
        try {
            Files.write(getPatchStatusFile(context).toPath(), status.toString().getBytes());
        } catch (IOException ignored) {
        }
    }

    /**
     * The ROM ships media_codecs.xml listing Google's software codecs but not the libraries
     * (libstagefright_soft_*.so), so every video/audio decoder fails with ComponentNotFound and
     * no video plays. Copy the AOSP 8.1 arm64 libraries bundled in assets/codecs/lib64, only
     * for files that are missing: nothing already in the ROM is overwritten.
     */
    private static String installMissingCodecs(Context context) {
        File lib64 = new File(getRootfsDir(context), "system/lib64");
        if (!lib64.isDirectory()) return "no system/lib64";
        int installed = 0, present = 0;
        try {
            String[] names = context.getAssets().list("codecs/lib64");
            if (names == null) return "no bundle";
            for (String name : names) {
                File target = new File(lib64, name);
                if (target.exists()) {
                    present++;
                    continue;
                }
                File tmp = new File(lib64, name + ".tmp");
                try (InputStream in = context.getAssets().open("codecs/lib64/" + name);
                     OutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                tmp.setReadable(true, false);
                if (!tmp.renameTo(target)) {
                    tmp.delete();
                    continue;
                }
                installed++;
            }
        } catch (IOException e) {
            return "ERROR " + e;
        }
        return "installed " + installed + ", already present " + present;
    }

    /** Larger Java heaps for apps inside the container (browsers/TikTok hit the 192m default). */
    private static String tuneBuildProp(Context context) {
        File buildProp = new File(getRootfsDir(context), "system/build.prop");
        if (!buildProp.exists()) return "missing";
        try {
            java.util.List<String> lines = Files.readAllLines(buildProp.toPath());
            java.util.Map<String, String> want = new java.util.LinkedHashMap<>();
            want.put("dalvik.vm.heapgrowthlimit", "384m");
            want.put("dalvik.vm.heapsize", "768m");
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                String l = lines.get(i);
                int eq = l.indexOf('=');
                if (eq <= 0 || l.startsWith("#")) continue;
                String key = l.substring(0, eq).trim();
                String v = want.remove(key);
                if (v != null && !l.substring(eq + 1).trim().equals(v)) {
                    lines.set(i, key + "=" + v);
                    changed = true;
                }
            }
            for (java.util.Map.Entry<String, String> e : want.entrySet()) {
                lines.add(e.getKey() + "=" + e.getValue());
                changed = true;
            }
            if (changed) {
                File tmp = new File(buildProp.getPath() + ".tmp");
                Files.write(tmp.toPath(), lines);
                tmp.setReadable(true, false);
                Files.move(tmp.toPath(), buildProp.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            return changed ? "updated" : "ok";
        } catch (Throwable e) {
            return "ERROR " + e;
        }
    }

    public static File getPatchStatusFile(Context context) {
        return new File(context.getDataDir(), "patch_status.txt");
    }

    /** Patches a copy and atomically renames it over the original (running processes keep the old inode). */
    private static String patchLib(File lib, String symbol, long fallbackFunc, long fallbackCallRet) {
        if (!lib.exists()) return "missing";
        File tmp = new File(lib.getPath() + ".patching");
        try {
            Files.copy(lib.toPath(), tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ElfPatcher.Result r = ElfPatcher.patchReturnZero(tmp, symbol);
            if (r == ElfPatcher.Result.SYMBOL_NOT_FOUND && fallbackFunc > 0) {
                r = ElfPatcher.patchReturnZeroAtVerified(tmp, fallbackFunc, fallbackCallRet);
            }
            if (r == ElfPatcher.Result.PATCHED) {
                tmp.setReadable(true, false);
                tmp.setExecutable(true, false);
                Files.move(tmp.toPath(), lib.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            return r.name();
        } catch (Throwable e) {
            Log.e(TAG, "patch " + lib + " failed", e);
            return "ERROR " + e;
        } finally {
            tmp.delete();
        }
    }

    /** Removes system/ and vendor/ before installing a different ROM; user data is kept. */
    public static void removeSystemAndVendor(Context context) {
        removeSystemPartition(context);
        removeVendorPartition(context);
    }

    /**
     * The community ROM (github.com/SUFandom/twoyi-customrom) ships without a usable build.prop
     * and rom.ini; its build script generates them. Apply the same result after every extraction.
     */
    public static void applyCommunityFixesIfNeeded(Context context) {
        if (!AppKV.getBooleanConfig(context, AppKV.ROM_IS_COMMUNITY, false)) {
            return;
        }
        File rootfs = getRootfsDir(context);
        File buildProp = new File(rootfs, "system/build.prop");
        try (InputStream in = context.getAssets().open("community_build.prop");
             OutputStream out = new FileOutputStream(buildProp)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (IOException e) {
            Log.e(TAG, "write community build.prop failed", e);
        }
        File romIni = new File(rootfs, ROM_INFO_FILE);
        if (!romIni.exists()) {
            try (Writer w = new FileWriter(romIni)) {
                w.write("author=SUFandom (community)\ncode=1\nversion=community-0.1\ndesc=AOSP 8.1 + microG, F-Droid, Aurora Store, NewPipe\n");
            } catch (IOException e) {
                Log.e(TAG, "write community rom.ini failed", e);
            }
        }
    }

    private static void removeSystemPartition(Context context) {
        removePartition(context, "system");
    }

    private static void removeVendorPartition(Context context) {
        removePartition(context, "vendor");
    }

    private static RomInfo getRomInfo(InputStream in) {
        Properties prop = new Properties();
        try {
            prop.load(in);

            RomInfo info = new RomInfo();
            info.author = prop.getProperty("author");
            info.code = Long.parseLong(prop.getProperty("code"));
            info.version = prop.getProperty("version");
            info.desc = prop.getProperty("desc", DEFAULT_INFO);
            info.md5 = prop.getProperty("md5");
            return info;
        } catch (Throwable e) {
            Log.e(TAG, "read rom info err", e);
            return DEFAULT_ROM_INFO;
        }
    }

    private static void ensureDir(File file) {
        if (file.exists()) {
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        file.mkdirs();
    }
}
