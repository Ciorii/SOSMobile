package com.example.sos.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sos.R;
import com.example.sos.models.SafeZone;
import com.example.sos.services.InactivityService;
import com.example.sos.utils.SafeZoneStorage;
import com.example.sos.utils.WeatherAlertHelper;
import com.example.sos.utils.WeatherAlertScheduler;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    private EditText msgEt;
    private LinearLayout contBox;
    private Button saveBtn;
    private Button addBtn;
    private SharedPreferences prefs;
    private ArrayList<String> nameList = new ArrayList<>();
    private ArrayList<String> numList = new ArrayList<>();
    private Switch swIn;
    private EditText inMinEt;
    private Switch swG;
    private Button zoneBtn;
    private LinearLayout zoneBox;
    private EditText planMsgEt;
    private EditText planMinEt;
    private Button planBtn;
    private Button cancelPlanBtn;
    private Button meteoBtn;
    private Switch swMeteo;

    private ArrayList<SafeZone> zoneList = new ArrayList<>();

    private final ActivityResultLauncher<Void> pickContact =
            registerForActivityResult(new ActivityResultContracts.PickContact(), uri -> {
                if (uri != null) {
                    getContact(uri);
                }
            });

    private final ActivityResultLauncher<String> permContact =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickContact.launch(null);
                } else {
                    Toast.makeText(this, "Perm contacte", Toast.LENGTH_SHORT).show();
                }
            });
    private final ActivityResultLauncher<Intent> pickMap =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) return;
                Intent data = result.getData();
                if (data == null || !data.hasExtra(MapPickerActivity.EXTRA_X)
                        || !data.hasExtra(MapPickerActivity.EXTRA_Y)) {
                    Toast.makeText(this, "Zona err", Toast.LENGTH_SHORT).show();
                    return;
                }

                double lat = data.getDoubleExtra(MapPickerActivity.EXTRA_X, 0);
                double lng = data.getDoubleExtra(MapPickerActivity.EXTRA_Y, 0);
                int radius = data.getIntExtra(MapPickerActivity.EXTRA_R, 100);

                zoneList.add(new SafeZone("Zona sigura", lat, lng, radius));
                showZones();

                Toast.makeText(this, "Zona ok", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE);
        msgEt = findViewById(R.id.etMessage);
        contBox = findViewById(R.id.llContacts);
        addBtn = findViewById(R.id.btnAddContact);
        saveBtn = findViewById(R.id.btnSave);
        swIn = findViewById(R.id.switchInactivity);
        inMinEt = findViewById(R.id.etInactivityMinutes);
        swG = findViewById(R.id.switchGeofence);
        zoneBtn = findViewById(R.id.btnPickZone);
        zoneBox = findViewById(R.id.llZones);
        planMsgEt = findViewById(R.id.etScheduledMessage);
        planMinEt = findViewById(R.id.etScheduledMinutes);
        planBtn = findViewById(R.id.btnSchedule);
        cancelPlanBtn = findViewById(R.id.btnCancelSchedule);
        meteoBtn = findViewById(R.id.btnWeatherTest);
        swMeteo = findViewById(R.id.switchWeather);

        zoneList = SafeZoneStorage.read(prefs);
        showZones();

        zoneBtn.setOnClickListener(v ->
                pickMap.launch(new Intent(this, MapPickerActivity.class)));

        load();

        planBtn.setOnClickListener(v -> planMsg());
        cancelPlanBtn.setOnClickListener(v -> stopPlan());
        meteoBtn.setOnClickListener(v -> testMeteo());
        swMeteo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            WeatherAlertScheduler.setOn(this, isChecked);
            if (isChecked) {
                WeatherAlertHelper.checkLast(this, true, null);
            }
        });

        saveBtn.setOnClickListener(v -> save());
        addBtn.setOnClickListener(v -> {
            if (numList.size() >= 5) {
                Toast.makeText(this, "Max 5", Toast.LENGTH_SHORT).show();
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED) {
                    pickContact.launch(null);
                } else {
                    permContact.launch(Manifest.permission.READ_CONTACTS);
                }
            }
        });
        showContacts();
    }

    private void testMeteo() {
        WeatherAlertHelper.checkLast(this, true, null);
    }

    private void load() {
        nameList.clear();
        numList.clear();
        msgEt.setText(prefs.getString("emergencyMessage", "Ai comandat sos"));
        int n = prefs.getInt("contactCount", 0);
        for (int i = 0; i < n; i++) {
            nameList.add(prefs.getString("contactName_" + i, ""));
            numList.add(prefs.getString("contactNumber_" + i, ""));
        }
        planMsgEt.setText(prefs.getString("scheduledMessage", ""));
        planMinEt.setText(String.valueOf(prefs.getInt("scheduledMinutes", 30)));

        swIn.setChecked(prefs.getBoolean("inactivityEnabled", false));
        inMinEt.setText(String.valueOf(prefs.getInt("inactivityMinutes", 10)));
        swG.setChecked(prefs.getBoolean("geofenceEnabled", false));
        swMeteo.setChecked(WeatherAlertScheduler.isOn(this));
    }

    private void showContacts() {
        contBox.removeAllViews();
        for (int i = 0; i < nameList.size(); i++) {
            final int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView tv = new TextView(this);
            tv.setText("✓ " + nameList.get(i) + " (" + numList.get(i) + ")");
            tv.setPadding(0, 12, 0, 12);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button btnDel = new Button(this);
            btnDel.setText("✕");
            btnDel.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
            btnDel.setTextColor(0xFFFFFFFF);
            btnDel.setOnClickListener(v -> {
                nameList.remove(index);
                numList.remove(index);
                showContacts();
            });

            row.addView(tv);
            row.addView(btnDel);
            contBox.addView(row);
        }

        if (addBtn != null) {
            if (nameList.isEmpty()) {
                addBtn.setText("+ Adauga contact");
            } else {
                addBtn.setText("Adauga alt contact (" + nameList.size() + "/5)");
            }
        }
    }

    private void save() {
        String msg = msgEt.getText().toString().trim();
        if (msg.isEmpty()) {
            Toast.makeText(this, "Mesaj gol", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("emergencyMessage", msg);
        editor.putInt("contactCount", numList.size());
        for (int i = 0; i < numList.size(); i++) {
            editor.putString("contactName_" + i, nameList.get(i));
            editor.putString("contactNumber_" + i, numList.get(i));
        }

        boolean inactOn = swIn.isChecked();
        int inactMin = 10;
        try {
            String s = inMinEt.getText().toString().trim();
            if (!s.isEmpty()) inactMin = Integer.parseInt(s);
        } catch (NumberFormatException ignored) {}
        if (inactMin < 10) inactMin = 10;
        if (inactMin > 30) inactMin = 30;
        editor.putBoolean("inactivityEnabled", inactOn);
        editor.putInt("inactivityMinutes", inactMin);
        editor.putBoolean("geofenceEnabled", swG.isChecked());

        editor.commit();
        SafeZoneStorage.write(prefs, zoneList);

        Intent inactivityIntent = new Intent(this, InactivityService.class);
        if (inactOn) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(inactivityIntent);
            } else {
                startService(inactivityIntent);
            }
        } else {
            stopService(inactivityIntent);
        }

        Toast.makeText(this, "Salvat", Toast.LENGTH_SHORT).show();
        finish();
    }
    private void showZones() {
        zoneBox.removeAllViews();

        if (zoneList.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Nicio zona salvata");
            tv.setPadding(0, 8, 0, 8);
            zoneBox.addView(tv);
            return;
        }

        for (int i = 0; i < zoneList.size(); i++) {
            final int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView tv = new TextView(this);
            tv.setText("✓ Zona sigura " + (index + 1));
            tv.setPadding(0, 12, 0, 12);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button btnDel = new Button(this);
            btnDel.setText("✕");
            btnDel.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));
            btnDel.setTextColor(0xFFFFFFFF);
            btnDel.setOnClickListener(v -> {
                zoneList.remove(index);
                showZones();
            });

            row.addView(tv);
            row.addView(btnDel);
            zoneBox.addView(row);
        }
    }

    private void getContact(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) return;

            int idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            int nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);

            if (idIdx < 0 || nameIdx < 0) { cursor.close(); return; }

            String cid = cursor.getString(idIdx);
            String name = cursor.getString(nameIdx);
            cursor.close();

            Cursor phoneCursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                    new String[]{cid}, null);

            if (phoneCursor == null || !phoneCursor.moveToFirst()) {
                Toast.makeText(this, "Fara nr", Toast.LENGTH_SHORT).show();
                if (phoneCursor != null) phoneCursor.close();
                return;
            }

            int numberIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if (numberIdx < 0) { phoneCursor.close(); return; }

            String nr = phoneCursor.getString(numberIdx);
            phoneCursor.close();

            if (numList.contains(nr)) {
                Toast.makeText(this, "Deja", Toast.LENGTH_SHORT).show();
            } else {
                numList.add(nr);
                nameList.add(name);
                showContacts();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Eroare", Toast.LENGTH_SHORT).show();
        }
    }
    private void planMsg() {
        String msg = planMsgEt.getText().toString().trim();
        String minText = planMinEt.getText().toString().trim();

        if (msg.isEmpty() || minText.isEmpty()) {
            Toast.makeText(this, "Msg+min", Toast.LENGTH_SHORT).show();
            return;
        }

        int min = Integer.parseInt(minText);
        if (min < 1) min = 1;

        long triggerAt = System.currentTimeMillis() + min * 60_000L;

        prefs.edit()
                .putBoolean("scheduledEnabled", true)
                .putString("scheduledMessage", msg)
                .putInt("scheduledMinutes", min)
                .apply();

        android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        android.app.PendingIntent pi = planPi();

        am.set(android.app.AlarmManager.RTC_WAKEUP, triggerAt, pi);

        Toast.makeText(this, "Plan " + min + " min", Toast.LENGTH_SHORT).show();
    }

    private void stopPlan() {
        prefs.edit().putBoolean("scheduledEnabled", false).apply();

        android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        android.app.PendingIntent pi = planPi();
        am.cancel(pi);

        Toast.makeText(this, "Anulat", Toast.LENGTH_SHORT).show();
    }

    private android.app.PendingIntent planPi() {
        Intent intent = new Intent(this, com.example.sos.utils.ScheduledMessage.class);
        return android.app.PendingIntent.getBroadcast(
                this, 2001, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );
    }
}
