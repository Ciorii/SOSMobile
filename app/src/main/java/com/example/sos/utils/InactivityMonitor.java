package com.example.sos.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

public class InactivityMonitor implements SensorEventListener {

    public interface Cb {
        void onInact();
    }

    private static final float MOVE_MAX = 1.2f;
    private static final float GYRO_MAX = 0.6f;

    private final SensorManager sm;
    private final Sensor acc;
    private final Sensor gyro;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Cb cb;
    private final long waitMs;

    private long lastMove = 0L;
    private boolean run = false;
    private boolean trig = false;

    private float px, py, pz;
    private boolean hasAcc = false;

    public InactivityMonitor(Context ctx, int min, Cb cb) {
        this.cb = cb;
        this.waitMs = min * 60L * 1000L;

        sm = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        if (sm != null) {
            acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        } else {
            acc = null;
            gyro = null;
        }
    }

    public void go() {
        if (run) return;
        run = true;
        trig = false;
        lastMove = System.currentTimeMillis();
        hasAcc = false;

        if (sm != null && acc != null) {
            sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_UI);
        }
        if (sm != null && gyro != null) {
            sm.registerListener(this, gyro, SensorManager.SENSOR_DELAY_UI);
        }

        h.post(chkTask);
    }

    public void halt() {
        run = false;
        h.removeCallbacks(chkTask);
        if (sm != null) {
            sm.unregisterListener(this);
        }
    }

    private final Runnable chkTask = new Runnable() {
        @Override
        public void run() {
            if (!run) return;

            long elapsed = System.currentTimeMillis() - lastMove;
            if (!trig && elapsed >= waitMs) {
                trig = true;
                if (cb != null) cb.onInact();
            }

            h.postDelayed(this, 1000);
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!run) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            if (!hasAcc) {
                px = x;
                py = y;
                pz = z;
                hasAcc = true;
                return;
            }

            float dx = Math.abs(x - px);
            float dy = Math.abs(y - py);
            float dz = Math.abs(z - pz);

            if (dx > MOVE_MAX || dy > MOVE_MAX || dz > MOVE_MAX) {
                markMove();
            }

            px = x;
            py = y;
            pz = z;
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = Math.abs(event.values[0]);
            float y = Math.abs(event.values[1]);
            float z = Math.abs(event.values[2]);
            if (x > GYRO_MAX || y > GYRO_MAX || z > GYRO_MAX) {
                markMove();
            }
        }
    }

    private void markMove() {
        lastMove = System.currentTimeMillis();
        trig = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
