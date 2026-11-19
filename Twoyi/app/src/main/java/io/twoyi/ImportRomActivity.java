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
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.twoyi.utils.AppKV;
import io.twoyi.utils.RomManager;

/**
 * Installs (or switches) the container ROM. Shown on first launch when no ROM is present,
 * and from Settings to switch ROMs. Sources:
 *  1. Official Twoyi ROM: pulled out of the official release APK on GitHub.
 *  2. Same official ROM from a community mirror (plain rootfs.7z), used as a fallback.
 *  3. Community ROM (SUFandom/twoyi-customrom): AOSP 8.1 + microG, F-Droid, Aurora Store, NewPipe.
 *  4. A local file the user picks (Twoyi APK or rootfs.7z).
 */
public class ImportRomActivity extends Activity {

    private static final String TAG = "ImportRomActivity";
    private static final int REQUEST_PICK = 1001;

    private static final String ROM_ENTRY_IN_APK = "assets/rootfs.7z";
    private static final String LATEST_RELEASE_API = "https://api.github.com/repos/twoyi/twoyi/releases/latest";
    private static final String OFFICIAL_APK_URL =
            "https://github.com/twoyi/twoyi/releases/download/0.5.4/twoyi_0.5.4-03211927-release.apk";
    private static final String MIRROR_ROOTFS_URL =
            "https://github.com/sunandar3221/twoyi-fork/releases/download/v0.5.4/rootfs.7z";
    private static final String COMMUNITY_ROM_URL =
            "https://github.com/SUFandom/twoyi-customrom/releases/download/0.1/romfile.7z";

    private TextView mStatus;
    private ProgressBar mProgress;
    private final List<Button> mButtons = new ArrayList<>();

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

        addButton(root, R.string.rom_official, v -> start(this::installOfficial));
        addButton(root, R.string.rom_community, v -> start(this::installCommunity));
        addButton(root, R.string.import_rom_pick, v -> pickFile());

        TextView note = new TextView(this);
        note.setText(R.string.rom_choice_note);
        note.setTextSize(13);
        note.setPadding(0, pad / 2, 0, 0);
        root.addView(note);

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

    private void addButton(LinearLayout root, int text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        root.addView(b);
        mButtons.add(b);
    }

    private void start(Runnable job) {
        setBusy(true, getString(R.string.import_rom_connecting));
        new Thread(job, "rom-install").start();
    }

    // ---- sources ----

    private void installOfficial() {
        File tmp = tmpFile();
        try {
            String apkUrl = OFFICIAL_APK_URL;
            try {
                apkUrl = findLatestOfficialApk();
            } catch (Throwable e) {
                Log.w(TAG, "release API failed, using fixed URL", e);
            }
            try {
                download(apkUrl, tmp);
            } catch (Throwable e) {
                Log.w(TAG, "official download failed, trying mirror", e);
                runOnUiThread(() -> mStatus.setText(R.string.rom_trying_mirror));
                download(MIRROR_ROOTFS_URL, tmp);
            }
            install(tmp, false);
        } catch (Throwable e) {
            Log.e(TAG, "official install failed", e);
            tmp.delete();
            fail(getString(R.string.import_rom_download_failed, String.valueOf(e.getMessage())));
        }
    }

    private void installCommunity() {
        File tmp = tmpFile();
        try {
            download(COMMUNITY_ROM_URL, tmp);
            install(tmp, true);
        } catch (Throwable e) {
            Log.e(TAG, "community install failed", e);
            tmp.delete();
            fail(getString(R.string.import_rom_download_failed, String.valueOf(e.getMessage())));
        }
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
        new Thread(() -> {
            File tmp = tmpFile();
            try {
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(tmp)) {
                    copy(in, out);
                }
                install(tmp, false);
            } catch (Throwable e) {
                Log.e(TAG, "import failed", e);
                tmp.delete();
                fail(e.toString());
            }
        }, "rom-import").start();
    }

    // ---- install ----

    /**
     * @param tmp       an APK holding assets/rootfs.7z, or a rootfs .7z archive
     * @param community the community ROM: no rom.ini inside, fixed up after extraction
     */
    private void install(File tmp, boolean community) throws IOException {
        File target = RomManager.getStoredRomFile(this);
        runOnUiThread(() -> {
            mProgress.setIndeterminate(true);
            mStatus.setText(R.string.import_rom_reading);
        });

        byte[] magic = new byte[6];
        try (InputStream in = new FileInputStream(tmp)) {
            if (in.read(magic) != magic.length) throw new IOException("file too small");
        }
        File candidate = new File(getCacheDir(), "rom_candidate.7z");
        candidate.delete();
        if (magic[0] == 'P' && magic[1] == 'K') {
            try (ZipFile zip = new ZipFile(tmp)) {
                ZipEntry e = zip.getEntry(ROM_ENTRY_IN_APK);
                if (e == null) {
                    tmp.delete();
                    fail(getString(R.string.import_rom_no_rom_in_apk));
                    return;
                }
                try (InputStream in = zip.getInputStream(e); OutputStream out = new FileOutputStream(candidate)) {
                    copy(in, out);
                }
            }
            tmp.delete();
        } else if (magic[0] == '7' && magic[1] == 'z') {
            if (!tmp.renameTo(candidate)) throw new IOException("rename failed");
        } else {
            tmp.delete();
            fail(getString(R.string.import_rom_unknown_file));
            return;
        }

        String version;
        if (community) {
            version = "community-0.1";
        } else {
            RomManager.RomInfo info = RomManager.getRomInfo(candidate);
            Log.i(TAG, "rom info: " + info);
            if (!info.isValid()) {
                candidate.delete();
                fail(getString(R.string.replace_rom_invalid));
                return;
            }
            version = info.version;
        }

        // Only now replace the stored ROM and the installed system (user data is kept).
        target.delete();
        if (!candidate.renameTo(target)) throw new IOException("cannot store ROM");
        AppKV.setBooleanConfig(this, AppKV.ROM_IS_COMMUNITY, community);
        AppKV.setBooleanConfig(this, AppKV.SHOULD_USE_THIRD_PARTY_ROM, false);
        AppKV.setBooleanConfig(this, AppKV.FORCE_ROM_BE_RE_INSTALL, false);

        runOnUiThread(() -> mStatus.setText(getString(R.string.import_rom_extracting, version)));
        RomManager.removeSystemAndVendor(this);
        int ret = RomManager.extractRootfs(this, target);
        if (ret != 0 || !RomManager.romExist(this)) {
            fail(getString(R.string.import_rom_extract_failed, ret));
            return;
        }
        RomManager.initRootfs(this);

        runOnUiThread(() -> {
            // Restart cleanly so the container boots with the new system.
            RomManager.reboot(this);
        });
    }

    // ---- network ----

    private String findLatestOfficialApk() throws Exception {
        HttpURLConnection api = open(LATEST_RELEASE_API);
        api.setRequestProperty("Accept", "application/vnd.github+json");
        String json;
        try (InputStream in = api.getInputStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(in, bos);
            json = bos.toString("UTF-8");
        }
        JSONArray assets = new JSONObject(json).getJSONArray("assets");
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            if (a.getString("name").endsWith(".apk")) {
                return a.getString("browser_download_url");
            }
        }
        throw new IOException("no apk asset");
    }

    private void download(String url, File out) throws IOException {
        HttpURLConnection conn = open(url);
        int code = conn.getResponseCode();
        if (code != 200) throw new IOException("HTTP " + code);
        long total = conn.getContentLengthLong();
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream os = new FileOutputStream(out)) {
            byte[] buf = new byte[256 * 1024];
            long done = 0;
            long lastUi = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                os.write(buf, 0, n);
                done += n;
                long now = SystemClock.uptimeMillis();
                if (now - lastUi > 300) {
                    lastUi = now;
                    final long d = done;
                    runOnUiThread(() -> showProgress(d, total));
                }
            }
            if (total > 0 && done != total) throw new IOException("incomplete download");
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

    // ---- ui helpers ----

    private void showProgress(long done, long total) {
        if (total > 0) {
            mProgress.setIndeterminate(false);
            mProgress.setProgress((int) (done * 1000 / total));
        }
        mStatus.setText(getString(R.string.import_rom_downloading, done >> 20, Math.max(total, 0) >> 20));
    }

    private File tmpFile() {
        return new File(getCacheDir(), "rom_import.tmp");
    }

    private void fail(String msg) {
        runOnUiThread(() -> setBusy(false, msg));
    }

    private void setBusy(boolean busy, String status) {
        for (Button b : mButtons) b.setEnabled(!busy);
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
