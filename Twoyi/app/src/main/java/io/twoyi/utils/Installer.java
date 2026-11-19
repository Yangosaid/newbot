/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.twoyi.R;

public class Installer {

    public interface InstallResult {
        void onSuccess(List<File> files);

        void onFail(List<File> files, String msg);
    }

    private static final String TAG = "Installer";

    public static final int REQUEST_INSTALL_APP = 101;

    private static final String ADB_TARGET = "localhost:22122";
    private static final int ADB_SERVER_PORT = 9563;

    /**
     * Runs a shell command inside the container through the bundled adb and returns stdout+stderr.
     * Never throws; returns the error text instead. Must not be called on the UI thread.
     */
    public static String adbShell(Context context, String command, long timeoutSeconds) {
        String adb = context.getApplicationInfo().nativeLibraryDir + File.separator + "libadb.so";
        String home = context.getCacheDir().getAbsolutePath();
        try {
            // Server (idempotent) + connect.
            run(context, home, timeoutSeconds, adb, "-P", String.valueOf(ADB_SERVER_PORT), "start-server");
            run(context, home, timeoutSeconds, adb, "-P", String.valueOf(ADB_SERVER_PORT), "connect", ADB_TARGET);
            return run(context, home, timeoutSeconds, adb, "-P", String.valueOf(ADB_SERVER_PORT),
                    "-s", ADB_TARGET, "shell", command);
        } catch (Throwable e) {
            return "adb error: " + e;
        }
    }

    private static String run(Context context, String home, long timeoutSeconds, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
        pb.environment().put("HOME", home);
        pb.environment().put("TMPDIR", home);
        File out = File.createTempFile("adb", ".txt", context.getCacheDir());
        pb.redirectOutput(out);
        Process p = pb.start();
        if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
            p.destroyForcibly();
        }
        String text = new String(java.nio.file.Files.readAllBytes(out.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        out.delete();
        return text;
    }

    public static void installAsync(Context context, String path, InstallResult callback) {
        installAsync(context, Collections.singletonList(new File(path)), callback);
    }

    public static void installAsync(Context context, List<File> files, InstallResult callback) {
        new Thread(() -> install(context, files, callback)).start();
    }

    public static void install(Context context, List<File> files, InstallResult callback) {

//        Shell.enableVerboseLogging = true;

        String nativeLibraryDir = context.getApplicationInfo().nativeLibraryDir;
        String adbPath = nativeLibraryDir + File.separator + "libadb.so";

        String connectTarget = "localhost:22122";

        final int ADB_PORT = 9563;

        String adbCommand = String.format(Locale.US, "%s -P %d connect %s", adbPath, ADB_PORT, connectTarget);

        String envPath = context.getCacheDir().getAbsolutePath();
        String envCmd = String.format("export TMPDIR=%s;export HOME=%s;", envPath, envPath);

        String adbServerCommand = String.format(Locale.US, "%s -P %d nodaemon server", adbPath, ADB_PORT);
        ShellUtil.newSh().newJob().add(envCmd).add(adbServerCommand).submit();

        Shell shell = ShellUtil.newSh();

        Shell.Result result = shell.newJob().add(envCmd).add(adbCommand).to(new ArrayList<>(), new ArrayList<>()).exec();

        String errMsg = Arrays.toString(result.getErr().toArray(new String[0]));
        String outMsg = Arrays.toString(result.getOut().toArray(new String[0]));

        Log.w(TAG, "success: " + result.isSuccess() + " err: " + errMsg + " out: " + outMsg);

        boolean connected = false;
        for (String s : result.getOut()) {

            // connected to localhost:22122
            // already connected to localhost
            if (s.contains("connected to")) {
                connected = true;
            }
        }

        try {
            shell.waitAndClose(1, TimeUnit.SECONDS);
        } catch (Throwable ignored) {
        }

        if (!connected) {
            if (callback != null) {
                callback.onFail(files, "Adb connect failed!");
            }
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            sb.append(file.getAbsolutePath()).append(" ");
        }

        String fileArgs = sb.toString();

        String installCommand;
        if (files.size() == 1) {
            installCommand = String.format(Locale.US, "%s -P %d -s %s install -t -r %s", adbPath, ADB_PORT, connectTarget, fileArgs);
        } else {
            // http://aospxref.com/android-10.0.0_r47/xref/system/core/adb/client/adb_install.cpp#447
            installCommand = String.format(Locale.US, "%s -P %d -s %s install-multiple -t -r %s", adbPath, ADB_PORT, connectTarget, fileArgs);
        }

        Log.w(TAG, "installCommand: " + installCommand);

        Shell installShell = ShellUtil.newSh();

        installShell.newJob().add(envCmd).add(installCommand).to(new ArrayList<>(), new ArrayList<>()).submit(out1 -> {
            Log.w(TAG, "install result: " + out1.isSuccess());

            if (callback == null) {
                return;
            }
            if (out1.isSuccess()) {
                callback.onSuccess(files);
            } else {
                String msg = Arrays.toString(out1.getErr().toArray(new String[0]));
                Log.w(TAG, "msg: " + msg);

                callback.onFail(files, msg);
            }
        });
    }

    public static boolean checkFile(Context context, List<File> files) {
        boolean valid = true;
        for (File file : files) {
            if (!checkFile(context, file.getAbsolutePath())) {
                valid = false;
                break;
            }
        }

        return valid;
    }

    public static boolean checkFile(Context context, String path) {
        if (path == null) {
            return false;
        }

        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return false;
        }

        PackageInfo packageInfo = pm.getPackageArchiveInfo(path, 0);

        if (packageInfo == null) {
            Toast.makeText(context.getApplicationContext(), R.string.check_file_invlid_apk, Toast.LENGTH_SHORT).show();
            return false;
        }

        String packageName = packageInfo.packageName;

        if (TextUtils.equals(packageName, context.getPackageName())) {
            Toast.makeText(context.getApplicationContext(), R.string.check_file_create_self_tip, Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
