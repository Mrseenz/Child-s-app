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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewCallLogsActivity extends AppCompatActivity {

    private static final String TAG = "ViewCallLogsActivity";
    public static final String EXTRA_DEVICE_ID = "device_id_for_logs";
    public static final String EXTRA_CHILD_NAME = "child_name_for_logs"; // Optional, for title

    private RecyclerView recyclerViewCallLogs;
    private CallLogAdapter callLogAdapter;
    private List<CallLogEntry> callLogList;
    private FirebaseFirestore db;
    private ProgressBar progressBarCallLogs;
    private TextView textViewNoCallLogs;

    private String deviceId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_call_logs);

        deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
        childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "No device ID provided for fetching call logs.");
            Toast.makeText(this, "Error: Device ID missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (childName != null && !childName.isEmpty()) {
            setTitle(getString(R.string.title_activity_view_call_logs, childName));
        } else {
            setTitle(getString(R.string.title_activity_view_call_logs, deviceId));
        }

        db = FirebaseFirestore.getInstance();
        recyclerViewCallLogs = findViewById(R.id.recyclerViewCallLogs);
        progressBarCallLogs = findViewById(R.id.progressBarCallLogs);
        textViewNoCallLogs = findViewById(R.id.textViewNoCallLogs);

        callLogList = new ArrayList<>();
        callLogAdapter = new CallLogAdapter(this, callLogList);
        recyclerViewCallLogs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCallLogs.setAdapter(callLogAdapter);

        fetchCallLogs();
    }

    private void fetchCallLogs() {
        progressBarCallLogs.setVisibility(View.VISIBLE);
        recyclerViewCallLogs.setVisibility(View.GONE);
        textViewNoCallLogs.setVisibility(View.GONE);

        db.collection("locations").document(deviceId)
                .collection("callLogs")
                .orderBy("callDateMillis", Query.Direction.DESCENDING) // Order by original call time
                .limit(100) // Optionally limit the number of logs fetched
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        progressBarCallLogs.setVisibility(View.GONE);
                        callLogList.clear();
                        if (queryDocumentSnapshots.isEmpty()) {
                            textViewNoCallLogs.setVisibility(View.VISIBLE);
                            Log.d(TAG, "No call logs found for deviceId: " + deviceId);
                        } else {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                CallLogEntry entry = document.toObject(CallLogEntry.class);
                                callLogList.add(entry);
                            }
                            callLogAdapter.notifyDataSetChanged(); // Or use adapter.updateCallLogs()
                            recyclerViewCallLogs.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Fetched " + callLogList.size() + " call logs for deviceId: " + deviceId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBarCallLogs.setVisibility(View.GONE);
                        textViewNoCallLogs.setText(getString(R.string.error_fetching_call_logs, e.getMessage()));
                        textViewNoCallLogs.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Error fetching call logs for " + deviceId, e);
                        Toast.makeText(ViewCallLogsActivity.this, getString(R.string.error_fetching_call_logs, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
