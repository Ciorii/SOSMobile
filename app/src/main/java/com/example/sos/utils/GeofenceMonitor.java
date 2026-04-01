package com.example.sos.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.example.sos.models.SafeZone;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.ArrayList;

public class GeofenceMonitor {

    private final Context context;
    private final SharedPreferences prefs;
    private final FusedLocationProviderClient fusedLocationClient;
    private final SosSmsManager smsManager;

    private boolean wasIn = false;
    private long lastGeo = 0L;
    private boolean running = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable job = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            checkOnce();
            handler.postDelayed(this, 25_000);
        }
    };

    public GeofenceMonitor(Context context, SharedPreferences prefs,
                           FusedLocationProviderClient fusedLocationClient,
                           SosSmsManager smsManager) {
        this.context = context;
        this.prefs = prefs;
        this.fusedLocationClient = fusedLocationClient;
        this.smsManager = smsManager;
    }

    public void start() {
        boolean on = prefs.getBoolean("geofenceEnabled", false);
        if (!on) return;
        running = true;
        handler.post(job);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(job);
    }

    private void checkOnce() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ArrayList<SafeZone> zones = SafeZoneStorage.read(prefs);
        if (zones.isEmpty()) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) return;

            SafeZone inZone = findZone(zones, location);
            boolean inAny = inZone != null;

            if (wasIn && !inAny) {
                sendGeoAlert(location);
            }

            wasIn = inAny;
        });
    }

    private SafeZone findZone(ArrayList<SafeZone> zones, Location location) {
        for (SafeZone z : zones) {
            float[] results = new float[1];
            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                    z.la, z.lo, results);
            if (results[0] <= z.r) return z;
        }
        return null;
    }

    private void sendGeoAlert(Location location) {
        long now = System.currentTimeMillis();
        if (now - lastGeo < 5 * 60_000L) return;
        lastGeo = now;

        beep();

        String msg = "Am iesit din zona sigura. Locatie: "
                + SmsHelper.makeLink(location.getLatitude(), location.getLongitude());
        smsManager.sendSms(msg);
    }

    private void beep() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ALARM, 90);
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700);
            handler.postDelayed(tg::release, 800);
        } catch (Exception ignored) {
        }
    }
}
