/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.twoyi.utils.RomManager;

/**
 * Shown on first launch when this APK does not bundle a ROM.
 * The user picks the official Twoyi release APK (or a rootfs.7z) from the phone,
 * the ROM is pulled out of it, stored, and extracted.
 */
public class ImportRomActivity extends Activity {

    private static final String TAG = "ImportRomActivity";
    private static final int REQUEST_PICK = 1001;
    private static final String RELEASES_URL = "https://github.com/twoyi/twoyi/releases";
    private static final String ROM_ENTRY_IN_APK = "assets/rootfs.7z";

    private TextView mStatus;
    private ProgressBar mProgress;
    private Button mPick;
    private Button mDownload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad * 2, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(R.string.import_rom_title);
        title.setTextSize(22);
        title.setPadding(0, 0, 0, pad);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText(R.string.import_rom_body);
        body.setTextSize(15);
        body.setLineSpacing(0, 1.2f);
        root.addView(body);

        mDownload = new Button(this);
        mDownload.setText(R.string.import_rom_download);
        mDownload.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)));
            } catch (Throwable ignored) {
            }
        });
        root.addView(mDownload);

        mPick = new Button(this);
        mPick.setText(R.string.import_rom_pick);
        mPick.setOnClickListener(v -> pickFile());
        root.addView(mPick);

        mProgress = new ProgressBar(this);
        mProgress.setVisibility(View.GONE);
        root.addView(mProgress);

        mStatus = new TextView(this);
        mStatus.setTextSize(14);
        mStatus.setPadding(0, pad, 0, 0);
        root.addView(mStatus);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            startActivityForResult(intent, REQUEST_PICK);
        } catch (Throwable e) {
            mStatus.setText(e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        setBusy(true, getString(R.string.import_rom_reading));
        new Thread(() -> importRom(uri), "import-rom").start();
    }

    private void importRom(Uri uri) {
        File tmp = new File(getCacheDir(), "rom_import.tmp");
        File target = RomManager.getStoredRomFile(this);
        try {
            // 1. Copy the picked file locally.
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tmp)) {
                copy(in, out);
            }

            // 2. Figure out what it is: an APK (zip) holding the ROM, or the ROM itself (7z).
            byte[] magic = new byte[6];
            try (InputStream in = new FileInputStream(tmp)) {
                if (in.read(magic) != magic.length) throw new IOException("file too small");
            }
            if (magic[0] == 'P' && magic[1] == 'K') {
                boolean found = false;
                try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(tmp)))) {
                    ZipEntry e;
                    while ((e = zin.getNextEntry()) != null) {
                        if (ROM_ENTRY_IN_APK.equals(e.getName())) {
                            try (OutputStream out = new FileOutputStream(target)) {
                                copy(zin, out);
                            }
                            found = true;
                            break;
                        }
                    }
                }
                tmp.delete();
                if (!found) {
                    fail(getString(R.string.import_rom_no_rom_in_apk));
                    return;
                }
            } else if (magic[0] == '7' && magic[1] == 'z') {
                if (!tmp.renameTo(target)) throw new IOException("rename failed");
            } else {
                tmp.delete();
                fail(getString(R.string.import_rom_unknown_file));
                return;
            }

            // 3. Validate.
            RomManager.RomInfo info = RomManager.getRomInfo(target);
            Log.i(TAG, "imported rom: " + info);
            if (!info.isValid()) {
                target.delete();
                fail(getString(R.string.replace_rom_invalid));
                return;
            }

            // 4. Extract it.
            runOnUiThread(() -> mStatus.setText(getString(R.string.import_rom_extracting, info.version)));
            int ret = RomManager.extractRootfs(this, target);
            if (ret != 0 || !RomManager.romExist(this)) {
                fail(getString(R.string.import_rom_extract_failed, ret));
                return;
            }
            RomManager.initRootfs(this);

            runOnUiThread(() -> {
                startActivity(new Intent(this, Render2Activity.class));
                finish();
            });
        } catch (Throwable e) {
            Log.e(TAG, "import failed", e);
            tmp.delete();
            fail(e.toString());
        }
    }

    private void fail(String msg) {
        runOnUiThread(() -> setBusy(false, msg));
    }

    private void setBusy(boolean busy, String status) {
        mPick.setEnabled(!busy);
        mDownload.setEnabled(!busy);
        mProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        mStatus.setText(status);
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[64 * 1024];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
    }
}
