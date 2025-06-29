package com.example.childmonitoringchildapp;

import java.util.List;
import java.util.Map;

// Note: This POJO is intended to be part of a list within a single Firestore document,
// rather than each ContactEntry being a separate document itself.
// Thus, it doesn't need @ServerTimestamp for each entry, but rather the parent document
// holding the list of contacts would have a timestamp.
// However, if we were to store each contact as a separate doc, @ServerTimestamp would be here.

public class ContactEntry {
    private long contactId; // Original ID from device ContactsContract.Contacts._ID
    private String displayName;
    private List<Map<String, String>> phoneNumbers; // List of maps, e.g., [{"type": "Mobile", "number": "123"}, ...]
    // private List<Map<String, String>> emails; // Optional for future

    // Required empty public constructor for Firestore deserialization (if used directly)
    // or for object mappers like Gson if serializing a list of these.
    public ContactEntry() {}

    public ContactEntry(long contactId, String displayName, List<Map<String, String>> phoneNumbers) {
        this.contactId = contactId;
        this.displayName = displayName;
        this.phoneNumbers = phoneNumbers;
    }

    // Getters and Setters
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

    // Example of how a phone number map would look:
    // Map<String, String> phone = new HashMap<>();
    // phone.put("type", "Mobile");
    // phone.put("number", "555-1234");
    // phoneNumbers.add(phone);
}
