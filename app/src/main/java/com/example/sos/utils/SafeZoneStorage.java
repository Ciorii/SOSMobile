package com.example.sos.utils;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.sos.models.SafeZone;

import java.util.ArrayList;

public class SafeZoneStorage {

    private static final String KEY_JSON = "safeZonesJson";

    public static ArrayList<SafeZone> read(SharedPreferences prefs) {
        ArrayList<SafeZone> out = new ArrayList<>();
        String txt = prefs.getString(KEY_JSON, "[]");

        try {
            JSONArray arr = new JSONArray(txt);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String n = obj.optString("name", "Zona");
                double la = obj.optDouble("lat", 0);
                double lo = obj.optDouble("lng", 0);
                int r = obj.optInt("radius", 100);
                out.add(new SafeZone(n, la, lo, r));
            }
        } catch (Exception ignored) { }

        return out;
    }

    public static void write(SharedPreferences prefs, ArrayList<SafeZone> zones) {
        JSONArray arr = new JSONArray();
        try {
            for (SafeZone z : zones) {
                JSONObject obj = new JSONObject();
                obj.put("name", z.n);
                obj.put("lat", z.la);
                obj.put("lng", z.lo);
                obj.put("radius", z.r);
                arr.put(obj);
            }
        } catch (Exception ignored) { }

        prefs.edit().putString(KEY_JSON, arr.toString()).apply();
    }
}
