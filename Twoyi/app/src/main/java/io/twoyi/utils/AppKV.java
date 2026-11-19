/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author weishu
 * @date 2019/2/28.
 */
public class AppKV {


    private static final String PREF_NAME = "app_kv";

    public static final String ADD_APP_NOT_SHOW_SYSTEM= "add_app_not_show_system";
    public static final String ADD_APP_NOT_SHOW_ADDED = "add_app_not_show_added";
    public static final String SHOW_ANDROID12_TIPS = "show_android12_tips_v2";
    public static final String ADD_APP_NOT_SHOW_32BIT = "add_app_not_show_32bit";

    // 是否应该重新安装 ROM
    // 1. 恢复出厂设置
    // 2. 替换 ROM
    public static final String FORCE_ROM_BE_RE_INSTALL = "rom_should_be_re_install";

    // 是否应该使用第三方 ROM
    public static final String SHOULD_USE_THIRD_PARTY_ROM = "should_use_third_party_rom";

    /** The stored ROM is the community build that needs build.prop/rom.ini fixes after extraction. */
    public static final String ROM_IS_COMMUNITY = "rom_is_community";

    /** Internal render resolution in percent of the real screen (50, 75 or 100). */
    public static final String RENDER_SCALE = "render_scale_percent";
    public static final int DEFAULT_RENDER_SCALE = 50;

    public static int getRenderScale(Context context) {
        int v = getPref(context).getInt(RENDER_SCALE, DEFAULT_RENDER_SCALE);
        return (v == 50 || v == 75 || v == 100) ? v : DEFAULT_RENDER_SCALE;
    }

    @SuppressLint("ApplySharedPref")
    public static void setRenderScale(Context context, int percent) {
        getPref(context).edit().putInt(RENDER_SCALE, percent).commit();
    }

    public static boolean getBooleanConfig(Context context,  String key, boolean fallback) {
        return getPref(context).getBoolean(key, fallback);
    }

    @SuppressLint("ApplySharedPref")
    public static void setBooleanConfig(Context context, String key, boolean value) {
        getPref(context).edit().putBoolean(key, value).commit();
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
