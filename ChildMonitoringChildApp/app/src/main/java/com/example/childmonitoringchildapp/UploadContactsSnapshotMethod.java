// This is a temporary file containing the method to be inserted.
// It will be merged into LocationReportService.java.

    private void uploadContactsSnapshot() {
        if (this.deviceId == null || this.deviceId.isEmpty()) {
            Log.w(TAG, "uploadContactsSnapshot: deviceId is null or empty, skipping contacts sync.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_CHILD_APP, MODE_PRIVATE);
        boolean contactsSyncEnabled = prefs.getBoolean(KEY_CONTACTS_SYNC_ENABLED, false);

        if (!contactsSyncEnabled) {
            Log.d(TAG, "uploadContactsSnapshot: Contacts Sync is disabled in SharedPreferences for deviceId: " + this.deviceId);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "uploadContactsSnapshot: READ_CONTACTS permission not granted. Cannot fetch contacts for deviceId: " + this.deviceId);
            return;
        }

        Log.d(TAG, "uploadContactsSnapshot: Fetching all contacts for deviceId: " + this.deviceId);
        List<ContactEntry> contacts = ContactsUtil.getAllContacts(this);

        if (contacts.isEmpty()) {
            Log.d(TAG, "uploadContactsSnapshot: No contacts found on device or list is empty for deviceId: " + this.deviceId);
            // Still, we might want to write an empty list to indicate a sync happened
            // or if the previous snapshot should be cleared.
            // For now, let's proceed to write even if empty to represent current state.
        }

        // Prepare data for Firestore: a map holding the list and a timestamp
        Map<String, Object> contactsSnapshotData = new HashMap<>();
        contactsSnapshotData.put("contactList", contacts); // Firestore can serialize List<POJO>
        contactsSnapshotData.put("lastSyncTimestamp", FieldValue.serverTimestamp());

        db.collection("locations").document(this.deviceId)
          .collection("contactsSnapshot").document("allContacts") // Single document for the whole list
          .set(contactsSnapshotData, SetOptions.merge()) // Overwrite the document with the new snapshot
          .addOnSuccessListener(aVoid -> Log.d(TAG, "Contacts snapshot successfully written to Firestore for deviceId: " + LocationReportService.this.deviceId + ", Count: " + contacts.size()))
          .addOnFailureListener(e -> Log.w(TAG, "Error writing contacts snapshot to Firestore for deviceId: " + LocationReportService.this.deviceId, e));
    }
