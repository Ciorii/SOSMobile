package com.example.sos.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryMonitor {

    public interface OnLowBatteryListener {
        void onLowBattery(int pct);
    }

    private final Context context;
    private final OnLowBatteryListener listener;
    private boolean registered = false;
    private boolean wasLow = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            if (level < 0 || scale <= 0) return;

            int pct = (int) ((level * 100f) / scale);

            if (pct <= 10) {
                if (!wasLow) {
                    wasLow = true;
                    listener.onLowBattery(pct);
                }
            } else {
                wasLow = false;
            }
        }
    };

    public BatteryMonitor(Context context, OnLowBatteryListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void register() {
        if (registered) return;
        context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registered = true;
    }

    public void unregister() {
        if (!registered) return;
        context.unregisterReceiver(receiver);
        registered = false;
    }
}
