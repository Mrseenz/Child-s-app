package com.example.childmonitoringchildapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging; // For FCM token refresh

import org.json.JSONObject; // For HTTP Call
import java.io.BufferedReader; // For HTTP Call
import java.io.InputStreamReader; // For HTTP Call
import java.io.OutputStream; // For HTTP Call
import java.net.HttpURLConnection; // For HTTP Call
import java.net.URL; // For HTTP Call
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ChildAppMainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 102;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 103;

    private static final String PREFS_NAME = "ChildAppPrefs";
    private static final String KEY_DEVICE_ID = "pairedChildDeviceId"; // Stored childDeviceId after pairing
    private static final String KEY_IS_PAIRED = "isPaired";
    private static final String KEY_PAIRED_CHILD_NAME = "pairedChildName";
    private static final String KEY_UNIQUE_ID = "childDeviceUniqueId"; // Persisted UUID as fallback

    // TODO: Replace with your actual deployed Cloud Function URL
    private static final String VERIFY_PAIRING_CODE_FUNCTION_URL = "YOUR_CLOUD_FUNCTION_URL_HERE/verifyPairingCode";

    private EditText editTextPairingCode;
    private Button buttonAction;
    private TextView textViewServiceStatus;
    private TextView textViewLastLocation; // Still useful for debugging location service

    private boolean isPaired = false;
    private String pairedChildDeviceId = null;
    private String pairedChildName = null;
    private boolean isLocationServiceRunning = false; // Tracks location service state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextPairingCode = findViewById(R.id.editTextPairingCode);
        buttonAction = findViewById(R.id.buttonAction);
        textViewServiceStatus = findViewById(R.id.textViewServiceStatus);
        textViewLastLocation = findViewById(R.id.textViewLastLocation); // Keep for location debug

        loadPairingState();
        updateUIAfterPairingStateChange(); // Sets initial UI based on isPaired
        requestNotificationPermissionIfNeeded();

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
        // TODO: Implement more robust check for actual service running state on startup/resume.
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
            this.pairedChildDeviceId = deviceId; // Update instance variable
            this.pairedChildName = childName;
        } else {
            editor.remove(KEY_DEVICE_ID);
            editor.remove(KEY_PAIRED_CHILD_NAME);
            this.pairedChildDeviceId = null;
            this.pairedChildName = null;
        }
        this.isPaired = paired; // Update instance variable
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
                    textViewServiceStatus.setText(getString(R.string.status_pairing_successful, receivedChildName, receivedDeviceId));
                    triggerFCMTokenRefreshAndUpload();
                } else {
                    String errorMsg = jsonResponse.optString("error", "Unknown pairing error.");
                    savePairingState(false, null, null);
                    textViewServiceStatus.setText(getString(R.string.status_pairing_failed, errorMsg));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing pairing response JSON", e);
                savePairingState(false, null, null);
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
                // Manually call the method in MyFirebaseMessagingService or a utility class
                // This assumes MyFirebaseMessagingService has a static method or you have another way
                // For simplicity, let's assume a utility method or direct call if context is available
                // MyFirebaseMessagingService.sendTokenToServer(getApplicationContext(), token); // Example
                // For now, just log. Actual upload is in MyFirebaseMessagingService.onNewToken
                // which should also read the LATEST deviceId from prefs.
                // The service itself will handle onNewToken. If token is same, it won't re-upload usually.
                // If deviceId was missing before pairing, this ensures it's available now.
                // A more direct way to ensure upload:
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String currentDeviceId = prefs.getString(KEY_DEVICE_ID, null);
                if (currentDeviceId != null && token != null) {
                    MyFirebaseMessagingService.staticSendRegistrationToServer(getApplicationContext(), token, currentDeviceId);
                }
            });
    }


    private void updateUIAfterPairingStateChange() {
        if (isPaired) {
            editTextPairingCode.setVisibility(View.GONE);
            // textViewServiceStatus is updated by pairing success/failure or service state
            // buttonAction text and enabled state will be managed by updateUIBasedOnLocationServiceState
            if (pairedChildDeviceId != null) {
                 // Show the child's name/ID if available
                textViewServiceStatus.setText(getString(R.string.status_pairing_successful, pairedChildName, pairedChildDeviceId) + "\n" + (isLocationServiceRunning ? getString(R.string.service_status_running) : getString(R.string.service_status_stopped)));
            }
            updateUIBasedOnLocationServiceState(); // Manages buttonAction and specific service status text
        } else {
            editTextPairingCode.setVisibility(View.VISIBLE);
            editTextPairingCode.setEnabled(true);
            buttonAction.setText(getString(R.string.pair_device_button));
            buttonAction.setEnabled(true);
            textViewServiceStatus.setText(getString(R.string.status_waiting_for_pairing));
            isLocationServiceRunning = false; // Reset service status if unpairing or initial state
        }
    }


    private void startLocationServiceWithPermissionsCheck() {
        if (!isPaired || pairedChildDeviceId == null) {
            Toast.makeText(this, "Device not paired. Please pair first.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Device ID for service is now pairedChildDeviceId, no need to read from EditText

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
        if (pairedChildDeviceId == null) { // Safeguard
            Log.e(TAG, "Attempted to start service, but pairedChildDeviceId is null.");
            isPaired = false; // Something went wrong with pairing state
            updateUIAfterPairingStateChange();
            return;
        }
        Intent serviceIntent = new Intent(this, LocationReportService.class);
        serviceIntent.putExtra("deviceId", pairedChildDeviceId); // Use the paired device ID
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

    // Renamed from updateUIBasedOnServiceState to be more specific
    private void updateUIBasedOnLocationServiceState() {
        if (!isPaired) { // If not paired, UI is handled by updateUIAfterPairingStateChange
            buttonAction.setText(getString(R.string.pair_device_button));
            buttonAction.setEnabled(true);
            editTextPairingCode.setVisibility(View.VISIBLE);
            editTextPairingCode.setEnabled(true);
            textViewServiceStatus.setText(getString(R.string.status_waiting_for_pairing));
            return;
        }

        // Paired, so manage service start/stop UI
        editTextPairingCode.setVisibility(View.GONE); // Hide pairing code input
        buttonAction.setEnabled(true); // Always enabled if paired

        if (isLocationServiceRunning) {
            buttonAction.setText(getString(R.string.stop_location_reporting));
            textViewServiceStatus.setText(getString(R.string.status_pairing_successful, pairedChildName, pairedChildDeviceId) + "\n" + getString(R.string.service_status_running));
        } else {
            buttonAction.setText(getString(R.string.start_location_reporting));
             textViewServiceStatus.setText(getString(R.string.status_pairing_successful, pairedChildName, pairedChildDeviceId) + "\n" + getString(R.string.service_status_stopped));
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
}
