/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.modern;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;
import top.niunaijun.blackbox.entity.pm.InstallResult;

/**
 * "Modern apps" mode: app-level virtualization (NewBlackbox engine). Virtual apps run on the
 * phone's own Android version (e.g. Android 16) and real GPU, isolated from the phone's apps,
 * with optional root hiding. Thin wrapper so the rest of the app never touches Bcore directly.
 */
public final class ModernEngine {

    private static final String TAG = "ModernEngine";
    private static final String PREFS = "modern_engine";
    public static final String KEY_HIDE_ROOT = "hide_root";
    public static final String KEY_DISABLE_FLAG_SECURE = "disable_flag_secure";
    public static final String KEY_DAEMON = "daemon";

    /** All virtual apps live in virtual user 0. */
    public static final int USER = 0;

    private static Context sContext;
    private static boolean sReady;

    private ModernEngine() {
    }

    public static void attach(Application app, Context base) {
        sContext = base;
        try {
            BlackBoxCore core = BlackBoxCore.get();
            try {
                core.closeCodeInit();
            } catch (Throwable ignored) {
            }
            core.onBeforeMainApplicationAttach(app, base);
            core.doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean isHideRoot() {
                    return prefs().getBoolean(KEY_HIDE_ROOT, true);
                }

                @Override
                public boolean isEnableDaemonService() {
                    return prefs().getBoolean(KEY_DAEMON, true);
                }

                @Override
                public boolean isDisableFlagSecure() {
                    return prefs().getBoolean(KEY_DISABLE_FLAG_SECURE, false);
                }
            });
            core.onAfterMainApplicationAttach(app, base);
            sReady = true;
        } catch (Throwable e) {
            Log.e(TAG, "engine attach failed", e);
        }
    }

    public static void create() {
        if (!sReady) return;
        try {
            BlackBoxCore.get().doCreate();
        } catch (Throwable e) {
            Log.e(TAG, "engine create failed", e);
            sReady = false;
        }
    }

    public static boolean isReady() {
        return sReady;
    }

    public static SharedPreferences prefs() {
        return sContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- app management (call off the UI thread) ----

    public static List<ApplicationInfo> installedApps() {
        try {
            List<ApplicationInfo> list = BlackBoxCore.get().getInstalledApplications(0, USER);
            return list != null ? list : new ArrayList<>();
        } catch (Throwable e) {
            Log.e(TAG, "list failed", e);
            return new ArrayList<>();
        }
    }

    /** Copies an app that is installed on the phone into the virtual space. */
    public static String installFromPhone(String packageName) {
        try {
            InstallResult r = BlackBoxCore.get().installPackageAsUser(packageName, USER);
            return r != null && r.success ? null : (r == null ? "unknown error" : String.valueOf(r.msg));
        } catch (Throwable e) {
            return e.toString();
        }
    }

    public static String installFromFile(File apk) {
        try {
            InstallResult r = BlackBoxCore.get().installPackageAsUser(apk, USER);
            return r != null && r.success ? null : (r == null ? "unknown error" : String.valueOf(r.msg));
        } catch (Throwable e) {
            return e.toString();
        }
    }

    public static boolean launch(String packageName) {
        try {
            return BlackBoxCore.get().launchApk(packageName, USER);
        } catch (Throwable e) {
            Log.e(TAG, "launch failed", e);
            return false;
        }
    }

    public static void uninstall(String packageName) {
        try {
            BlackBoxCore.get().uninstallPackageAsUser(packageName, USER);
        } catch (Throwable e) {
            Log.e(TAG, "uninstall failed", e);
        }
    }

    public static void clearData(String packageName) {
        try {
            BlackBoxCore.get().clearPackage(packageName, USER);
        } catch (Throwable e) {
            Log.e(TAG, "clear failed", e);
        }
    }

    public static void stop(String packageName) {
        try {
            BlackBoxCore.get().stopPackage(packageName, USER);
        } catch (Throwable e) {
            Log.e(TAG, "stop failed", e);
        }
    }

    /** Label and icon come from the phone's PackageManager reading the virtual app's APK. */
    public static CharSequence label(PackageManager pm, ApplicationInfo info) {
        try {
            if (info.sourceDir != null) {
                info.publicSourceDir = info.sourceDir;
            }
            return info.loadLabel(pm);
        } catch (Throwable e) {
            return info.packageName;
        }
    }
}
