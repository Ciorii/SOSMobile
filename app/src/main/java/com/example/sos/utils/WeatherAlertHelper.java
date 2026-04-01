package com.example.sos.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.sos.BuildConfig;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WeatherAlertHelper {

    private static final double MAX_WIND = 25;
    private static final String[] BAD_WORDS = {"storm", "thunder","flood"};
    private static final int CONN_MS = 8000;
    private static final int READ_MS = 8000;
    private static final ExecutorService EX = Executors.newSingleThreadExecutor();

    private WeatherAlertHelper() {
    }

    public static void checkLast(Context ctx, boolean show, Runnable cb) {
        if (ctx == null) return;

        Context app = ctx.getApplicationContext();
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            toastIf(app, show, "Perm loc");
            end(cb);
            return;
        }

        FusedLocationProviderClient loc =
                LocationServices.getFusedLocationProviderClient(app);
        loc.getLastLocation().addOnSuccessListener(locRes -> {
            if (locRes == null) {
                toastIf(app, show, "Loc lipsa");
                end(cb);
                return;
            }
            check(app, locRes, show, cb);
        }).addOnFailureListener(e -> {
            toastIf(app, show, "Loc lipsa");
            end(cb);
        });
    }

    private static void check(Context ctx, Location loc, boolean show, Runnable cb) {
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            toastIf(ctx, show, "Cheie meteo");
            end(cb);
            return;
        }

        EX.execute(() -> {
            boolean extreme;
            HttpURLConnection conn = null;
            try {
                String urlText = "https://api.openweathermap.org/data/2.5/weather"
                        + "?lat=" + loc.getLatitude()
                        + "&lon=" + loc.getLongitude()
                        + "&appid=" + apiKey
                        + "&units=metric";
                URL url = new URL(urlText);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(CONN_MS);
                conn.setReadTimeout(READ_MS);

                if (conn.getResponseCode() != 200) {
                    toastIf(ctx, show, "Eroare meteo");
                    end(cb);
                    return;
                }

                String response;
                try (Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                    sc.useDelimiter("\\A");
                    response = sc.hasNext() ? sc.next() : "";
                }
                JSONObject obj = new JSONObject(response);
                double wind = 0;
                JSONObject windObj = obj.optJSONObject("wind");
                if (windObj != null) wind = windObj.optDouble("speed", 0);

                String main = "";
                String desc = "";
                JSONArray weatherArr = obj.optJSONArray("weather");
                if (weatherArr != null && weatherArr.length() > 0) {
                    JSONObject w = weatherArr.getJSONObject(0);
                    main = w.optString("main", "");
                    desc = w.optString("description", "");
                }

                String lower = (main + " " + desc).toLowerCase();
                extreme = isBad(wind, lower);
            } catch (Exception e) {
                toastIf(ctx, show, "Eroare meteo");
                end(cb);
                return;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            if (extreme) {
                SmsHelper.sendAll(ctx, "Alerta meteo: vreme extrema.");
                toastIf(ctx, show, "Alerta meteo");
            } else {
                toastIf(ctx, show, "Meteo ok");
            }
            end(cb);
        });
    }

    private static boolean isBad(double wind, String summary) {
        if (wind > MAX_WIND) {
            return true;
        }
        for (String kw : BAD_WORDS) {
            if (summary.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static void toastIf(Context ctx, boolean show, String msg) {
        if (!show) return;
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        );
    }

    private static void end(Runnable cb) {
        if (cb != null) {
            cb.run();
        }
    }
}
