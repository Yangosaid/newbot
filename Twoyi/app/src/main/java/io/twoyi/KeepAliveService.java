/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.twoyi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Foreground service kept while the container runs. It raises the app to foreground priority,
 * so the host (and vendor battery managers such as Honor's) does not throttle, freeze or kill
 * the container's processes, and holds a partial wake lock so the CPU keeps running.
 */
public class KeepAliveService extends Service {

    private static final String CHANNEL = "container";
    private static final int ID = 1;

    private PowerManager.WakeLock mWakeLock;

    public static void start(Context context) {
        try {
            context.startForegroundService(new Intent(context, KeepAliveService.class));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL,
                    getString(R.string.keepalive_channel), NotificationManager.IMPORTANCE_LOW));
        }
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, Render2Activity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.keepalive_text))
                .setContentIntent(open)
                .setOngoing(true)
                .build();
        startForeground(ID, n);

        PowerManager pm = getSystemService(PowerManager.class);
        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "twoyi:container");
            mWakeLock.setReferenceCounted(false);
            mWakeLock.acquire();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
