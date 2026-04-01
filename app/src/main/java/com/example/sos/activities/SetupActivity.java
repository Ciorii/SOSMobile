package com.example.sos.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sos.R;

import java.util.ArrayList;

public class SetupActivity extends AppCompatActivity {
    private EditText msgEt;
    private TextView contTv;
    private Button pickBtn;
    private Button saveBtn;
    private SharedPreferences prefs;
    private ArrayList<String> numList = new ArrayList<>();
    private ArrayList<String> nameList = new ArrayList<>();

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE);
        msgEt = findViewById(R.id.etEmergencyMessage);
        contTv = findViewById(R.id.tvSelectedContacts);
        pickBtn = findViewById(R.id.btnSelectContacts);
        saveBtn = findViewById(R.id.btnSave);

        load();

        pickBtn.setOnClickListener(v -> {
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

        saveBtn.setOnClickListener(v -> saveGo());
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

    private void showContacts() {
        if (nameList.isEmpty()) {
            contTv.setText("Niciun contact selectat");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nameList.size(); i++) {
            sb.append("✓ ").append(nameList.get(i))
                    .append(" (").append(numList.get(i)).append(")");
            if (i < nameList.size() - 1) sb.append("\n");
        }
        contTv.setText(sb.toString());
        pickBtn.setText("Adauga alt contact (" + numList.size() + "/5)");
    }

    private void saveGo() {
        String message = msgEt.getText().toString().trim();

        if (message.isEmpty()) {
            msgEt.setError("Scrie un mesaj de urgenta!");
            return;
        }
        if (numList.isEmpty()) {
            Toast.makeText(this, "Adauga", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("emergencyMessage", message);
        editor.putInt("contactCount", numList.size());
        for (int i = 0; i < numList.size(); i++) {
            editor.putString("contactName_" + i, nameList.get(i));
            editor.putString("contactNumber_" + i, numList.get(i));
        }
        editor.putBoolean("FirstRun", false);
        editor.commit();

        Toast.makeText(this, "Salvat", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void load() {
        String savedMessage = prefs.getString("emergencyMessage", "");
        if (!savedMessage.isEmpty()) {
            msgEt.setText(savedMessage);
        }

        int n = prefs.getInt("contactCount", 0);
        nameList.clear();
        numList.clear();
        for (int i = 0; i < n; i++) {
            nameList.add(prefs.getString("contactName_" + i, ""));
            numList.add(prefs.getString("contactNumber_" + i, ""));
        }
        showContacts();
    }
}
