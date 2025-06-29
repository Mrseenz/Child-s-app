package com.example.childmonitoringchildapp;

import android.provider.CallLog; // For CallLog.Calls.TYPE constants

import com.google.firebase.firestore.ServerTimestamp; // For @ServerTimestamp annotation
import java.util.Date; // For Date object

public class CallLogEntry {
    private String type;         // e.g., "INCOMING", "OUTGOING", "MISSED"
    private String phoneNumber;  // The phone number
    private long duration;       // Call duration in seconds
    private @ServerTimestamp Date date; // Firestore server ingestion timestamp
    private long callDateMillis;         // Original timestamp of the call from device log
    private String contactName;          // (Optional) Contact name if found

    // Required empty public constructor for Firestore deserialization
    public CallLogEntry() {}

    public CallLogEntry(int callType, String phoneNumber, long duration, long callDateMillis, String contactName) {
        this.type = getCallTypeString(callType);
        this.phoneNumber = phoneNumber;
        this.duration = duration;
        this.callDateMillis = callDateMillis; // Store original call time
        this.contactName = contactName != null ? contactName : ""; // Ensure not null
        // 'date' field (server timestamp) will be set by Firestore
    }

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public long getCallDateMillis() { return callDateMillis; }
    public void setCallDateMillis(long callDateMillis) { this.callDateMillis = callDateMillis; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }


    /**
     * Helper method to convert CallLog.Calls.TYPE integer to a readable string.
     * @param callType The integer type from CallLog.Calls.TYPE.
     * @return String representation of the call type.
     */
    public static String getCallTypeString(int callType) {
        switch (callType) {
            case CallLog.Calls.INCOMING_TYPE:
                return "INCOMING";
            case CallLog.Calls.OUTGOING_TYPE:
                return "OUTGOING";
            case CallLog.Calls.MISSED_TYPE:
                return "MISSED";
            case CallLog.Calls.VOICEMAIL_TYPE:
                return "VOICEMAIL";
            case CallLog.Calls.REJECTED_TYPE:
                return "REJECTED";
            case CallLog.Calls.BLOCKED_TYPE:
                return "BLOCKED";
            default:
                return "UNKNOWN (" + callType + ")";
        }
    }
}
