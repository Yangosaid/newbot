/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import io.twoyi.modern.ModernAppsActivity;
import io.twoyi.ui.SettingsActivity;

/** Entry screen: choose between the two engines of "الهاتف الافتراضي". */
public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(R.string.home_subtitle);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, pad / 2, 0, pad);
        root.addView(subtitle);

        root.addView(card(getString(R.string.mode_modern_title),
                getString(R.string.mode_modern_desc, Build.VERSION.RELEASE), 0xFF1E88E5,
                () -> startActivity(new Intent(this, ModernAppsActivity.class))), cardParams(pad));

        root.addView(card(getString(R.string.mode_root_title),
                getString(R.string.mode_root_desc), 0xFF43A047,
                () -> startActivity(new Intent(this, Render2Activity.class))), cardParams(pad));

        root.addView(card(getString(R.string.title_settings),
                getString(R.string.home_settings_desc), 0xFF757575,
                () -> startActivity(new Intent(this, SettingsActivity.class))), cardParams(pad));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private LinearLayout.LayoutParams cardParams(int pad) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = pad;
        return lp;
    }

    private LinearLayout card(String title, String desc, int color, Runnable onClick) {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (18 * d);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(16 * d);
        c.setBackground(bg);
        c.setClickable(true);
        c.setFocusable(true);
        c.setOnClickListener(v -> onClick.run());

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(20);
        t.setTextColor(Color.WHITE);
        c.addView(t);

        TextView s = new TextView(this);
        s.setText(desc);
        s.setTextSize(14);
        s.setTextColor(0xEEFFFFFF);
        s.setPadding(0, (int) (6 * d), 0, 0);
        c.addView(s);
        return c;
    }
}
