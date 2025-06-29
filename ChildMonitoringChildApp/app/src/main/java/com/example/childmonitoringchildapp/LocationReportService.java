package com.example.childmonitoringchildapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import java.util.Map;

public class LocationReportService extends Service {

    private static final String TAG = "LocationReportService";
    private static final String NOTIFICATION_CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 12345;
    private static final long LOCATION_UPDATE_INTERVAL = 60000; // 60 seconds
    private static final long FASTEST_LOCATION_UPDATE_INTERVAL = 30000; // 30 seconds

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private String deviceId; // Instance variable to store deviceId

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null && LocationReportService.this.deviceId != null) { // Use instance deviceId
                        Log.d(TAG, "Location received: " + location.getLatitude() + ", " + location.getLongitude() + " for deviceId: " + LocationReportService.this.deviceId);
                        sendLocationToFirestore(location);
                        // TODO: Optionally, send a broadcast to MainActivity to update UI with this location
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand received");
        if (intent != null && intent.hasExtra("deviceId")) {
            this.deviceId = intent.getStringExtra("deviceId"); // Assign to instance variable
            Log.d(TAG, "Service starting/restarted with deviceId: " + this.deviceId);
        } else if (this.deviceId == null) { // If service restarted by system and intent is null, deviceId might be null
             Log.e(TAG, "Service started or restarted without deviceId, stopping.");
             stopSelf();
             return START_NOT_STICKY; // Don't restart if we don't have a deviceId
        }
        // If this.deviceId is already set (e.g. from previous onStartCommand and service is sticky), continue

        startForeground(NOTIFICATION_ID, createNotification());
        startLocationUpdates();

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
                .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Replace with actual app icon
                .setOngoing(true)
                .build();
    }

    private void startLocationUpdates() {
        if (this.deviceId == null || this.deviceId.isEmpty()) {
            Log.e(TAG, "Cannot start location updates: deviceId is null or empty.");
            stopSelf(); // Critical to have deviceId
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
            return; // Should not happen if startLocationUpdates checks deviceId
        }
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("locations").document(this.deviceId) // Use instance deviceId
                .set(locationData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location successfully written for " + LocationReportService.this.deviceId))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing location for " + LocationReportService.this.deviceId, e));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped for deviceId: " + this.deviceId);
        }
        Log.d(TAG, "Service destroyed for deviceId: " + this.deviceId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
