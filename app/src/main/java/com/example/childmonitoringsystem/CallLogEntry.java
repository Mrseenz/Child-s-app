package com.example.childmonitoringsystem;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CallLogEntry {
    private String type;
    private String phoneNumber;
    private long duration; // in seconds
    private long callDateMillis; // Original device timestamp of the call
    private String contactName; // Currently not populated by Child App
    private @ServerTimestamp Date date; // Firestore server timestamp of when this record was written

    // Required empty public constructor for Firestore deserialization
    public CallLogEntry() {}

    // Getters (setters are used by Firestore during deserialization)
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getCallDateMillis() { return callDateMillis; }
    public void setCallDateMillis(long callDateMillis) { this.callDateMillis = callDateMillis; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    // Helper to format duration, e.g., "1m 25s"
    public String getFormattedDuration() {
        if (duration < 0) return "N/A";
        if (duration == 0) return "0s";
        long minutes = duration / 60;
        long seconds = duration % 60;
        if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm %ds", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%ds", seconds);
        }
    }

    // Helper to format date
    public String getFormattedCallDate() {
        if (callDateMillis <= 0) return "Unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(callDateMillis));
    }
}
