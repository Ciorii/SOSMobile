package com.example.sos.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsManager;

public final class SmsHelper {

    private SmsHelper() {
    }

    public static String makeLink(double lat, double lng) {
        return "https://maps.google.com/?q=" + lat + "," + lng;
    }

    public static void sendAll(Context ctx, String msg) {
        if (ctx == null || msg == null) return;

        SharedPreferences prefs = ctx.getSharedPreferences("SOSPrefs", Context.MODE_PRIVATE);
        int n = prefs.getInt("contactCount", 0);
        SmsManager sms = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? ctx.getSystemService(SmsManager.class)
                : SmsManager.getDefault();

        for (int i = 0; i < n; i++) {
            String nr = prefs.getString("contactNumber_" + i, "");
            if (!nr.isEmpty()) {
                sms.sendTextMessage(nr, null, msg, null, null);
            }
        }
    }
}
