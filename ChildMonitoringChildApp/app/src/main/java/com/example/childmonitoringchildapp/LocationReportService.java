package com.example.childmonitoringchildapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat; // Added ContextCompat
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationReportService extends Service {

    private static final String TAG = "LocationReportService";
    private static final String NOTIFICATION_CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 12345;
    private static final long LOCATION_UPDATE_INTERVAL = 60000; // 60 seconds
    private static final long FASTEST_LOCATION_UPDATE_INTERVAL = 30000; // 30 seconds

    // For Call Log monitoring
    private static final String PREFS_CHILD_APP = "ChildAppPrefs"; // Matches MainActivity
    private static final String KEY_LAST_CALL_LOG_UPLOAD_TIME = "lastCallLogUploadTimeMs";
    public static final String KEY_CALL_LOG_MONITORING_ENABLED = "callLogMonitoringEnabled";
    private static final long CALL_LOG_UPLOAD_INTERVAL_MS = 1 * 60 * 60 * 1000;

    // For SMS Log monitoring
    private static final String KEY_LAST_SMS_LOG_UPLOAD_TIME = "lastSmsLogUploadTimeMs";
    public static final String KEY_SMS_LOG_MONITORING_ENABLED = "smsLogMonitoringEnabled";
    private static final long SMS_LOG_UPLOAD_INTERVAL_MS = 1 * 60 * 60 * 1000;

    // For Contacts Sync
    public static final String KEY_CONTACTS_SYNC_ENABLED = "contactsSyncEnabled";
    private static final long CONTACTS_SYNC_INTERVAL_MS = 24 * 60 * 60 * 1000; // Daily
    // private static final long CONTACTS_SYNC_INTERVAL_MS = 3 * 60 * 1000; // Shorter for testing: 3 minutes


    private Handler periodicTaskHandler;
    private Runnable callLogUploadRunnable;
    private Runnable smsLogUploadRunnable;
    private Runnable contactsSyncRunnable;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private String deviceId;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) { return; }
                for (Location location : locationResult.getLocations()) {
                    if (location != null && LocationReportService.this.deviceId != null) {
                        Log.d(TAG, "Location received: " + location.getLatitude() + ", " + location.getLongitude() + " for deviceId: " + LocationReportService.this.deviceId);
                        sendLocationToFirestore(location);
                    }
                }
            }
        };

        periodicTaskHandler = new Handler(Looper.getMainLooper());
        callLogUploadRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Runnable: Attempting to upload call logs for deviceId: " + LocationReportService.this.deviceId);
                uploadCallLogs();
                periodicTaskHandler.postDelayed(this, CALL_LOG_UPLOAD_INTERVAL_MS);
            }
        };

        smsLogUploadRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Runnable: Attempting to upload SMS logs for deviceId: " + LocationReportService.this.deviceId);
                uploadSmsLogs();
                periodicTaskHandler.postDelayed(this, SMS_LOG_UPLOAD_INTERVAL_MS);
            }
        };

        contactsSyncRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Runnable: Attempting to sync contacts for deviceId: " + LocationReportService.this.deviceId);
                uploadContactsSnapshot();
                periodicTaskHandler.postDelayed(this, CONTACTS_SYNC_INTERVAL_MS);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand received");

        if (intent != null && intent.hasExtra("deviceId")) {
            this.deviceId = intent.getStringExtra("deviceId");
            Log.d(TAG, "Service started with deviceId from intent: " + this.deviceId);
        } else if (this.deviceId == null) {
            SharedPreferences prefs = getSharedPreferences(PREFS_CHILD_APP, MODE_PRIVATE);
            this.deviceId = prefs.getString("pairedChildDeviceId", null);
            if (this.deviceId != null) {
                Log.d(TAG, "Service restarted, loaded deviceId from SharedPreferences: " + this.deviceId);
            } else {
                Log.e(TAG, "Service restarted or started without deviceId, stopping.");
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        startForeground(NOTIFICATION_ID, createNotification());
        startLocationUpdates();

        periodicTaskHandler.removeCallbacks(callLogUploadRunnable);
        periodicTaskHandler.postDelayed(callLogUploadRunnable, CALL_LOG_UPLOAD_INTERVAL_MS);
        Log.d(TAG, "Call log upload task scheduled.");

        periodicTaskHandler.removeCallbacks(smsLogUploadRunnable);
        periodicTaskHandler.postDelayed(smsLogUploadRunnable, SMS_LOG_UPLOAD_INTERVAL_MS);
        Log.d(TAG, "SMS log upload task scheduled.");

        periodicTaskHandler.removeCallbacks(contactsSyncRunnable);
        periodicTaskHandler.postDelayed(contactsSyncRunnable, CONTACTS_SYNC_INTERVAL_MS);
        Log.d(TAG, "Contacts sync task scheduled.");

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        String contentText = getString(R.string.foreground_service_notification_text) +
                             (this.deviceId != null ? " (ID: " + this.deviceId + ")" : "");
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.foreground_service_notification_title))
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
    }

    private void startLocationUpdates() {
        if (this.deviceId == null || this.deviceId.isEmpty()) {
            Log.e(TAG, "Cannot start location updates: deviceId is null or empty.");
            stopSelf();
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted to service. Stopping self.");
            stopSelf();
            return;
        }
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "Location updates requested for deviceId: " + this.deviceId);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException on requesting location updates for " + this.deviceId + ": " + e.getMessage());
            stopSelf();
        }
    }

    private void sendLocationToFirestore(Location location) {
        if (this.deviceId == null || this.deviceId.isEmpty()) {
            Log.w(TAG, "No deviceId, cannot send location to Firestore for: " + location.toString());
            return;
        }
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("locations").document(this.deviceId)
                .set(locationData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location successfully written for " + LocationReportService.this.deviceId))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing location for " + LocationReportService.this.deviceId, e));
    }

    private void uploadCallLogs() {
        if (this.deviceId == null || this.deviceId.isEmpty()) {
            Log.w(TAG, "uploadCallLogs: deviceId is null or empty, skipping call log upload.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_CHILD_APP, MODE_PRIVATE);
        boolean callLogMonitoringEnabled = prefs.getBoolean(KEY_CALL_LOG_MONITORING_ENABLED, false);

        if (!callLogMonitoringEnabled) {
            Log.d(TAG, "uploadCallLogs: Call Log Monitoring is disabled in SharedPreferences for deviceId: " + this.deviceId);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "uploadCallLogs: READ_CALL_LOG permission not granted. Cannot fetch call logs for deviceId: " + this.deviceId);
            return;
        }

        long lastFetchTime = prefs.getLong(KEY_LAST_CALL_LOG_UPLOAD_TIME + "_" + this.deviceId, 0);
        Log.d(TAG, "uploadCallLogs: Fetching call logs for " + this.deviceId + " after timestamp: " + lastFetchTime);

        List<CallLogEntry> callLogs = CallLogUtil.getCallLogsAfter(this, lastFetchTime);

        if (callLogs.isEmpty()) {
            Log.d(TAG, "uploadCallLogs: No new call logs to upload for deviceId: " + this.deviceId);
            return;
        }

        long newestCallTimestampInBatch = lastFetchTime;

        for (CallLogEntry entry : callLogs) {
            db.collection("locations").document(this.deviceId)
              .collection("callLogs")
              .add(entry)
              .addOnSuccessListener(documentReference -> {
                  Log.d(TAG, "Call log entry successfully written to Firestore: " + documentReference.getId() + " for deviceId: " + LocationReportService.this.deviceId);
              })
              .addOnFailureListener(e -> Log.w(TAG, "Error writing call log entry to Firestore for deviceId: " + LocationReportService.this.deviceId, e));

            if (entry.getCallDateMillis() > newestCallTimestampInBatch) {
                newestCallTimestampInBatch = entry.getCallDateMillis();
            }
        }

        if (newestCallTimestampInBatch > lastFetchTime) {
            prefs.edit().putLong(KEY_LAST_CALL_LOG_UPLOAD_TIME + "_" + this.deviceId, newestCallTimestampInBatch).apply();
            Log.d(TAG, "Updated lastCallLogUploadTimeMs for " + this.deviceId + " to: " + newestCallTimestampInBatch);
        }
    }

    private void uploadSmsLogs() {
        if (this.deviceId == null || this.deviceId.isEmpty()) {
            Log.w(TAG, "uploadSmsLogs: deviceId is null or empty, skipping SMS log upload.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_CHILD_APP, MODE_PRIVATE);
        boolean smsLogMonitoringEnabled = prefs.getBoolean(KEY_SMS_LOG_MONITORING_ENABLED, false);

        if (!smsLogMonitoringEnabled) {
            Log.d(TAG, "uploadSmsLogs: SMS Log Monitoring is disabled in SharedPreferences for deviceId: " + this.deviceId);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "uploadSmsLogs: READ_SMS permission not granted. Cannot fetch SMS logs for deviceId: " + this.deviceId);
            return;
        }

        long lastFetchTime = prefs.getLong(KEY_LAST_SMS_LOG_UPLOAD_TIME + "_" + this.deviceId, 0);
        Log.d(TAG, "uploadSmsLogs: Fetching SMS logs for " + this.deviceId + " after timestamp: " + lastFetchTime);

        List<SmsLogEntry> smsLogs = SmsLogUtil.getSmsLogsAfter(this, lastFetchTime);

        if (smsLogs.isEmpty()) {
            Log.d(TAG, "uploadSmsLogs: No new SMS logs to upload for deviceId: " + this.deviceId);
            return;
        }

        long newestSmsTimestampInBatch = lastFetchTime;

        for (SmsLogEntry entry : smsLogs) {
            db.collection("locations").document(this.deviceId)
              .collection("smsLogs")
              .add(entry)
              .addOnSuccessListener(documentReference -> {
                  Log.d(TAG, "SMS log entry successfully written to Firestore: " + documentReference.getId() + " for deviceId: " + LocationReportService.this.deviceId);
              })
              .addOnFailureListener(e -> Log.w(TAG, "Error writing SMS log entry to Firestore for deviceId: " + LocationReportService.this.deviceId, e));

            if (entry.getMessageDateMillis() > newestSmsTimestampInBatch) {
                newestSmsTimestampInBatch = entry.getMessageDateMillis();
            }
        }

        if (newestSmsTimestampInBatch > lastFetchTime) {
            prefs.edit().putLong(KEY_LAST_SMS_LOG_UPLOAD_TIME + "_" + this.deviceId, newestSmsTimestampInBatch).apply();
            Log.d(TAG, "Updated lastSmsLogUploadTimeMs for " + this.deviceId + " to: " + newestSmsTimestampInBatch);
        }
    }

    private void uploadContactsSnapshot() {
        if (this.deviceId == null || this.deviceId.isEmpty()) {
            Log.w(TAG, "uploadContactsSnapshot: deviceId is null or empty, skipping contacts sync.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_CHILD_APP, MODE_PRIVATE);
        boolean contactsSyncEnabled = prefs.getBoolean(KEY_CONTACTS_SYNC_ENABLED, false);

        if (!contactsSyncEnabled) {
            Log.d(TAG, "uploadContactsSnapshot: Contacts Sync is disabled in SharedPreferences for deviceId: " + this.deviceId);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "uploadContactsSnapshot: READ_CONTACTS permission not granted. Cannot fetch contacts for deviceId: " + this.deviceId);
            return;
        }

        Log.d(TAG, "uploadContactsSnapshot: Fetching all contacts for deviceId: " + this.deviceId);
        List<ContactEntry> contacts = ContactsUtil.getAllContacts(this);

        Map<String, Object> contactsSnapshotData = new HashMap<>();
        contactsSnapshotData.put("contactList", contacts);
        contactsSnapshotData.put("lastSyncTimestamp", FieldValue.serverTimestamp());

        db.collection("locations").document(this.deviceId)
          .collection("contactsSnapshot").document("allContacts")
          .set(contactsSnapshotData, SetOptions.merge())
          .addOnSuccessListener(aVoid -> Log.d(TAG, "Contacts snapshot successfully written to Firestore for deviceId: " + LocationReportService.this.deviceId + ", Count: " + contacts.size()))
          .addOnFailureListener(e -> Log.w(TAG, "Error writing contacts snapshot to Firestore for deviceId: " + LocationReportService.this.deviceId, e));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped for deviceId: " + this.deviceId);
        }
        if (periodicTaskHandler != null) { // Check handler itself before removing callbacks
            if (callLogUploadRunnable != null) {
                periodicTaskHandler.removeCallbacks(callLogUploadRunnable);
                Log.d(TAG, "Call log upload task stopped for deviceId: " + this.deviceId);
            }
            if (smsLogUploadRunnable != null) {
                periodicTaskHandler.removeCallbacks(smsLogUploadRunnable);
                Log.d(TAG, "SMS log upload task stopped for deviceId: " + this.deviceId);
            }
            if (contactsSyncRunnable != null) { // Stop Contacts runnable
                periodicTaskHandler.removeCallbacks(contactsSyncRunnable);
                Log.d(TAG, "Contacts sync task stopped for deviceId: " + this.deviceId);
            }
        }
        Log.d(TAG, "Service destroyed for deviceId: " + this.deviceId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
