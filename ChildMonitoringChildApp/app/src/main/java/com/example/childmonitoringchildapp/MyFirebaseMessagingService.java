package com.example.childmonitoringchildapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String FCM_NOTIFICATION_CHANNEL_ID = "FCMNotificationChannel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Get deviceId from SharedPreferences (saved by MainActivity)
        SharedPreferences prefs = getSharedPreferences("ChildAppPrefs", MODE_PRIVATE);
        String deviceId = prefs.getString("deviceId", null);

        if (deviceId != null && !deviceId.isEmpty()) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("fcmToken", token);
            // Update the existing document for this deviceId in 'locations' collection
            // Alternatively, use a separate '/devices/{deviceId}' collection if preferred
            db.collection("locations").document(deviceId)
                    .update(tokenData) // Use update to add/change fcmToken without overwriting other fields
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token successfully updated in Firestore for deviceId: " + deviceId))
                    .addOnFailureListener(e -> Log.w(TAG, "Error updating FCM token in Firestore for deviceId: " + deviceId, e));
        } else {
            Log.w(TAG, "Cannot send FCM token to server: deviceId is null or empty.");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            // Handle data payload here (e.g., for silent pushes or custom UI)
            // String customData = remoteMessage.getData().get("my_custom_key");
        }

        // Check if message contains a notification payload.
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification != null) {
            Log.d(TAG, "Message Notification Title: " + notification.getTitle());
            Log.d(TAG, "Message Notification Body: " + notification.getBody());
            sendNotification(notification.getTitle(), notification.getBody());
        } else {
            // If no notification payload, try to get from data payload if app specific
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            if (title != null && body != null) {
                sendNotification(title, body);
            }
        }
    }

    private void sendNotification(String messageTitle, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class); // Open MainActivity on notification click
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, FCM_NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with actual app icon
                        .setContentTitle(messageTitle)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(FCM_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.fcm_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.fcm_notification_channel_description));
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        if (notificationManager != null) {
            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }
    }
}
