package com.example.childmonitoringsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// This POJO mirrors the structure used by the Child App for its ContactEntry objects
// when they are serialized as part of a list in the Firestore snapshot document.
public class ContactEntry {
    private long contactId;
    private String displayName;
    private List<Map<String, String>> phoneNumbers;

    // Required empty public constructor for Firestore deserialization
    public ContactEntry() {
        // Initialize list to prevent null pointer exceptions if Firestore data is sparse
        this.phoneNumbers = new ArrayList<>();
    }

    // Getters and Setters (required by Firestore for deserialization into this POJO)
    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<Map<String, String>> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<Map<String, String>> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    // Helper method to get a formatted string of phone numbers for display
    public String getFormattedPhoneNumbers(Context context) { // Added context
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            return context.getString(R.string.no_phone_numbers_available);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phoneNumbers.size(); i++) {
            Map<String, String> phone = phoneNumbers.get(i);
            String type = phone.get("type");
            String number = phone.get("number");
            if (type == null) type = "Other"; // Default type if null
            if (number == null) number = "N/A";

            sb.append(type).append(": ").append(number);
            if (i < phoneNumbers.size() - 1) {
                sb.append("\n"); // New line for each subsequent number
            }
        }
        return sb.toString();
    }
}
