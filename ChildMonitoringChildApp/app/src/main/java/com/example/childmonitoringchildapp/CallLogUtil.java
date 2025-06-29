package com.example.childmonitoringchildapp;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date; // For using in CallLogEntry if not using @ServerTimestamp primarily
import java.util.List;

public class CallLogUtil {

    private static final String TAG = "CallLogUtil";

    /**
     * Fetches call logs from the device after a specified timestamp.
     *
     * @param context         The application context.
     * @param lastFetchTimeMs Timestamp (in milliseconds since epoch) of the last fetch.
     *                        Only calls newer than this will be fetched.
     *                        Pass 0 to fetch all available (or up to a reasonable limit).
     * @return A list of CallLogEntry objects.
     */
    public static List<CallLogEntry> getCallLogsAfter(Context context, long lastFetchTimeMs) {
        List<CallLogEntry> callLogEntries = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri callLogUri = CallLog.Calls.CONTENT_URI;

        // Columns to fetch
        String[] projection = {
                CallLog.Calls.TYPE,
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,     // Date the call occurred, in milliseconds since epoch
                CallLog.Calls.DURATION, // Duration of the call in seconds
                // CallLog.Calls.CACHED_NAME // Optional: Contact name (requires READ_CONTACTS too)
        };

        // Selection: Fetch calls newer than lastFetchTimeMs
        String selection = CallLog.Calls.DATE + " > ?";
        String[] selectionArgs = {String.valueOf(lastFetchTimeMs)};

        // Sort order: Newest calls first (optional, but good for limiting if needed)
        String sortOrder = CallLog.Calls.DATE + " DESC";
        // To limit number of results, you can append " LIMIT N" to sortOrder if not using ContentProvider limit argument

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(callLogUri, projection, selection, selectionArgs, sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
                int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);
                int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
                // int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME); // If fetching name

                do {
                    int type = cursor.getInt(typeIndex);
                    String number = cursor.getString(numberIndex);
                    long dateMillis = cursor.getLong(dateIndex);
                    long duration = cursor.getLong(durationIndex);
                    // String name = (nameIndex != -1) ? cursor.getString(nameIndex) : null;
                    String name = null; // Not fetching name in this phase

                    if (number == null) number = "Unknown"; // Handle cases where number might be null (e.g. private)

                    // Note: CallLogEntry's 'date' field is @ServerTimestamp.
                    // We pass 'dateMillis' from the call log to the constructor,
                    // but it's primarily for record-keeping or if we decide to use client-time.
                    // The actual 'date' field in Firestore will be the server's ingestion time.
                    callLogEntries.add(new CallLogEntry(type, number, duration, dateMillis, name));

                } while (cursor.moveToNext());
                Log.d(TAG, "Fetched " + callLogEntries.size() + " call logs after " + new Date(lastFetchTimeMs).toString());
            } else {
                Log.d(TAG, "No call logs found after " + new Date(lastFetchTimeMs).toString());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: READ_CALL_LOG permission might be missing or revoked.", e);
            // This should be handled by requesting permission in MainActivity.
            // If it still occurs, it's a problem.
        } catch (Exception e) {
            Log.e(TAG, "Error fetching call logs: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return callLogEntries;
    }

    /**
     * Gets the timestamp of the most recent call log entry.
     * Can be used to determine the 'lastFetchTimeMs' for the next fetch.
     * @param context The application context.
     * @return Timestamp in milliseconds, or 0 if no call logs.
     */
    public static long getTimestampOfLatestCall(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri callLogUri = CallLog.Calls.CONTENT_URI;
        String[] projection = { CallLog.Calls.DATE };
        String sortOrder = CallLog.Calls.DATE + " DESC LIMIT 1";
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(callLogUri, projection, null, null, sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting timestamp of latest call: ", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }
}
