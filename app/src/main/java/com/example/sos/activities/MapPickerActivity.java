package com.example.sos.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;

import com.example.sos.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_X = "extra_lat";
    public static final String EXTRA_Y = "extra_lng";
    public static final String EXTRA_R = "extra_radius";

    private static final int MIN_RAD = 50;
    private static final int MAX_RAD = 300;
    private static final int DEF_RAD = 100;

    private GoogleMap map;
    private FusedLocationProviderClient locCli;
    private Marker mark;
    private Circle circ;
    private LatLng sel;
    private Button btnOk;
    private Slider slRad;
    private TextView tvRad;
    private SearchView svFind;
    private ExecutorService geoEx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        btnOk = findViewById(R.id.btnConfirmZone);
        btnOk.setEnabled(false);
        btnOk.setOnClickListener(v -> sendPick());

        tvRad = findViewById(R.id.tvRadiusValue);
        slRad = findViewById(R.id.sliderRadius);
        slRad.setValueFrom(MIN_RAD);
        slRad.setValueTo(MAX_RAD);
        slRad.setStepSize(1f);
        slRad.setValue(DEF_RAD);
        setRadText(DEF_RAD);
        
        slRad.addOnChangeListener((slider, value, fromUser) -> {
            setRadText(Math.round(value));
            if (circ != null) {
                circ.setRadius(value);
            }
        });

        svFind = findViewById(R.id.searchLocation);
        svFind.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                svFind.clearFocus();
                findPlace(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        geoEx = Executors.newSingleThreadExecutor();
        locCli = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Harta lipsa", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnMapClickListener(this::setPick);
        map.setOnMapLongClickListener(this::setPick);

        LatLng defaultLocation = new LatLng(46.0, 25.0);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 6f));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                map.setMyLocationEnabled(true);
            } catch (SecurityException ignored) {
            }

            locCli.getLastLocation().addOnSuccessListener(location -> {
                if (location == null) return;
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15f));
            });
        }
    }

    private void setPick(LatLng latLng) {
        sel = latLng;
        float curRad = slRad.getValue();

        if (mark == null) {
            mark = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Zona selectata"));
        } else {
            mark.setPosition(latLng);
        }

        if (circ == null) {
            circ = map.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(curRad)
                    .strokeWidth(2)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255)));
        } else {
            circ.setCenter(latLng);
            circ.setRadius(curRad);
        }

        btnOk.setEnabled(true);
    }

    private void sendPick() {
        if (sel == null) {
            Toast.makeText(this, "Alege loc", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_X, sel.latitude);
        data.putExtra(EXTRA_Y, sel.longitude);
        data.putExtra(EXTRA_R, Math.round(slRad.getValue()));
        setResult(RESULT_OK, data);
        finish();
    }

    private void setRadText(int rad) {
        tvRad.setText("Raza: " + rad + "m");
    }

    private void findPlace(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        if (map == null) {
            Toast.makeText(this, "Harta nu", Toast.LENGTH_SHORT).show();
            return;
        }

        geoEx.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> results = geocoder.getFromLocationName(query, 1);
                if (results == null || results.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Loc lipsa", Toast.LENGTH_SHORT).show());
                    return;
                }

                Address address = results.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                runOnUiThread(() -> {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                    setPick(latLng);
                });
            } catch (IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Eroare", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geoEx != null) {
            geoEx.shutdownNow();
        }
    }
}
