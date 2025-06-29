package com.example.childmonitoringchildapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat; // Added for SwitchCompat
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton; // Added for Switch
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ChildAppMainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 102;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 103;
    private static final int CALL_LOG_PERMISSION_REQUEST_CODE = 104;
    private static final int SMS_PERMISSION_REQUEST_CODE = 105;
    private static final int CONTACTS_PERMISSION_REQUEST_CODE = 106; // New request code

    private static final String PREFS_NAME = "ChildAppPrefs";
    private static final String KEY_DEVICE_ID = "pairedChildDeviceId";
    private static final String KEY_IS_PAIRED = "isPaired";
    private static final String KEY_PAIRED_CHILD_NAME = "pairedChildName";
    private static final String KEY_UNIQUE_ID = "childDeviceUniqueId";
    // Using LocationReportService.KEY_CALL_LOG_MONITORING_ENABLED to ensure consistency
    // private static final String KEY_CALL_LOG_MONITORING_ENABLED_PREF = LocationReportService.KEY_CALL_LOG_MONITORING_ENABLED;


    private static final String VERIFY_PAIRING_CODE_FUNCTION_URL = "YOUR_CLOUD_FUNCTION_URL_HERE/verifyPairingCode"; // TODO: User must set this

    private EditText editTextPairingCode;
    private Button buttonAction;
    private TextView textViewServiceStatus;
    private TextView textViewLastLocation;
    private SwitchCompat switchCallLogMonitoring;
    private SwitchCompat switchSmsLogMonitoring;
    private SwitchCompat switchContactsSync; // New Contacts Switch

    private boolean isPaired = false;
    private String pairedChildDeviceId = null;
    private String pairedChildName = null;
    private boolean isLocationServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextPairingCode = findViewById(R.id.editTextPairingCode);
        buttonAction = findViewById(R.id.buttonAction);
        textViewServiceStatus = findViewById(R.id.textViewServiceStatus);
        textViewLastLocation = findViewById(R.id.textViewLastLocation);
        switchCallLogMonitoring = findViewById(R.id.switchCallLogMonitoring);
        switchSmsLogMonitoring = findViewById(R.id.switchSmsLogMonitoring);
        switchContactsSync = findViewById(R.id.switchContactsSync); // Initialize new switch

        loadPairingState();
        loadCallLogSwitchState();
        loadSmsLogSwitchState();
        loadContactsSyncSwitchState(); // Load Contacts switch state
        updateUIAfterPairingStateChange();
        requestNotificationPermissionIfNeeded();
        // Permissions for call/SMS/contacts logs are requested when their respective switches are toggled ON.

        buttonAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPaired) {
                    if (isLocationServiceRunning) {
                        stopLocationService();
                    } else {
                        startLocationServiceWithPermissionsCheck();
                    }
                } else {
                    attemptPairing();
                }
            }
        });

        switchCallLogMonitoring.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Request permission only if enabling and not already granted
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                        requestCallLogPermission();
                    } else {
                        saveCallLogMonitoringState(true);
                        Toast.makeText(MainActivity.this, getString(R.string.call_log_monitoring_enabled_toast), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    saveCallLogMonitoringState(false);
                    Toast.makeText(MainActivity.this, getString(R.string.call_log_monitoring_disabled_toast), Toast.LENGTH_SHORT).show();
                }
            }
        });

        switchSmsLogMonitoring.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                        requestSmsPermission();
                    } else {
                        saveSmsMonitoringState(true);
                        Toast.makeText(MainActivity.this, getString(R.string.sms_log_monitoring_enabled_toast), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    saveSmsMonitoringState(false);
                    Toast.makeText(MainActivity.this, getString(R.string.sms_log_monitoring_disabled_toast), Toast.LENGTH_SHORT).show();
                }
            }
        });

        switchContactsSync.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                        requestContactsPermission();
                    } else {
                        saveContactsSyncState(true);
                        Toast.makeText(MainActivity.this, getString(R.string.contacts_sync_enabled_toast), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    saveContactsSyncState(false);
                    Toast.makeText(MainActivity.this, getString(R.string.contacts_sync_disabled_toast), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadCallLogSwitchState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(LocationReportService.KEY_CALL_LOG_MONITORING_ENABLED, false);
        switchCallLogMonitoring.setChecked(enabled);
        switchCallLogMonitoring.setVisibility(isPaired ? View.VISIBLE : View.GONE);
    }

    private void saveCallLogMonitoringState(boolean isEnabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(LocationReportService.KEY_CALL_LOG_MONITORING_ENABLED, isEnabled).apply();
    }

    private void loadSmsLogSwitchState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(LocationReportService.KEY_SMS_LOG_MONITORING_ENABLED, false);
        switchSmsLogMonitoring.setChecked(enabled);
        switchSmsLogMonitoring.setVisibility(isPaired ? View.VISIBLE : View.GONE);
    }

    private void saveSmsMonitoringState(boolean isEnabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(LocationReportService.KEY_SMS_LOG_MONITORING_ENABLED, isEnabled).apply();
    }

    private void loadContactsSyncSwitchState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(LocationReportService.KEY_CONTACTS_SYNC_ENABLED, false);
        switchContactsSync.setChecked(enabled);
        switchContactsSync.setVisibility(isPaired ? View.VISIBLE : View.GONE);
    }

    private void saveContactsSyncState(boolean isEnabled) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(LocationReportService.KEY_CONTACTS_SYNC_ENABLED, isEnabled).apply();
    }


    private void loadPairingState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isPaired = prefs.getBoolean(KEY_IS_PAIRED, false);
        if (isPaired) {
            pairedChildDeviceId = prefs.getString(KEY_DEVICE_ID, null);
            pairedChildName = prefs.getString(KEY_PAIRED_CHILD_NAME, "Paired Child");
        }
    }

    private void savePairingState(boolean paired, String deviceId, String childName) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_PAIRED, paired);
        if (paired && deviceId != null) {
            editor.putString(KEY_DEVICE_ID, deviceId);
            editor.putString(KEY_PAIRED_CHILD_NAME, childName);
            this.pairedChildDeviceId = deviceId;
            this.pairedChildName = childName;
        } else {
            editor.remove(KEY_DEVICE_ID);
            editor.remove(KEY_PAIRED_CHILD_NAME);
            this.pairedChildDeviceId = null;
            this.pairedChildName = null;
        }
        this.isPaired = paired;
        editor.apply();
    }

    private String getUniqueDeviceIdentifier() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uniqueId = prefs.getString(KEY_UNIQUE_ID, null);
        if (uniqueId == null) {
            String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId != null && !androidId.isEmpty() && !"9774d56d682e549c".equals(androidId)) {
                uniqueId = "ANDID_" + androidId;
            } else {
                uniqueId = "UUID_" + UUID.randomUUID().toString();
            }
            prefs.edit().putString(KEY_UNIQUE_ID, uniqueId).apply();
        }
        return uniqueId;
    }

    private void attemptPairing() {
        String pairingCode = editTextPairingCode.getText().toString().trim().toUpperCase();
        if (TextUtils.isEmpty(pairingCode)) {
            Toast.makeText(this, getString(R.string.pairing_code_required_toast), Toast.LENGTH_SHORT).show();
            return;
        }

        if (VERIFY_PAIRING_CODE_FUNCTION_URL.equals("YOUR_CLOUD_FUNCTION_URL_HERE/verifyPairingCode")) {
             Toast.makeText(this, "Error: Cloud Function URL not configured.", Toast.LENGTH_LONG).show();
             Log.e(TAG, "VERIFY_PAIRING_CODE_FUNCTION_URL is not set in MainActivity.java");
             return;
        }

        String childDeviceUniqueId = getUniqueDeviceIdentifier();
        textViewServiceStatus.setText(getString(R.string.status_pairing_in_progress));
        buttonAction.setEnabled(false);
        editTextPairingCode.setEnabled(false);

        new VerifyCodeTask().execute(pairingCode, childDeviceUniqueId);
    }

    private class VerifyCodeTask extends AsyncTask<String, Void, String> {
        // ... (doInBackground remains the same) ...
        @Override
        protected String doInBackground(String... params) {
            String pairingCode = params[0];
            String childDeviceUniqueId = params[1];
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(VERIFY_PAIRING_CODE_FUNCTION_URL);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(15000); // 15 seconds
                urlConnection.setReadTimeout(15000); // 15 seconds


                JSONObject jsonParam = new JSONObject();
                jsonParam.put("pairingCode", pairingCode);
                jsonParam.put("childDeviceUniqueId", childDeviceUniqueId);

                OutputStream os = urlConnection.getOutputStream();
                byte[] input = jsonParam.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
                os.close();

                int responseCode = urlConnection.getResponseCode();
                StringBuilder response = new StringBuilder();
                BufferedReader br;

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), "utf-8"));
                }
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                br.close();
                return response.toString();

            } catch (Exception e) {
                Log.e(TAG, "Error during pairing HTTP request", e);
                return "{\"success\":false, \"error\":\"Network error or exception: " + e.getMessage() + "\"}";
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject jsonResponse = new JSONObject(result);
                boolean success = jsonResponse.optBoolean("success", false);
                if (success) {
                    String receivedDeviceId = jsonResponse.getString("childDeviceId");
                    String receivedChildName = jsonResponse.optString("childName", "Paired Child");
                    savePairingState(true, receivedDeviceId, receivedChildName);
                    // textViewServiceStatus will be updated by updateUIAfterPairingStateChange
                    triggerFCMTokenRefreshAndUpload();
                } else {
                    String errorMsg = jsonResponse.optString("error", "Unknown pairing error.");
                    savePairingState(false, null, null); // Ensure isPaired is false
                    textViewServiceStatus.setText(getString(R.string.status_pairing_failed, errorMsg));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing pairing response JSON", e);
                savePairingState(false, null, null); // Ensure isPaired is false
                textViewServiceStatus.setText(getString(R.string.status_pairing_failed, "Invalid response from server."));
            }
            updateUIAfterPairingStateChange();
        }
    }

    private void triggerFCMTokenRefreshAndUpload() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }
                String token = task.getResult();
                Log.d(TAG, "FCM Token fetched for upload: " + token);
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String currentDeviceId = prefs.getString(KEY_DEVICE_ID, null);
                if (currentDeviceId != null && token != null) {
                    MyFirebaseMessagingService.staticSendRegistrationToServer(getApplicationContext(), token, currentDeviceId);
                }
            });
    }


    private void updateUIAfterPairingStateChange() {
        loadCallLogSwitchState();
        loadSmsLogSwitchState();
        loadContactsSyncSwitchState(); // Also update Contacts switch visibility
        if (isPaired) {
            editTextPairingCode.setVisibility(View.GONE);
            updateUIBasedOnLocationServiceState();
        } else {
            editTextPairingCode.setVisibility(View.VISIBLE);
            editTextPairingCode.setEnabled(true);
            buttonAction.setText(getString(R.string.pair_device_button));
            buttonAction.setEnabled(true);
            textViewServiceStatus.setText(getString(R.string.status_waiting_for_pairing));
            isLocationServiceRunning = false;
            switchCallLogMonitoring.setVisibility(View.GONE);
            switchSmsLogMonitoring.setVisibility(View.GONE);
            switchContactsSync.setVisibility(View.GONE); // Hide Contacts switch if not paired
        }
    }


    private void startLocationServiceWithPermissionsCheck() {
        if (!isPaired || pairedChildDeviceId == null) {
            Toast.makeText(this, "Device not paired. Please pair first.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.background_location_permission_rationale_title))
                        .setMessage(getString(R.string.background_location_permission_rationale_message))
                        .setPositiveButton(getString(R.string.button_grant_permission), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton(getString(R.string.button_cancel), (dialog, which) -> {
                            Toast.makeText(MainActivity.this, getString(R.string.background_location_denied_message), Toast.LENGTH_LONG).show();
                            startLocationService();
                        })
                        .show();
            } else {
                startLocationService();
            }
        } else {
            startLocationService();
        }
    }

    private void startLocationService() {
        if (pairedChildDeviceId == null) {
            Log.e(TAG, "Attempted to start service, but pairedChildDeviceId is null.");
            isPaired = false;
            updateUIAfterPairingStateChange();
            return;
        }
        Intent serviceIntent = new Intent(this, LocationReportService.class);
        serviceIntent.putExtra("deviceId", pairedChildDeviceId);
        ContextCompat.startForegroundService(this, serviceIntent);
        isLocationServiceRunning = true;
        updateUIBasedOnLocationServiceState();
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationReportService.class);
        stopService(serviceIntent);
        isLocationServiceRunning = false;
        updateUIBasedOnLocationServiceState();
    }

    private void updateUIBasedOnLocationServiceState() {
        if (!isPaired) {
            buttonAction.setText(getString(R.string.pair_device_button));
            buttonAction.setEnabled(true);
            editTextPairingCode.setVisibility(View.VISIBLE);
            editTextPairingCode.setEnabled(true);
            textViewServiceStatus.setText(getString(R.string.status_waiting_for_pairing));
            switchCallLogMonitoring.setVisibility(View.GONE); // Hide if not paired
            return;
        }

        editTextPairingCode.setVisibility(View.GONE);
        buttonAction.setEnabled(true);
        switchCallLogMonitoring.setVisibility(View.VISIBLE); // Show if paired

        String baseStatus = getString(R.string.status_pairing_successful, pairedChildName, pairedChildDeviceId);
        if (isLocationServiceRunning) {
            buttonAction.setText(getString(R.string.stop_location_reporting));
            textViewServiceStatus.setText(baseStatus + "\n" + getString(R.string.service_status_running));
        } else {
            buttonAction.setText(getString(R.string.start_location_reporting));
             textViewServiceStatus.setText(baseStatus + "\n" + getString(R.string.service_status_stopped));
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
                    Toast.makeText(this, getString(R.string.background_location_denied_message), Toast.LENGTH_LONG).show();
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        startLocationService();
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
            case CALL_LOG_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Call Log permission granted.", Toast.LENGTH_SHORT).show();
                    saveCallLogMonitoringState(true); // Save state as enabled since permission granted
                    switchCallLogMonitoring.setChecked(true); // Reflect in UI
                } else {
                    Toast.makeText(this, getString(R.string.call_log_permission_denied_toast), Toast.LENGTH_LONG).show();
                    saveCallLogMonitoringState(false); // Save state as disabled
                    switchCallLogMonitoring.setChecked(false); // Reflect in UI
                }
                break;
            case SMS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.sms_permission_granted_toast), Toast.LENGTH_SHORT).show();
                    saveSmsMonitoringState(true); // Save state as enabled
                    switchSmsLogMonitoring.setChecked(true); // Reflect in UI
                } else {
                    Toast.makeText(this, getString(R.string.sms_permission_denied_toast), Toast.LENGTH_LONG).show();
                    saveSmsMonitoringState(false); // Save state as disabled
                    switchSmsLogMonitoring.setChecked(false); // Reflect in UI
                }
                break;
            case CONTACTS_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(R.string.contacts_permission_granted_toast), Toast.LENGTH_SHORT).show();
                    saveContactsSyncState(true); // Save state as enabled
                    switchContactsSync.setChecked(true); // Reflect in UI
                } else {
                    Toast.makeText(this, getString(R.string.contacts_permission_denied_toast), Toast.LENGTH_LONG).show();
                    saveContactsSyncState(false); // Save state as disabled
                    switchContactsSync.setChecked(false); // Reflect in UI
                }
                break;
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    // Renamed from requestCallLogPermissionIfNeeded to be more explicit for the switch
    private void requestCallLogPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) !=
                PackageManager.PERMISSION_GRANTED) {
            // TODO: Show rationale dialog if not shown before or if user denied previously.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALL_LOG},
                    CALL_LOG_PERMISSION_REQUEST_CODE);
        }
        // If already granted, the switch listener will handle saving state.
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            // TODO: Show rationale dialog if not shown before or if user denied previously.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
        }
        // If already granted, the switch listener (when implemented for SMS) will handle saving state.
    }

    private void requestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
                PackageManager.PERMISSION_GRANTED) {
            // TODO: Show rationale dialog if not shown before or if user denied previously.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_PERMISSION_REQUEST_CODE);
        }
        // If already granted, the switch listener (when implemented for Contacts) will handle saving state.
    }
}
