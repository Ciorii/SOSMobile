package com.example.sos.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.sos.utils.WeatherAlertHelper;

public class WeatherAlertReceiver extends BroadcastReceiver {

    public static final String ACT_METEO = "com.example.sos.action.WEATHER_CHECK";
    public static final String EXTRA_TOAST = "extra_show_toast";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean show = intent != null && intent.getBooleanExtra(EXTRA_TOAST, false);
        PendingResult res = goAsync();
        WeatherAlertHelper.checkLast(context, show, res::finish);
    }

    public static Intent makeIntent(Context ctx, boolean show) {
        Intent i = new Intent(ctx, WeatherAlertReceiver.class);
        i.setAction(ACT_METEO);
        i.putExtra(EXTRA_TOAST, show);
        return i;
    }
}
