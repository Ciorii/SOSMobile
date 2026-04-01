package com.example.sos.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;

import com.example.sos.receivers.WeatherAlertReceiver;

import java.util.concurrent.TimeUnit;

public final class WeatherAlertScheduler {

    public static final String PREF_METEO = "weatherEnabled";
    private static final int REQ = 3002;
    private static final long CHECK_MS = TimeUnit.HOURS.toMillis(2);
    private static final long START_MS = TimeUnit.MINUTES.toMillis(5);

    private WeatherAlertScheduler() {
    }

    public static boolean isOn(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("SOSPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_METEO, false);
    }

    public static void setOn(Context ctx, boolean on) {
        SharedPreferences prefs = ctx.getSharedPreferences("SOSPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_METEO, on).apply();
        if (on) {
            plan(ctx);
        } else {
            stop(ctx);
        }
    }

    public static void ensure(Context ctx) {
        if (isOn(ctx)) {
            plan(ctx);
        }
    }

    public static void plan(Context ctx) {
        AlarmManager alarm = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        PendingIntent pi = makePi(ctx);
        long firstAt = System.currentTimeMillis() + START_MS;
        alarm.setInexactRepeating(AlarmManager.RTC, firstAt, CHECK_MS, pi);
    }

    public static void stop(Context ctx) {
        AlarmManager alarm = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) return;
        PendingIntent pi = makePi(ctx);
        alarm.cancel(pi);
    }

    private static PendingIntent makePi(Context ctx) {
        return PendingIntent.getBroadcast(
                ctx,
                REQ,
                WeatherAlertReceiver.makeIntent(ctx, false),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
