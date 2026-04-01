package com.example.sos.utils;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class ShakeDetector {

    private static final float MAX_SHAKE = 20f;
    private static final long SHAKE_WAIT = 8000L;
    private static final float G_ALPHA = 0.8f;

    private final SensorManager sensorManager;
    private final Runnable onShake;
    private Sensor accelerometer;
    private boolean useLinear = false;
    private final float[] gravity = new float[]{0f, 0f, 0f};
    private long lastShake = 0L;

    private final SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float magnitude;

            if (useLinear) {
                magnitude = (float) Math.sqrt(x * x + y * y + z * z);
            } else {
                gravity[0] = G_ALPHA * gravity[0] + (1 - G_ALPHA) * x;
                gravity[1] = G_ALPHA * gravity[1] + (1 - G_ALPHA) * y;
                gravity[2] = G_ALPHA * gravity[2] + (1 - G_ALPHA) * z;

                float lx = x - gravity[0];
                float ly = y - gravity[1];
                float lz = z - gravity[2];
                magnitude = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
            }

            if (magnitude < MAX_SHAKE) return;

            long now = System.currentTimeMillis();
            if (now - lastShake < SHAKE_WAIT) return;
            lastShake = now;
            onShake.run();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public ShakeDetector(SensorManager sensorManager, Runnable onShake) {
        this.sensorManager = sensorManager;
        this.onShake = onShake;

        if (sensorManager != null) {
            Sensor linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            if (linear != null) {
                accelerometer = linear;
                useLinear = true;
            } else {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                useLinear = false;
            }
        }
    }

    public void start() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(listener);
        }
    }
}
