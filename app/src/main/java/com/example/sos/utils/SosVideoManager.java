package com.example.sos.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SosVideoManager {

    private final AppCompatActivity a;
    private final PreviewView prev;
    private final Button btnStop;
    private final SosSmsManager smsManager;
    private final Runnable onCallFirst;

    private VideoCapture<Recorder> cap;
    private Recording rec;
    private File vid;
    private final ExecutorService exe = Executors.newSingleThreadExecutor();

    public SosVideoManager(AppCompatActivity activity, PreviewView previewView, Button btnStop,
                           SosSmsManager smsManager, Runnable onCallFirst) {
        this.a = activity;
        this.prev = previewView;
        this.btnStop = btnStop;
        this.smsManager = smsManager;
        this.onCallFirst = onCallFirst;
        btnStop.setOnClickListener(v -> stopRec());
    }

    public void startRec() {
        if (!hasPerm()) {
            Toast.makeText(a, "Perm cam", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cap == null) startCam();

        vid = new File(a.getFilesDir(),
                "sos_video_" + System.currentTimeMillis() + ".mp4");

        FileOutputOptions outOpt = new FileOutputOptions.Builder(vid).build();

        rec = cap.getOutput()
                .prepareRecording(a, outOpt)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(a), ev -> {
                    if (ev instanceof VideoRecordEvent.Start) {
                        btnStop.setVisibility(View.VISIBLE);
                    } else if (ev instanceof VideoRecordEvent.Finalize) {
                        btnStop.setVisibility(View.GONE);
                        sendCloud(vid);
                    }
                });

        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(this::stopRec, 20_000);
    }

    public void stopRec() {
        if (rec != null) {
            rec.stop();
            rec = null;
        }
    }

    private boolean hasPerm() {
        return ActivityCompat.checkSelfPermission(a, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(a, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    public void startCam() {
        if (!hasPerm() || cap != null) return;

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(a);

        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(prev.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder().build();
                cap = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(a, CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, cap);

            } catch (Exception e) {
                Toast.makeText(a, "Eroare cam", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(a));
    }

    private void sendCloud(File file) {
        exe.execute(() -> {
            try {
                String boundary = "Boundary-" + System.currentTimeMillis();
                URL url = new URL("https://api.cloudinary.com/v1_1/dikio5jom/auto/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream out = conn.getOutputStream();

                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n".getBytes());
                out.write("sosapl\r\n".getBytes());

                out.write(("--" + boundary + "\r\n").getBytes());
                out.write("Content-Disposition: form-data; name=\"file\"; filename=\"video.mp4\"\r\n".getBytes());
                out.write("Content-Type: video/mp4\r\n\r\n".getBytes());

                try (InputStream in = new FileInputStream(file)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                }

                out.write("\r\n".getBytes());
                out.write(("--" + boundary + "--\r\n").getBytes());
                out.flush();
                out.close();

                InputStream response = conn.getInputStream();
                String responseText = new Scanner(response).useDelimiter("\\A").next();

                String link = new JSONObject(responseText).getString("secure_url");

                a.runOnUiThread(() -> {
                    smsManager.sendSms("Clip SOS: " + link);
                    onCallFirst.run();
                });

            } catch (Exception e) {
                a.runOnUiThread(() ->
                        Toast.makeText(a, "Eroare up", Toast.LENGTH_LONG).show());
            }
        });
    }
}
