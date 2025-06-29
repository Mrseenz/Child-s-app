package com.example.childmonitoringsystem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp; // For reading timestamp from Firestore
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewContactsActivity extends AppCompatActivity {

    private static final String TAG = "ViewContactsActivity";
    public static final String EXTRA_DEVICE_ID = "device_id_for_contacts";
    public static final String EXTRA_CHILD_NAME = "child_name_for_contacts"; // Optional

    private RecyclerView recyclerViewContacts;
    private ContactsAdapter contactsAdapter;
    private List<ContactEntry> contactList; // List of POJOs
    private FirebaseFirestore db;
    private ProgressBar progressBarContacts;
    private TextView textViewNoContacts;
    private TextView textViewLastSyncedContacts;

    private String deviceId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_contacts);

        deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
        childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "No device ID provided for fetching contacts.");
            Toast.makeText(this, getString(R.string.error_device_id_missing), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (childName != null && !childName.isEmpty()) {
            setTitle(getString(R.string.title_activity_view_contacts, childName));
        } else {
            setTitle(getString(R.string.title_activity_view_contacts, deviceId));
        }

        db = FirebaseFirestore.getInstance();
        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        progressBarContacts = findViewById(R.id.progressBarContacts);
        textViewNoContacts = findViewById(R.id.textViewNoContacts);
        textViewLastSyncedContacts = findViewById(R.id.textViewLastSyncedContacts);

        contactList = new ArrayList<>();
        contactsAdapter = new ContactsAdapter(contactList); // Context not strictly needed by adapter now
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewContacts.setAdapter(contactsAdapter);

        fetchContactsSnapshot();
    }

    private void fetchContactsSnapshot() {
        progressBarContacts.setVisibility(View.VISIBLE);
        recyclerViewContacts.setVisibility(View.GONE);
        textViewNoContacts.setVisibility(View.GONE);
        textViewLastSyncedContacts.setVisibility(View.GONE);

        db.collection("locations").document(deviceId)
                .collection("contactsSnapshot").document("allContacts")
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        progressBarContacts.setVisibility(View.GONE);
                        contactList.clear();
                        if (documentSnapshot.exists()) {
                            List<Map<String, Object>> rawContactList = (List<Map<String, Object>>) documentSnapshot.get("contactList");
                            Timestamp lastSyncFirestoreTimestamp = documentSnapshot.getTimestamp("lastSyncTimestamp");

                            if (lastSyncFirestoreTimestamp != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy, hh:mm a", Locale.getDefault());
                                textViewLastSyncedContacts.setText(getString(R.string.last_synced_label) + sdf.format(lastSyncFirestoreTimestamp.toDate()));
                                textViewLastSyncedContacts.setVisibility(View.VISIBLE);
                            }

                            if (rawContactList != null && !rawContactList.isEmpty()) {
                                for (Map<String, Object> map : rawContactList) {
                                    ContactEntry entry = new ContactEntry();
                                    entry.setContactId((Long) map.get("contactId"));
                                    entry.setDisplayName((String) map.get("displayName"));
                                    // Firestore might store list of maps as is, or convert to list of ContactEntryPhone
                                    // Assuming it's List<Map<String, String>> for phoneNumbers
                                    if (map.get("phoneNumbers") instanceof List) {
                                        entry.setPhoneNumbers((List<Map<String, String>>) map.get("phoneNumbers"));
                                    }
                                    contactList.add(entry);
                                }
                                contactsAdapter.updateContacts(contactList); // Use the adapter's update method
                                recyclerViewContacts.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Fetched " + contactList.size() + " contacts for deviceId: " + deviceId);
                            } else {
                                textViewNoContacts.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Contact list is empty or null for deviceId: " + deviceId);
                            }
                        } else {
                            textViewNoContacts.setVisibility(View.VISIBLE);
                            Log.d(TAG, "No contacts snapshot document found for deviceId: " + deviceId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBarContacts.setVisibility(View.GONE);
                        textViewNoContacts.setText(getString(R.string.error_fetching_contacts, e.getMessage()));
                        textViewNoContacts.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Error fetching contacts snapshot for " + deviceId, e);
                        Toast.makeText(ViewContactsActivity.this, getString(R.string.error_fetching_contacts, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
