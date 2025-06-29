package com.example.childmonitoringchildapp;

import android.provider.Telephony; // For Telephony.Sms.TYPE constants
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class SmsLogEntry {
    private String address;          // Other party's phone number
    private int bodyLength;          // Length of the SMS body
    private long messageDateMillis;  // Original timestamp of the message from device
    private String type;             // e.g., "INBOX", "SENT", "DRAFT"
    private long threadId;           // Thread ID for grouping
    private boolean read;            // Read status (for inbox messages)
    private @ServerTimestamp Date firestoreTimestamp; // Firestore server ingestion timestamp

    // Required empty public constructor for Firestore deserialization
    public SmsLogEntry() {}

    public SmsLogEntry(String address, int bodyLength, long messageDateMillis, int messageType, long threadId, boolean read) {
        this.address = address;
        this.bodyLength = bodyLength;
        this.messageDateMillis = messageDateMillis;
        this.type = getSmsTypeString(messageType);
        this.threadId = threadId;
        this.read = read;
        // firestoreTimestamp will be set by Firestore
    }

    // Getters and Setters
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getBodyLength() { return bodyLength; }
    public void setBodyLength(int bodyLength) { this.bodyLength = bodyLength; }

    public long getMessageDateMillis() { return messageDateMillis; }
    public void setMessageDateMillis(long messageDateMillis) { this.messageDateMillis = messageDateMillis; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; } // Mainly for Firestore, type set via constructor

    public long getThreadId() { return threadId; }
    public void setThreadId(long threadId) { this.threadId = threadId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Date getFirestoreTimestamp() { return firestoreTimestamp; }
    public void setFirestoreTimestamp(Date firestoreTimestamp) { this.firestoreTimestamp = firestoreTimestamp; }


    public static String getSmsTypeString(int messageType) {
        switch (messageType) {
            case Telephony.Sms.MESSAGE_TYPE_INBOX:
                return "INBOX";
            case Telephony.Sms.MESSAGE_TYPE_SENT:
                return "SENT";
            case Telephony.Sms.MESSAGE_TYPE_DRAFT:
                return "DRAFT";
            case Telephony.Sms.MESSAGE_TYPE_OUTBOX:
                return "OUTBOX";
            case Telephony.Sms.MESSAGE_TYPE_FAILED:
                return "FAILED";
            case Telephony.Sms.MESSAGE_TYPE_QUEUED:
                return "QUEUED";
            case Telephony.Sms.MESSAGE_TYPE_ALL: // Should not typically be a type for a single message
                return "ALL";
            default:
                return "UNKNOWN (" + messageType + ")";
        }
    }
}
