package com.example.childmonitoringchildapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

// TODO: Import LocalBroadcastManager and BroadcastReceiver if updating UI from service
// import androidx.localbroadcastmanager.content.LocalBroadcastManager;
// import android.content.BroadcastReceiver;
// import android.content.IntentFilter;


public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 102;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 103;


    private static final String PREFS_NAME = "ChildAppPrefs";
    private static final String KEY_DEVICE_ID = "deviceId";
    // public static final String KEY_SERVICE_RUNNING_STATUS = "isServiceRunning"; // Example for robust status

    private EditText editTextDeviceId;
    private Button buttonToggleService;
    private TextView textViewServiceStatus;
    private TextView textViewLastLocation;

    private boolean isServiceRunning = false; // UI state tracking only, not perfectly reliable for actual service state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextDeviceId = findViewById(R.id.editTextDeviceId);
        buttonToggleService = findViewById(R.id.buttonToggleService);
        textViewServiceStatus = findViewById(R.id.textViewServiceStatus);
        textViewLastLocation = findViewById(R.id.textViewLastLocation);

        loadDeviceId();
        // TODO: Implement a more robust way to check actual service running state on startup.
        // This could involve the service writing its state to SharedPreferences or using a static flag.
        // For now, 'isServiceRunning' only reflects UI interactions within this activity instance.
        updateUIBasedOnServiceState();
        requestNotificationPermissionIfNeeded(); // Request notification permission

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

    // TODO: Implement BroadcastReceiver for service status and location updates from service.
    // @Override
    // protected void onResume() { super.onResume(); /* Register receiver */ }
    // @Override
    // protected void onPause() { super.onPause(); /* Unregister receiver */ }


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
        saveDeviceId(deviceId);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            checkBackgroundLocationAndStartService();
        }
    }

    private void checkBackgroundLocationAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Show rationale before requesting background location
                new AlertDialog.Builder(this)
                        .setTitle("Background Location Permission")
                        .setMessage("This app requires background location access to track location even when the app is closed or not in use. Please grant 'Allow all the time'.")
                        .setPositiveButton("Grant Permission", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            Toast.makeText(MainActivity.this, "Background location access denied. Reporting may be limited.", Toast.LENGTH_LONG).show();
                            startLocationService(); // Start with foreground access only if background is denied here
                        })
                        .show();
            } else {
                startLocationService(); // Both fine and background granted
            }
        } else {
            startLocationService(); // Pre-Android Q, fine location is enough
        }
    }


    private void startLocationService() {
        String deviceId = editTextDeviceId.getText().toString().trim();
        Intent serviceIntent = new Intent(this, LocationReportService.class);
        serviceIntent.putExtra("deviceId", deviceId);
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
            editTextDeviceId.setEnabled(false);
        } else {
            buttonToggleService.setText(getString(R.string.start_location_reporting));
            textViewServiceStatus.setText(getString(R.string.service_status_stopped));
            editTextDeviceId.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkBackgroundLocationAndStartService();
                } else {
                    Toast.makeText(this, getString(R.string.location_permission_denied_toast), Toast.LENGTH_SHORT).show();
                }
                break;
            case BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService();
                } else {
                    Toast.makeText(this, "Background location access denied. Reporting may be limited when app is not in use.", Toast.LENGTH_LONG).show();
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startLocationService(); // Start with foreground permission if background denied
                    }
                }
                break;
            case NOTIFICATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notification permission denied. You may not receive important updates.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU is API 33
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Show rationale if needed, then request
                // For simplicity, directly requesting. A real app should show rationale.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }
}
