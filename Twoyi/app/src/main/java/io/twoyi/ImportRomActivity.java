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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

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
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/twoyi/twoyi/releases/latest";

    private TextView mStatus;
    private ProgressBar mProgress;
    private Button mPick;
    private Button mDownload;
    private Button mBrowser;

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
            setBusy(true, getString(R.string.import_rom_connecting));
            new Thread(this::downloadAndImport, "download-rom").start();
        });
        root.addView(mDownload);

        mBrowser = new Button(this);
        mBrowser.setText(R.string.import_rom_open_browser);
        mBrowser.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL)));
            } catch (Throwable ignored) {
            }
        });
        root.addView(mBrowser);

        mPick = new Button(this);
        mPick.setText(R.string.import_rom_pick);
        mPick.setOnClickListener(v -> pickFile());
        root.addView(mPick);

        mProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mProgress.setMax(1000);
        mProgress.setIndeterminate(true);
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

    /** Downloads the official Twoyi release APK from GitHub (the phone can reach it) and imports its ROM. */
    private void downloadAndImport() {
        File tmp = new File(getCacheDir(), "rom_import.tmp");
        try {
            HttpURLConnection api = open(LATEST_RELEASE_API);
            api.setRequestProperty("Accept", "application/vnd.github+json");
            String json;
            try (InputStream in = api.getInputStream()) {
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                copy(in, bos);
                json = bos.toString("UTF-8");
            }
            JSONArray assets = new JSONObject(json).getJSONArray("assets");
            String url = null;
            long size = -1;
            for (int i = 0; i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                if (a.getString("name").endsWith(".apk")) {
                    url = a.getString("browser_download_url");
                    size = a.optLong("size", -1);
                    break;
                }
            }
            if (url == null) {
                fail(getString(R.string.import_rom_no_asset));
                return;
            }

            HttpURLConnection conn = open(url);
            long total = conn.getContentLengthLong() > 0 ? conn.getContentLengthLong() : size;
            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new FileOutputStream(tmp)) {
                byte[] buf = new byte[256 * 1024];
                long done = 0;
                long lastUi = 0;
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    long now = android.os.SystemClock.uptimeMillis();
                    if (now - lastUi > 300) {
                        lastUi = now;
                        final long d = done, t = total;
                        runOnUiThread(() -> showProgress(d, t));
                    }
                }
            }
            importLocal(tmp);
        } catch (Throwable e) {
            Log.e(TAG, "download failed", e);
            tmp.delete();
            fail(getString(R.string.import_rom_download_failed, e.getMessage()));
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(60000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "twoyi-virtual-phone");
        return c;
    }

    private void showProgress(long done, long total) {
        if (total > 0) {
            mProgress.setIndeterminate(false);
            mProgress.setProgress((int) (done * 1000 / total));
            mStatus.setText(getString(R.string.import_rom_downloading, done >> 20, total >> 20));
        } else {
            mStatus.setText(getString(R.string.import_rom_downloading, done >> 20, 0L));
        }
    }

    private void importRom(Uri uri) {
        File tmp = new File(getCacheDir(), "rom_import.tmp");
        try {
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(tmp)) {
                copy(in, out);
            }
            importLocal(tmp);
        } catch (Throwable e) {
            Log.e(TAG, "import failed", e);
            tmp.delete();
            fail(e.toString());
        }
    }

    /** tmp is either an APK containing assets/rootfs.7z, or a rootfs.7z itself. */
    private void importLocal(File tmp) {
        File target = RomManager.getStoredRomFile(this);
        runOnUiThread(() -> {
            mProgress.setIndeterminate(true);
            mStatus.setText(R.string.import_rom_reading);
        });
        try {

            // 2. Figure out what it is: an APK (zip) holding the ROM, or the ROM itself (7z).
            byte[] magic = new byte[6];
            try (InputStream in = new FileInputStream(tmp)) {
                if (in.read(magic) != magic.length) throw new IOException("file too small");
            }
            if (magic[0] == 'P' && magic[1] == 'K') {
                boolean found = false;
                try (ZipFile zip = new ZipFile(tmp)) {
                    ZipEntry e = zip.getEntry(ROM_ENTRY_IN_APK);
                    if (e != null) {
                        try (InputStream in = zip.getInputStream(e); OutputStream out = new FileOutputStream(target)) {
                            copy(in, out);
                        }
                        found = true;
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
        mBrowser.setEnabled(!busy);
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
