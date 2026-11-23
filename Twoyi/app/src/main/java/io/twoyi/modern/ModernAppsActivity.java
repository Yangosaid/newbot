/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.modern;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.twoyi.R;

/**
 * "Modern apps" mode: apps run on the phone's own Android version, isolated in a virtual space.
 */
public class ModernAppsActivity extends Activity {

    private static final int REQUEST_APK = 2001;

    private final List<App> mApps = new ArrayList<>();
    private AppsAdapter mAdapter;
    private TextView mEmpty;
    private int mPad;

    private static class App {
        String pkg;
        CharSequence label;
        Drawable icon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.mode_modern_title);
        mPad = (int) (12 * getResources().getDisplayMetrics().density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(mPad, mPad, mPad, mPad);

        TextView info = new TextView(this);
        info.setText(getString(R.string.modern_info, android.os.Build.VERSION.RELEASE));
        info.setTextSize(13);
        info.setPadding(0, 0, 0, mPad);
        root.addView(info);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button fromPhone = new Button(this);
        fromPhone.setText(R.string.modern_add_from_phone);
        fromPhone.setAllCaps(false);
        fromPhone.setOnClickListener(v -> pickPhoneApp());
        Button fromApk = new Button(this);
        fromApk.setText(R.string.modern_add_from_apk);
        fromApk.setAllCaps(false);
        fromApk.setOnClickListener(v -> pickApk());
        buttons.addView(fromPhone, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        buttons.addView(fromApk, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(buttons);

        root.addView(toggle(R.string.modern_hide_root, ModernEngine.KEY_HIDE_ROOT, true));
        root.addView(toggle(R.string.modern_allow_screenshots, ModernEngine.KEY_DISABLE_FLAG_SECURE, false));

        mEmpty = new TextView(this);
        mEmpty.setText(R.string.modern_empty);
        mEmpty.setGravity(Gravity.CENTER);
        mEmpty.setPadding(0, mPad * 3, 0, 0);
        root.addView(mEmpty);

        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(mPad);
        mAdapter = new AppsAdapter();
        grid.setAdapter(mAdapter);
        grid.setOnItemClickListener((p, v, pos, id) -> launch(mApps.get(pos)));
        grid.setOnItemLongClickListener((p, v, pos, id) -> {
            showAppMenu(mApps.get(pos));
            return true;
        });
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);

        if (!ModernEngine.isReady()) {
            mEmpty.setText(R.string.modern_engine_failed);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private CheckBox toggle(int text, String key, boolean def) {
        CheckBox cb = new CheckBox(this);
        cb.setText(text);
        cb.setChecked(ModernEngine.prefs().getBoolean(key, def));
        cb.setOnCheckedChangeListener((b, checked) -> {
            ModernEngine.prefs().edit().putBoolean(key, checked).apply();
            Toast.makeText(this, R.string.modern_applies_next_launch, Toast.LENGTH_SHORT).show();
        });
        return cb;
    }

    // ---- list ----

    private void reload() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<App> apps = new ArrayList<>();
            for (ApplicationInfo ai : ModernEngine.installedApps()) {
                App a = new App();
                a.pkg = ai.packageName;
                a.label = ModernEngine.label(pm, ai);
                try {
                    a.icon = ai.loadIcon(pm);
                } catch (Throwable ignored) {
                }
                apps.add(a);
            }
            Collections.sort(apps, (x, y) -> String.valueOf(x.label).compareToIgnoreCase(String.valueOf(y.label)));
            runOnUiThread(() -> {
                mApps.clear();
                mApps.addAll(apps);
                mAdapter.notifyDataSetChanged();
                if (ModernEngine.isReady()) {
                    mEmpty.setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
                }
            });
        }, "modern-list").start();
    }

    private class AppsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mApps.size();
        }

        @Override
        public Object getItem(int position) {
            return mApps.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell = convertView instanceof LinearLayout ? (LinearLayout) convertView : null;
            if (cell == null) {
                cell = new LinearLayout(ModernAppsActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                int size = (int) (52 * getResources().getDisplayMetrics().density);
                ImageView iv = new ImageView(ModernAppsActivity.this);
                cell.addView(iv, new LinearLayout.LayoutParams(size, size));
                TextView tv = new TextView(ModernAppsActivity.this);
                tv.setGravity(Gravity.CENTER);
                tv.setMaxLines(2);
                tv.setTextSize(12);
                cell.addView(tv);
            }
            App a = mApps.get(position);
            ((ImageView) cell.getChildAt(0)).setImageDrawable(a.icon);
            ((TextView) cell.getChildAt(1)).setText(a.label);
            return cell;
        }
    }

    // ---- actions ----

    private void launch(App a) {
        Toast.makeText(this, getString(R.string.modern_launching, a.label), Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            boolean ok = ModernEngine.launch(a.pkg);
            if (!ok) {
                runOnUiThread(() -> Toast.makeText(this, R.string.modern_launch_failed, Toast.LENGTH_LONG).show());
            }
        }, "modern-launch").start();
    }

    private void showAppMenu(App a) {
        String[] items = {getString(R.string.modern_open), getString(R.string.modern_stop),
                getString(R.string.modern_clear), getString(R.string.modern_remove)};
        new AlertDialog.Builder(this)
                .setTitle(a.label)
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            launch(a);
                            break;
                        case 1:
                            runBusy(() -> ModernEngine.stop(a.pkg));
                            break;
                        case 2:
                            confirm(getString(R.string.modern_clear_confirm, a.label), () -> runBusy(() -> ModernEngine.clearData(a.pkg)));
                            break;
                        case 3:
                            confirm(getString(R.string.modern_remove_confirm, a.label), () -> runBusy(() -> ModernEngine.uninstall(a.pkg)));
                            break;
                    }
                })
                .show();
    }

    private void confirm(String msg, Runnable action) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, (d, w) -> action.run())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @SuppressWarnings("deprecation")
    private void runBusy(Runnable job) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.progress_dialog_title));
        dialog.setCancelable(false);
        dialog.show();
        new Thread(() -> {
            job.run();
            runOnUiThread(() -> {
                dialog.dismiss();
                reload();
            });
        }, "modern-job").start();
    }

    private void pickPhoneApp() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(main, 0);
        Set<String> seen = new HashSet<>();
        for (App a : mApps) seen.add(a.pkg);
        List<ResolveInfo> list = new ArrayList<>();
        for (ResolveInfo ri : found) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || seen.contains(pkg)) continue;
            seen.add(pkg);
            list.add(ri);
        }
        Collections.sort(list, (x, y) -> String.valueOf(x.loadLabel(pm)).compareToIgnoreCase(String.valueOf(y.loadLabel(pm))));
        String[] labels = new String[list.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = String.valueOf(list.get(i).loadLabel(pm));
        new AlertDialog.Builder(this)
                .setTitle(R.string.modern_add_from_phone)
                .setItems(labels, (d, which) -> install(list.get(which).activityInfo.packageName, null))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void pickApk() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
        try {
            startActivityForResult(i, REQUEST_APK);
        } catch (Throwable e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_APK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            install(null, data.getData());
        }
    }

    @SuppressWarnings("deprecation")
    private void install(String pkg, Uri apk) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.modern_installing));
        dialog.setCancelable(false);
        dialog.show();
        new Thread(() -> {
            String err;
            if (pkg != null) {
                err = ModernEngine.installFromPhone(pkg);
            } else {
                File tmp = new File(getCacheDir(), "modern_install.apk");
                try (InputStream in = getContentResolver().openInputStream(apk); OutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    err = ModernEngine.installFromFile(tmp);
                } catch (Throwable e) {
                    err = e.toString();
                } finally {
                    tmp.delete();
                }
            }
            final String error = err;
            runOnUiThread(() -> {
                dialog.dismiss();
                Toast.makeText(this, error == null ? getString(R.string.install_success)
                        : getString(R.string.install_failed, error), Toast.LENGTH_LONG).show();
                reload();
            });
        }, "modern-install").start();
    }
}
