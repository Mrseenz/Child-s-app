package com.example.childmonitoringchildapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SmsLogUtil {

    private static final String TAG = "SmsLogUtil";

    /**
     * Fetches SMS logs from the device after a specified timestamp.
     *
     * @param context         The application context.
     * @param lastFetchTimeMs Timestamp (in milliseconds since epoch) of the last fetch.
     *                        Only SMS messages newer than this will be fetched.
     * @return A list of SmsLogEntry objects.
     */
    public static List<SmsLogEntry> getSmsLogsAfter(Context context, long lastFetchTimeMs) {
        List<SmsLogEntry> smsLogEntries = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        // Query all SMS types (inbox, sent, draft etc.)
        Uri smsUri = Telephony.Sms.CONTENT_URI;

        // Columns to fetch
        String[] projection = {
                Telephony.Sms.ADDRESS,      // Phone number of the other party
                Telephony.Sms.BODY,         // We'll get length from this
                Telephony.Sms.DATE,         // Date the message was sent or received, in milliseconds
                Telephony.Sms.TYPE,         // Type of the message (inbox, sent, draft)
                Telephony.Sms.THREAD_ID,    // Thread ID for grouping
                Telephony.Sms.READ          // Read status (0 for unread, 1 for read)
        };

        // Selection: Fetch messages newer than lastFetchTimeMs
        String selection = Telephony.Sms.DATE + " > ?";
        String[] selectionArgs = {String.valueOf(lastFetchTimeMs)};

        // Sort order: Newest messages first
        String sortOrder = Telephony.Sms.DATE + " DESC";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(smsUri, projection, selection, selectionArgs, sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
                int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID);
                int readIndex = cursor.getColumnIndex(Telephony.Sms.READ);

                do {
                    String address = cursor.getString(addressIndex);
                    String body = cursor.getString(bodyIndex); // Get full body to calculate length
                    long dateMillis = cursor.getLong(dateIndex);
                    int type = cursor.getInt(typeIndex);
                    long threadId = cursor.getLong(threadIdIndex);
                    boolean read = cursor.getInt(readIndex) == 1;

                    int bodyLength = (body != null) ? body.length() : 0;
                    if (address == null) address = "Unknown";

                    smsLogEntries.add(new SmsLogEntry(address, bodyLength, dateMillis, type, threadId, read));

                } while (cursor.moveToNext());
                Log.d(TAG, "Fetched " + smsLogEntries.size() + " SMS logs after " + new Date(lastFetchTimeMs).toString());
            } else {
                Log.d(TAG, "No SMS logs found after " + new Date(lastFetchTimeMs).toString());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: READ_SMS permission might be missing or revoked.", e);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching SMS logs: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return smsLogEntries;
    }

    /**
     * Gets the timestamp of the most recent SMS log entry.
     * Can be used to determine the 'lastFetchTimeMs' for the next fetch.
     * @param context The application context.
     * @return Timestamp in milliseconds, or 0 if no SMS logs.
     */
    public static long getTimestampOfLatestSms(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri smsUri = Telephony.Sms.CONTENT_URI;
        String[] projection = { Telephony.Sms.DATE };
        String sortOrder = Telephony.Sms.DATE + " DESC LIMIT 1";
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(smsUri, projection, null, null, sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting timestamp of latest SMS: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }
}
