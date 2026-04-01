package com.example.sos.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public final class CallHelper {

    private CallHelper() {
    }

    public static void callFirst(Activity activity, SharedPreferences prefs) {
        String nr = prefs.getString("contactNumber_0", "");
        if (nr.isEmpty()) return;

        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + nr));

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity, "Perm apel", Toast.LENGTH_SHORT).show();
        }
    }
}
