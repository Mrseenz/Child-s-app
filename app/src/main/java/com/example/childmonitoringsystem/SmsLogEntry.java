package com.example.childmonitoringsystem;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class SmsLogEntry {
    private String address;
    private int bodyLength;
    private long messageDateMillis; // Original device timestamp of the SMS
    private String type;
    private long threadId;
    private boolean read;
    private @ServerTimestamp Date firestoreTimestamp; // Firestore server timestamp

    // Required empty public constructor for Firestore deserialization
    public SmsLogEntry() {}

    // Getters (and setters if modifying, but primarily for reading from Firestore)
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getBodyLength() { return bodyLength; }
    public void setBodyLength(int bodyLength) { this.bodyLength = bodyLength; }

    public long getMessageDateMillis() { return messageDateMillis; }
    public void setMessageDateMillis(long messageDateMillis) { this.messageDateMillis = messageDateMillis; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getThreadId() { return threadId; }
    public void setThreadId(long threadId) { this.threadId = threadId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Date getFirestoreTimestamp() { return firestoreTimestamp; }
    public void setFirestoreTimestamp(Date firestoreTimestamp) { this.firestoreTimestamp = firestoreTimestamp; }

    // Helper to format date
    public String getFormattedMessageDate() {
        if (messageDateMillis <= 0) return "Unknown date";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(messageDateMillis));
    }

    public String getFormattedType() {
        if (type == null) return "Unknown";
        // Capitalize first letter, make rest lowercase for display
        if (type.length() > 1) {
            return type.substring(0, 1).toUpperCase(Locale.ROOT) + type.substring(1).toLowerCase(Locale.ROOT);
        } else if (type.length() == 1) {
            return type.toUpperCase(Locale.ROOT);
        }
        return "Unknown";
    }
}
