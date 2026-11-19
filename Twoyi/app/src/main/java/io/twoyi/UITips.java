/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi;

import android.app.Activity;

import io.twoyi.utils.AppKV;
import io.twoyi.utils.RomManager;
import io.twoyi.utils.UIHelper;

/**
 * @author weishu
 * @date 2022/2/24.
 */

public class UITips {

    /**
     * Android 12+ kills "phantom" child processes of apps, which kills the container's
     * init/zygote/system_server. Explain how to turn that off before booting.
     */
    public static boolean checkForAndroid12(Activity activity, Runnable bootCallback) {
        boolean showTips = AppKV.getBooleanConfig(activity, AppKV.SHOW_ANDROID12_TIPS, true);

        if (!RomManager.isAndroid12() || !showTips || isPhantomProcessKillerDisabled(activity)) {
            bootCallback.run();
            return true;
        }

        UIHelper.getDialogBuilder(activity)
                .setTitle(R.string.app_guide_title)
                .setMessage(R.string.tips_for_android12)
                .setCancelable(false)
                .setPositiveButton(R.string.open_developer_options, (dialog, which) -> {
                    dialog.dismiss();
                    openDeveloperOptions(activity);
                    activity.finish();
                })
                .setNeutralButton(R.string.donate_never_show, (dialog, which) -> {
                    dialog.dismiss();
                    AppKV.setBooleanConfig(activity, AppKV.SHOW_ANDROID12_TIPS, false);
                    bootCallback.run();
                })
                .setNegativeButton(R.string.boot_anyway, (dialog, which) -> {
                    dialog.dismiss();
                    bootCallback.run();
                })
                .show();

        return false;
    }

    /**
     * True when the user turned on "Disable child process restrictions" (Android 14+ developer
     * option) or ran: adb shell settings put global settings_enable_monitor_phantom_procs false
     */
    public static boolean isPhantomProcessKillerDisabled(android.content.Context context) {
        if (!RomManager.isAndroid12()) {
            return true;
        }
        try {
            String g = android.provider.Settings.Global.getString(context.getContentResolver(),
                    "settings_enable_monitor_phantom_procs");
            if ("false".equalsIgnoreCase(g)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        String prop = getprop("persist.sys.fflag.override.settings_enable_monitor_phantom_procs");
        return "false".equalsIgnoreCase(prop);
    }

    private static String getprop(String key) {
        try {
            Process p = new ProcessBuilder("getprop", key).redirectErrorStream(true).start();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                p.waitFor();
                return line == null ? "" : line.trim();
            }
        } catch (Throwable e) {
            return "";
        }
    }

    public static void openDeveloperOptions(Activity activity) {
        try {
            activity.startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Throwable e) {
            try {
                activity.startActivity(new android.content.Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS));
            } catch (Throwable ignored) {
            }
        }
    }
}
