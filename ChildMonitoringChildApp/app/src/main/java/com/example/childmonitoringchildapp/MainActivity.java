package com.example.childmonitoringchildapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final String PREFS_NAME = "ChildAppPrefs";
    private static final String KEY_DEVICE_ID = "deviceId";

    private EditText editTextDeviceId;
    private Button buttonToggleService;
    private TextView textViewServiceStatus;
    private TextView textViewLastLocation; // For displaying last sent location (optional debug)

    private boolean isServiceRunning = false; // Track service state (needs to be more robust, e.g. check service itself)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextDeviceId = findViewById(R.id.editTextDeviceId);
        buttonToggleService = findViewById(R.id.buttonToggleService);
        textViewServiceStatus = findViewById(R.id.textViewServiceStatus);
        textViewLastLocation = findViewById(R.id.textViewLastLocation);

        loadDeviceId();
        // TODO: Check actual service running state more reliably (e.g., static flag in service, or ActivityManager)
        updateUIBasedOnServiceState();

        buttonToggleService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning) {
                    stopLocationService();
                } else {
                    startLocationServiceWithPermissionsCheck();
                }
            }
        });
    }

    private void loadDeviceId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String deviceId = prefs.getString(KEY_DEVICE_ID, "");
        editTextDeviceId.setText(deviceId);
    }

    private void saveDeviceId(String deviceId) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_DEVICE_ID, deviceId);
        editor.apply();
    }

    private void startLocationServiceWithPermissionsCheck() {
        String deviceId = editTextDeviceId.getText().toString().trim();
        if (TextUtils.isEmpty(deviceId)) {
            Toast.makeText(this, getString(R.string.device_id_required_toast), Toast.LENGTH_SHORT).show();
            return;
        }
        saveDeviceId(deviceId); // Save it before starting service

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Permissions already granted
            // TODO: If targeting Android 10+, also check/request ACCESS_BACKGROUND_LOCATION if needed for continuous operation
            startLocationService();
        }
    }

    private void startLocationService() {
        String deviceId = editTextDeviceId.getText().toString().trim(); // Re-fetch in case it was just saved
        Intent serviceIntent = new Intent(this, LocationReportService.class);
        serviceIntent.putExtra("deviceId", deviceId);
        // Consider ContextCompat.startForegroundService for API 26+
        ContextCompat.startForegroundService(this, serviceIntent);
        isServiceRunning = true;
        updateUIBasedOnServiceState();
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationReportService.class);
        stopService(serviceIntent);
        isServiceRunning = false;
        updateUIBasedOnServiceState();
    }

    private void updateUIBasedOnServiceState() {
        if (isServiceRunning) {
            buttonToggleService.setText(getString(R.string.stop_location_reporting));
            textViewServiceStatus.setText(getString(R.string.service_status_running));
            editTextDeviceId.setEnabled(false); // Disable editing while service is running
        } else {
            buttonToggleService.setText(getString(R.string.start_location_reporting));
            textViewServiceStatus.setText(getString(R.string.service_status_stopped));
            editTextDeviceId.setEnabled(true);
        }
        // TODO: Update textViewLastLocation based on actual data from service (e.g., via BroadcastReceiver or LiveData)
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                // TODO: If targeting Android 10+, also check/request ACCESS_BACKGROUND_LOCATION if needed
                startLocationService();
            } else {
                Toast.makeText(this, getString(R.string.location_permission_denied_toast), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // TODO: Add a BroadcastReceiver to get status/location updates from the service
    // to update textViewLastLocation and potentially isServiceRunning more reliably.
}
