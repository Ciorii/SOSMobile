package com.example.sos.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;

import com.google.android.gms.location.FusedLocationProviderClient;

public class SosSmsManager {

    private final Context context;
    private final SharedPreferences prefs;
    private final FusedLocationProviderClient fusedLocationClient;

    public SosSmsManager(Context context, SharedPreferences prefs, FusedLocationProviderClient fusedLocationClient) {
        this.context = context;
        this.prefs = prefs;
        this.fusedLocationClient = fusedLocationClient;
    }

    public void sendSms(String msg) {
        SmsHelper.sendAll(context, msg);
        Toast.makeText(context, "SMS trimis", Toast.LENGTH_SHORT).show();
    }

    public String makeMsg(Location location) {
        String msg = prefs.getString("emergencyMessage", "Ajutor! SOS!");
        if (location != null) {
            msg += "\nLocatie: " + SmsHelper.makeLink(
                    location.getLatitude(), location.getLongitude());
        }
        return msg;
    }

    public String makeLowBatMsg(int pct, Location location) {
        String msg = "Baterie scazuta (" + pct + "%).";
        if (location != null) {
            msg += " Ultima locatie: "
                    + SmsHelper.makeLink(location.getLatitude(), location.getLongitude());
        }
        return msg;
    }

    public void getLocSms() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            sendSms(makeMsg(null));
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location ->
                sendSms(makeMsg(location))
        ).addOnFailureListener(e ->
                sendSms(makeMsg(null))
        );
    }

    public void getLocSmsBat(int pct) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            sendSms(makeLowBatMsg(pct, null));
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location ->
                sendSms(makeLowBatMsg(pct, location))
        ).addOnFailureListener(e ->
                sendSms(makeLowBatMsg(pct, null))
        );
    }
}
