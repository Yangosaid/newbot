/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.twoyi.utils.AppKV;
import io.twoyi.utils.LogEvents;
import io.twoyi.utils.NavUtils;
import io.twoyi.utils.RomManager;

/**
 * @author weishu
 * @date 2021/10/20.
 */
public class Render2Activity extends Activity implements View.OnTouchListener {

    private static final String TAG = "Render2Activity";

    // The first boot on a new kernel can be slow; the original 15s killed it too early.
    private static final int BOOT_TIMEOUT_SECONDS = 90;

    private SurfaceView mSurfaceView;

    private ViewGroup mRootView;
    private android.widget.ProgressBar mLoadingView;
    private TextView mLoadingText;
    private View mLoadingLayout;
    private View mBootLogView;

    private final AtomicBoolean mIsExtracting = new AtomicBoolean(false);

    private int mRenderScale = 100;
    private final android.graphics.Matrix mTouchMatrix = new android.graphics.Matrix();

    private final SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            Surface surface = holder.getSurface();
            WindowManager windowManager = getWindowManager();
            Display defaultDisplay = windowManager.getDefaultDisplay();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            defaultDisplay.getRealMetrics(displayMetrics);

            float scale = mRenderScale / 100f;
            float xdpi = displayMetrics.xdpi * scale;
            float ydpi = displayMetrics.ydpi * scale;

            Renderer.init(surface, RomManager.getLoaderPath(getApplicationContext()), xdpi, ydpi, (int) getBestFps());

            Log.i(TAG, "surfaceCreated");
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            Surface surface = holder.getSurface();
            // width/height are the (possibly reduced) buffer size set with setFixedSize().
            Renderer.resetWindow(surface, 0, 0, width, height);
            Log.i(TAG, "surfaceChanged: " + mSurfaceView.getWidth() + "x" + mSurfaceView.getHeight());
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            Renderer.removeWindow(holder.getSurface());
            Log.i(TAG, "surfaceDestroyed!");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean started = TwoyiStatusManager.getInstance().isStarted();
        Log.i(TAG, "onCreate: " + savedInstanceState + " isStarted: " + started);

        if (started) {
            // we have been started, but WTF we are onCreate again? just reboot ourself.
            finish();
            RomManager.reboot(this);
            return;
        }

        if (!RomManager.romExist(this) && !RomManager.hasAssetRom(this)
                && !RomManager.getStoredRomFile(this).exists()) {
            // No ROM bundled in this APK: ask the user to import it first.
            super.onCreate(savedInstanceState);
            startActivity(new android.content.Intent(this, ImportRomActivity.class));
            finish();
            return;
        }

        // reset state
        TwoyiStatusManager.getInstance().reset();

        NavUtils.hideNavigation(getWindow());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.ac_render);
        mRootView = findViewById(R.id.root);

        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().addCallback(mSurfaceCallback);

        // Render the guest at a lower resolution and let the compositor scale it up:
        // far less GPU work and graphics memory (fixes EGL_BAD_ALLOC crashes and lag).
        mRenderScale = AppKV.getRenderScale(this);
        if (mRenderScale < 100) {
            DisplayMetrics real = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(real);
            int w = Math.max(2, Math.round(real.widthPixels * mRenderScale / 100f) & ~1);
            int h = Math.max(2, Math.round(real.heightPixels * mRenderScale / 100f) & ~1);
            mSurfaceView.getHolder().setFixedSize(w, h);
        }

        mLoadingLayout = findViewById(R.id.loadingLayout);
        mLoadingView = findViewById(R.id.loading);
        mLoadingText = findViewById(R.id.loadingText);
        mBootLogView = findViewById(R.id.bootlog);

        mLoadingLayout.setVisibility(View.VISIBLE);

        UITips.checkForAndroid12(this, this::bootSystem);

        mSurfaceView.setOnTouchListener(this);

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(TAG, "onRestoreInstanceState: " + savedInstanceState);

        // we don't support state restore, just reboot.
        finish();
        RomManager.reboot(this);
    }

    private void bootSystem() {
        KeepAliveService.start(getApplicationContext());
        requestBatteryExemptionOnce();

        boolean romExist = RomManager.romExist(this);
        boolean factoryRomUpdated = RomManager.needsUpgrade(this);
        boolean forceInstall = AppKV.getBooleanConfig(getApplicationContext(), AppKV.FORCE_ROM_BE_RE_INSTALL, false);
        boolean use3rdRom = AppKV.getBooleanConfig(getApplicationContext(), AppKV.SHOULD_USE_THIRD_PARTY_ROM, false);

        boolean shouldExtractRom = !romExist || forceInstall || (!use3rdRom && factoryRomUpdated);

        if (shouldExtractRom) {
            Log.i(TAG, "extracting rom...");

            showTipsForFirstBoot();

            new Thread(() -> {
                mIsExtracting.set(true);
                RomManager.extractRootfs(getApplicationContext(), romExist, factoryRomUpdated, forceInstall, use3rdRom);
                mIsExtracting.set(false);

                RomManager.initRootfs(getApplicationContext());

                runOnUiThread(() -> {
                    mRootView.addView(mSurfaceView, 0);
                    showBootingProcedure();
                });
            }, "extract-rom").start();
        } else {
            // Refresh language/timezone/density and renderer props on every boot.
            RomManager.initRootfs(getApplicationContext());
            RomManager.applySystemPatches(getApplicationContext());
            mRootView.addView(mSurfaceView, 0);
            showBootingProcedure();
        }
    }

    private void showTipsForFirstBoot() {
        mLoadingText.setText(R.string.extracting_tips);
        mRootView.postDelayed(() -> {
            if (mIsExtracting.get()) {
                mLoadingText.setText(R.string.first_boot_tips);
            }
        }, 5000);

        mRootView.postDelayed(() -> {
            if (mIsExtracting.get()) {
                mLoadingText.setText(R.string.first_boot_tips2);
            }
        }, 10 * 1000);

        mRootView.postDelayed(() -> {
            if (mIsExtracting.get()) {
                mLoadingText.setText(R.string.first_boot_tips3);
            }
        }, 15 * 1000);
    }

    private void showBootingProcedure() {
        // mLoadingText.setText(R.string.booting_tips);
        mLoadingText.setVisibility(View.GONE);
        mBootLogView.setVisibility(View.VISIBLE);
        new Thread(() -> {

            if (true) {
                boolean success = false;
                try {
                    success = TwoyiStatusManager.getInstance().waitBoot(BOOT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (Throwable ignored) {
                }

                if (!success) {
                    runOnUiThread(this::showBootFailure);
                    return;
                }
            }

            runOnUiThread(() -> {
                mLoadingLayout.setVisibility(View.GONE);
            });
        }, "waiting-boot").start();
    }

    /** Ask once to be excluded from battery optimisation so the container is not throttled. */
    private void requestBatteryExemptionOnce() {
        if (AppKV.getBooleanConfig(this, "battery_exemption_asked", false)) return;
        AppKV.setBooleanConfig(this, "battery_exemption_asked", true);
        try {
            android.os.PowerManager pm = getSystemService(android.os.PowerManager.class);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:" + getPackageName())));
            }
        } catch (Throwable ignored) {
        }
    }

    private void showBootFailure() {
        if (isFinishing()) return;
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.boot_failed)
                .setMessage(R.string.boot_failed_help)
                .setCancelable(false)
                .setPositiveButton(R.string.settings_key_sendlog, (d, w) -> {
                    Toast.makeText(this, R.string.collecting_log, Toast.LENGTH_LONG).show();
                    new Thread(this::shareBugreport, "bugreport").start();
                })
                .setNeutralButton(R.string.open_developer_options, (d, w) -> {
                    UITips.openDeveloperOptions(this);
                    finish();
                    RomManager.shutdown(this);
                })
                .setNegativeButton(R.string.close_app, (d, w) -> {
                    finish();
                    RomManager.shutdown(this);
                })
                .show();
    }

    private void shareBugreport() {
        try {
            java.io.File out = new java.io.File(getCacheDir(), "bugreport.zip");
            java.nio.file.Files.write(out.toPath(), LogEvents.getBugreport(this));
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(this, "io.twoyi.fileprovider", out);
            android.content.Intent share = new android.content.Intent(android.content.Intent.ACTION_SEND);
            share.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            share.setType("application/zip");
            share.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            runOnUiThread(() -> startActivity(android.content.Intent.createChooser(share, getString(R.string.settings_key_sendlog))));
        } catch (Throwable e) {
            Log.e(TAG, "share bugreport failed", e);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            NavUtils.hideNavigation(getWindow());
        }

        // Update global visibility.
        TwoyiStatusManager.getInstance().updateVisibility(hasFocus);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mRenderScale >= 100) {
            Renderer.handleTouch(event);
            return true;
        }
        // Map view coordinates to the reduced guest resolution.
        android.graphics.Rect frame = mSurfaceView.getHolder().getSurfaceFrame();
        if (v.getWidth() == 0 || v.getHeight() == 0 || frame.width() == 0) {
            return true;
        }
        mTouchMatrix.setScale(frame.width() / (float) v.getWidth(), frame.height() / (float) v.getHeight());
        MotionEvent scaled = MotionEvent.obtain(event);
        scaled.transform(mTouchMatrix);
        Renderer.handleTouch(scaled);
        scaled.recycle();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // TODO: 2021/10/26 Add Volume control
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        Renderer.sendKeycode(KeyEvent.KEYCODE_HOME);
    }

    private float getBestFps() {
        WindowManager windowManager = getWindowManager();
        Display defaultDisplay = windowManager.getDefaultDisplay();
        Display.Mode[] supportedModes = defaultDisplay.getSupportedModes();
        float fps = 45;
        for (Display.Mode supportedMode : supportedModes) {
            float refreshRate = supportedMode.getRefreshRate();
            if (refreshRate > fps) {
                // fps = refreshRate;
            }
        }

        Log.w(TAG, "current fps: " + fps);
        return fps;
    }
}
