package com.example.sos.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;

import com.example.sos.R;
import com.example.sos.services.VoiceService;
import com.example.sos.utils.BatteryMonitor;
import com.example.sos.utils.CallHelper;
import com.example.sos.utils.GeofenceMonitor;
import com.example.sos.utils.ShakeDetector;
import com.example.sos.utils.SosSmsManager;
import com.example.sos.utils.SosVideoManager;
import com.example.sos.utils.WeatherAlertHelper;
import com.example.sos.utils.WeatherAlertScheduler;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_SOS = "extra_trigger_sos";
    private SharedPreferences prefs;
    private FusedLocationProviderClient loc;

    private TextView tvNum;
    private Button btnCancel;
    private CountDownTimer timer;
    private boolean inCd = false;

    private SosSmsManager smsManager;
    private ShakeDetector shakeDetector;
    private BatteryMonitor batteryMonitor;
    private GeofenceMonitor geofenceMonitor;
    private SosVideoManager vid;
    private PreviewView prev;
    private Button btnStopVid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE);

        if (prefs.getBoolean("FirstRun", true)) {
            startActivity(new Intent(this, SetupActivity.class));
            finish();
            return;
        }

        prev = findViewById(R.id.previewView);
        btnStopVid = findViewById(R.id.btnStopRecording);

        loc = LocationServices.getFusedLocationProviderClient(this);
        smsManager = new SosSmsManager(this, prefs, loc);

        vid = new SosVideoManager(this, prev, btnStopVid, smsManager,
                () -> CallHelper.callFirst(this, prefs));

        android.hardware.SensorManager sens =
                (android.hardware.SensorManager) getSystemService(Context.SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(sens, this::startSos);

        batteryMonitor = new BatteryMonitor(this, this::onLowBat);
        geofenceMonitor = new GeofenceMonitor(this, prefs, loc, smsManager);

        tvNum = findViewById(R.id.tvCountdown);
        btnCancel = findViewById(R.id.btnCancel);

        askPerms();

        Button btnS = findViewById(R.id.btnSOS);
        btnS.setOnClickListener(v -> startSos());

        btnCancel.setOnClickListener(v -> cancelSos());

        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        setupVoice();
        WeatherAlertScheduler.ensure(this);
        if (WeatherAlertScheduler.isOn(this)) {
            WeatherAlertHelper.checkLast(this, false, null);
        }
        checkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (geofenceMonitor != null) geofenceMonitor.start();
        if (batteryMonitor != null) batteryMonitor.register();
        if (vid != null) vid.startCam();
        if (shakeDetector != null) shakeDetector.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (geofenceMonitor != null) geofenceMonitor.stop();
        if (batteryMonitor != null) batteryMonitor.unregister();
        if (shakeDetector != null) shakeDetector.stop();
    }

    private void askPerms() {
        ArrayList<String> pList = new ArrayList<>();
        pList.add(Manifest.permission.SEND_SMS);
        pList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        pList.add(Manifest.permission.RECORD_AUDIO);
        pList.add(Manifest.permission.CAMERA);
        pList.add(Manifest.permission.CALL_PHONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pList.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        ActivityCompat.requestPermissions(this, pList.toArray(new String[0]), 1);
    }

    private void startSos() {
        if (inCd) return;

        inCd = true;
        tvNum.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);

        timer = new CountDownTimer(2_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvNum.setText(String.valueOf(millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                inCd = false;
                tvNum.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
                runSos();
            }
        }.start();
    }

    private void cancelSos() {
        if (timer == null) return;

        timer.cancel();
        inCd = false;
        tvNum.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
    }

    private void runSos() {
        smsManager.getLocSms();
        vid.startRec();
    }

    public void callFirst() {
        CallHelper.callFirst(this, prefs);
    }

    private void setupVoice() {
        Switch sw = findViewById(R.id.switchVoice);
        sw.setChecked(prefs.getBoolean("voiceEnabled", false));
        sw.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("voiceEnabled", isChecked).apply();

            Intent intent = new Intent(this, VoiceService.class);
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            } else {
                stopService(intent);
            }
        });
    }

    private void onLowBat(int pct) {
        Toast.makeText(this, "Bat " + pct + "%", Toast.LENGTH_LONG).show();
        smsManager.getLocSmsBat(pct);
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS);
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void checkIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getBooleanExtra(EXTRA_SOS, false)) {
            intent.removeExtra(EXTRA_SOS);
            startSos();
        }
    }
}

