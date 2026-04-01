package com.example.sos.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.sos.R;
import com.example.sos.activities.MainActivity;
import com.example.sos.utils.InactivityMonitor;
import com.example.sos.utils.SmsHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class InactivityService extends Service {

    private static final String CH = "InactivityServiceChannel";
    private static final int NOTIF = 2;

    private SharedPreferences prefs;
    private FusedLocationProviderClient loc;
    private InactivityMonitor mon;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE);
        loc = LocationServices.getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            makeChan();
        }

        startForeground(NOTIF, makeNotif());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean ok = prefs.getBoolean("inactivityEnabled", false);
        if (!ok) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int min = prefs.getInt("inactivityMinutes", 10);
        runMon(min);
        return START_STICKY;
    }

    private void runMon(int min) {
        if (mon != null) {
            mon.halt();
        }

        mon = new InactivityMonitor(this, min, () -> onInact(min));
        mon.go();
    }

    private void onInact(int min) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            SmsHelper.sendAll(this, makeInactMsg(min, null));
            return;
        }

        loc.getLastLocation().addOnSuccessListener(loc ->
                SmsHelper.sendAll(this, makeInactMsg(min, loc))
        ).addOnFailureListener(e ->
                SmsHelper.sendAll(this, makeInactMsg(min, null))
        );
    }

    private String makeInactMsg(int min, Location loc) {
        String msg = "Nu am mai fost activ de " + min + " minute.";
        if (loc != null) {
            msg += " Locatie: " + SmsHelper.makeLink(
                    loc.getLatitude(), loc.getLongitude());
        }
        return msg;
    }

    private void makeChan() {
        NotificationChannel channel = new NotificationChannel(
                CH, "SOS Inactivity Monitor",
                NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification makeNotif() {
        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CH)
                .setContentTitle("Monitorizare inactivitate")
                .setContentText("SOS ruleaza in background")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        if (mon != null) {
            mon.halt();
            mon = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
