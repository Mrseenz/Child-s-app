package com.example.childmonitoringchildapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
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
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null && deviceId != null) {
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
            deviceId = intent.getStringExtra("deviceId");
            Log.d(TAG, "Service started with deviceId: " + deviceId);
        } else {
            Log.e(TAG, "Service started without deviceId, stopping.");
            stopSelf(); // Stop if no deviceId is provided
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, createNotification());
        startLocationUpdates();

        return START_STICKY; // If service is killed, restart it
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name), // Name from strings.xml
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setDescription(getString(R.string.notification_channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification() {
        // Intent notificationIntent = new Intent(this, MainActivity.class); // Optional: open app on click
        // PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.foreground_service_notification_title))
                .setContentText(getString(R.string.foreground_service_notification_text))
                .setSmallIcon(R.mipmap.ic_launcher) // Replace with a proper notification icon
                // .setContentIntent(pendingIntent) // Optional
                .build();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_LOCATION_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted. Cannot start updates.");
            // This should ideally be checked before starting the service.
            // MainActivity handles permission requests. If service starts without permission, it's an issue.
            stopSelf();
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.d(TAG, "Location updates started for deviceId: " + deviceId);
    }

    private void sendLocationToFirestore(Location location) {
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("timestamp", FieldValue.serverTimestamp()); // Use server-side timestamp

        db.collection("locations").document(deviceId)
                .set(locationData, SetOptions.merge()) // Creates or updates the document
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location successfully written for " + deviceId + ": " + location.getLatitude() + ", " + location.getLongitude()))
                .addOnFailureListener(e -> Log.w(TAG, "Error writing location for " + deviceId, e));

        // TODO: Update MainActivity UI - e.g. send broadcast with location data
        // Intent intent = new Intent("LocationUpdate");
        // intent.putExtra("latitude", location.getLatitude());
        // intent.putExtra("longitude", location.getLongitude());
        // LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped for deviceId: " + deviceId);
        }
        Log.d(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
}
