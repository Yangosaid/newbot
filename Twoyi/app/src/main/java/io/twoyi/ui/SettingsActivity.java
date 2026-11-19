/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.DocumentsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import io.twoyi.R;
import io.twoyi.utils.AppKV;
import io.twoyi.utils.LogEvents;
import io.twoyi.utils.RomManager;
import io.twoyi.utils.UIHelper;

/**
 * @author weishu
 * @date 2022/1/2.
 */

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_GET_FILE = 1000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        SettingsFragment settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.settingsFrameLayout, settingsFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.colorPrimary));
            actionBar.setTitle(R.string.title_settings);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_settings);
        }

        private Preference findPreference(@StringRes int id) {
            String key = getString(id);
            return findPreference(key);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            Preference importApp = findPreference(R.string.settings_key_import_app);
            Preference export = findPreference(R.string.settings_key_manage_files);

            Preference shutdown = findPreference(R.string.settings_key_shutdown);
            Preference reboot = findPreference(R.string.settings_key_reboot);
            Preference replaceRom = findPreference(R.string.settings_key_replace_rom);
            Preference factoryReset = findPreference(R.string.settings_key_factory_reset);

            Preference donate = findPreference(R.string.settings_key_donate);
            Preference sendLog = findPreference(R.string.settings_key_sendlog);
            Preference about = findPreference(R.string.settings_key_about);

            Preference renderScale = findPreference(R.string.settings_key_render_scale);
            if (renderScale != null) {
                final int[] values = {50, 75, 100};
                int current = io.twoyi.utils.AppKV.getRenderScale(getActivity());
                renderScale.setSummary(getString(R.string.render_scale_summary, current));
                renderScale.setOnPreferenceClickListener(preference -> {
                    Activity activity = getActivity();
                    String[] labels = {getString(R.string.render_scale_50), getString(R.string.render_scale_75), getString(R.string.render_scale_100)};
                    int checked = 0;
                    for (int i = 0; i < values.length; i++) if (values[i] == io.twoyi.utils.AppKV.getRenderScale(activity)) checked = i;
                    UIHelper.getDialogBuilder(activity)
                            .setTitle(R.string.settings_key_render_scale)
                            .setSingleChoiceItems(labels, checked, (d, which) -> {
                                d.dismiss();
                                io.twoyi.utils.AppKV.setRenderScale(activity, values[which]);
                                // The guest reads its screen size and density at boot.
                                activity.finishAffinity();
                                RomManager.reboot(activity);
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                    return true;
                });
            }

            Preference killApp = findPreference(R.string.settings_key_kill_app);
            if (killApp != null) {
                killApp.setOnPreferenceClickListener(preference -> {
                    Activity activity = getActivity();
                    ProgressDialog dialog = UIHelper.getProgressDialog(activity);
                    dialog.setCancelable(false);
                    dialog.show();
                    new Thread(() -> {
                        // Find the app in front inside the container and force-stop it.
                        String focus = io.twoyi.utils.Installer.adbShell(activity,
                                "dumpsys window | grep -m1 mCurrentFocus", 15);
                        java.util.regex.Matcher m = java.util.regex.Pattern
                                .compile("([a-zA-Z][\\w.]+)/").matcher(focus);
                        String pkg = m.find() ? m.group(1) : null;
                        String msg;
                        if (pkg == null || pkg.startsWith("com.android.launcher") || pkg.equals("android")) {
                            msg = getString(R.string.kill_app_none);
                        } else {
                            io.twoyi.utils.Installer.adbShell(activity, "am force-stop " + pkg, 15);
                            msg = getString(R.string.kill_app_done, pkg);
                        }
                        activity.runOnUiThread(() -> {
                            UIHelper.dismiss(dialog);
                            android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_LONG).show();
                        });
                    }, "kill-app").start();
                    return true;
                });
            }

            Preference childProcess = findPreference(R.string.settings_key_child_process);
            if (childProcess != null) {
                boolean ok = io.twoyi.UITips.isPhantomProcessKillerDisabled(getActivity());
                childProcess.setSummary(ok ? R.string.child_process_ok : R.string.child_process_not_ok);
                childProcess.setOnPreferenceClickListener(preference -> {
                    io.twoyi.UITips.openDeveloperOptions(getActivity());
                    return true;
                });
            }

            importApp.setOnPreferenceClickListener(preference -> {
                UIHelper.startActivity(getContext(), SelectAppActivity.class);
                return true;
            });

            export.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType(DocumentsContract.Document.MIME_TYPE_DIR);
                startActivity(intent);
                return true;
            });

            shutdown.setOnPreferenceClickListener(preference -> {
                Activity activity = getActivity();
                activity.finishAffinity();
                RomManager.shutdown(activity);
                return true;
            });

            reboot.setOnPreferenceClickListener(preference -> {
                Activity activity = getActivity();
                activity.finishAndRemoveTask();
                RomManager.reboot(activity);
                return true;
            });

            replaceRom.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), io.twoyi.ImportRomActivity.class));
                return true;
            });

            factoryReset.setOnPreferenceClickListener(preference -> {
                UIHelper.getDialogBuilder(getActivity())
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.factory_reset_confirm_message)
                        .setPositiveButton(R.string.i_confirm_it, (dialog, which) -> {
                            AppKV.setBooleanConfig(getActivity(), AppKV.SHOULD_USE_THIRD_PARTY_ROM, false);
                            AppKV.setBooleanConfig(getActivity(), AppKV.FORCE_ROM_BE_RE_INSTALL, true);
                            dialog.dismiss();

                            RomManager.reboot(getActivity());
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .show();
                return true;
            });

            donate.setOnPreferenceClickListener(preference -> {
                Context context = getContext();
                if (context instanceof Activity) {
                    UIHelper.showDonateDialog((Activity) context);
                    return true;
                }
                return false;
            });

            sendLog.setOnPreferenceClickListener(preference -> {
                Activity activity = getActivity();
                ProgressDialog dialog = UIHelper.getProgressDialog(activity);
                dialog.setMessage(getString(R.string.collecting_log));
                dialog.setCancelable(false);
                dialog.show();
                new Thread(() -> {
                    File tmpLog = new File(activity.getCacheDir(), "bugreport.zip");
                    try {
                        Files.write(tmpLog.toPath(), LogEvents.getBugreport(activity));
                    } catch (Throwable e) {
                        android.util.Log.e("Settings", "bugreport", e);
                    }
                    activity.runOnUiThread(() -> {
                        UIHelper.dismiss(dialog);
                        Uri uri = FileProvider.getUriForFile(activity, "io.twoyi.fileprovider", tmpLog);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.setDataAndType(uri, "application/zip");
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        activity.startActivity(Intent.createChooser(shareIntent, getString(R.string.settings_key_sendlog)));
                    });
                }, "bugreport").start();
                return true;
            });

            about.setOnPreferenceClickListener(preference -> {
                UIHelper.startActivity(getContext(), AboutActivity.class);
                return true;
            });
        }


        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (!(requestCode == REQUEST_GET_FILE && resultCode == Activity.RESULT_OK)) {
                return;
            }

            if (data == null) {
                return;
            }

            Uri uri = data.getData();
            if (uri == null) {
                return;
            }

            Activity activity = getActivity();
            ProgressDialog dialog = UIHelper.getProgressDialog(activity);
            dialog.setCancelable(false);
            dialog.show();

            // start copy 3rd rom
            UIHelper.defer().when(() -> {

                File rootfs3rd = RomManager.get3rdRootfsFile(activity);

                ContentResolver contentResolver = activity.getContentResolver();
                try (InputStream inputStream = contentResolver.openInputStream(uri); OutputStream os = new FileOutputStream(rootfs3rd)) {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = inputStream.read(buffer)) > 0) {
                        os.write(buffer, 0, count);
                    }
                }

                RomManager.RomInfo romInfo = RomManager.getRomInfo(rootfs3rd);
                return Pair.create(rootfs3rd, romInfo);
            }).done(result -> {

                File rootfs3rd = result.first;
                RomManager.RomInfo romInfo = result.second;
                UIHelper.dismiss(dialog);

                // copy finished, show dialog confirm
                if (romInfo.isValid()) {

                    String author = romInfo.author;
                    if ("weishu".equalsIgnoreCase(author) || "twoyi".equalsIgnoreCase(author)) {
                        Toast.makeText(activity, R.string.replace_rom_unofficial_tips, Toast.LENGTH_SHORT).show();
                        rootfs3rd.delete();
                        return;

                    }
                    UIHelper.getDialogBuilder(activity)
                            .setTitle(R.string.replace_rom_confirm_title)
                            .setMessage(getString(R.string.replace_rom_confirm_message, author, romInfo.version, romInfo.desc))
                            .setPositiveButton(R.string.i_confirm_it, (dialog1, which) -> {
                                AppKV.setBooleanConfig(activity, AppKV.SHOULD_USE_THIRD_PARTY_ROM, true);
                                AppKV.setBooleanConfig(activity, AppKV.FORCE_ROM_BE_RE_INSTALL, true);

                                dialog1.dismiss();

                                RomManager.reboot(getActivity());
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog12, which) -> dialog12.dismiss())
                            .show();
                } else {
                    Toast.makeText(activity, R.string.replace_rom_invalid, Toast.LENGTH_SHORT).show();
                    rootfs3rd.delete();
                }
            }).fail(result -> activity.runOnUiThread(() -> {
                Toast.makeText(activity, getResources().getString(R.string.install_failed_reason, result.getMessage()), Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                activity.finish();
            }));

        }
    }
}
