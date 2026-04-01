package com.example.sos.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.SmsManager;

public class ScheduledMessage extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("SOSPrefs", Context.MODE_PRIVATE);

        boolean ok = prefs.getBoolean("scheduledEnabled", false);
        if (!ok) return;

        String msg = prefs.getString("scheduledMessage", "");
        if (msg == null || msg.trim().isEmpty()) return;

        int n = prefs.getInt("contactCount", 0);
        SmsManager sms = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? context.getSystemService(SmsManager.class)
                : SmsManager.getDefault();

        for (int i = 0; i < n; i++) {
            String nr = prefs.getString("contactNumber_" + i, "");
            if (!nr.isEmpty()) {
                sms.sendTextMessage(nr, null, msg, null, null);
            }
        }

        prefs.edit().putBoolean("scheduledEnabled", false).apply();
    }
}
