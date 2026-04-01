package com.example.sos.services;
import com.example.sos.utils.SosSmsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.example.sos.activities.MainActivity;
import java.util.ArrayList;

public class VoiceService extends Service {
    private SosSmsManager sos;
    private SpeechRecognizer rec;
    private SharedPreferences prefs;
    private FusedLocationProviderClient loc;
    private static final String CH = "VoiceServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE);
        sos = new SosSmsManager(this, prefs, loc);
        loc = LocationServices.getFusedLocationProviderClient(this);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            makeChan();
        }
        
        startForeground(1, makeNotif());
        startListen();
    }

    private void startListen() {
        rec = SpeechRecognizer.createSpeechRecognizer(this);
        rec.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (list != null) {
                    for (String t : list) {
                        String low = t.toLowerCase();
                        if (low.contains("sos") || low.contains("ajutor") || low.contains("help")) {
                            runSos();
                            return;
                        }
                    }
                }
                startListen();
            }

            @Override
            public void onError(int error) {
                startListen();
            }

            @Override public void onReadyForSpeech(Bundle p) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int t, Bundle b) {}
        });

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ro-RO");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        rec.startListening(intent);
    }

    private void runSos() {
    sos.getLocSms();
}

    private void makeChan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CH, "SOS Voice Service",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification makeNotif() {
        Intent notifIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CH)
                .setContentTitle("SOS Activ")
                .setContentText("Spune 'SOS' pentru ajutor")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rec != null) rec.destroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
