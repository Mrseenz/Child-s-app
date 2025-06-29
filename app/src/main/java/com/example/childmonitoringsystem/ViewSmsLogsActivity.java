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

public class ViewSmsLogsActivity extends AppCompatActivity {

    private static final String TAG = "ViewSmsLogsActivity";
    public static final String EXTRA_DEVICE_ID = "device_id_for_sms_logs";
    public static final String EXTRA_CHILD_NAME = "child_name_for_sms_logs"; // Optional

    private RecyclerView recyclerViewSmsLogs;
    private SmsLogAdapter smsLogAdapter;
    private List<SmsLogEntry> smsLogList;
    private FirebaseFirestore db;
    private ProgressBar progressBarSmsLogs;
    private TextView textViewNoSmsLogs;

    private String deviceId;
    private String childName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_sms_logs);

        deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
        childName = getIntent().getStringExtra(EXTRA_CHILD_NAME);

        if (deviceId == null || deviceId.isEmpty()) {
            Log.e(TAG, "No device ID provided for fetching SMS logs.");
            Toast.makeText(this, getString(R.string.error_device_id_missing), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (childName != null && !childName.isEmpty()) {
            setTitle(getString(R.string.title_activity_view_sms_logs, childName));
        } else {
            setTitle(getString(R.string.title_activity_view_sms_logs, deviceId));
        }

        db = FirebaseFirestore.getInstance();
        recyclerViewSmsLogs = findViewById(R.id.recyclerViewSmsLogs);
        progressBarSmsLogs = findViewById(R.id.progressBarSmsLogs);
        textViewNoSmsLogs = findViewById(R.id.textViewNoSmsLogs);

        smsLogList = new ArrayList<>();
        smsLogAdapter = new SmsLogAdapter(this, smsLogList);
        recyclerViewSmsLogs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSmsLogs.setAdapter(smsLogAdapter);

        fetchSmsLogs();
    }

    private void fetchSmsLogs() {
        progressBarSmsLogs.setVisibility(View.VISIBLE);
        recyclerViewSmsLogs.setVisibility(View.GONE);
        textViewNoSmsLogs.setVisibility(View.GONE);

        db.collection("locations").document(deviceId)
                .collection("smsLogs")
                .orderBy("messageDateMillis", Query.Direction.DESCENDING) // Order by original SMS time
                .limit(100) // Optional limit
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        progressBarSmsLogs.setVisibility(View.GONE);
                        smsLogList.clear();
                        if (queryDocumentSnapshots.isEmpty()) {
                            textViewNoSmsLogs.setVisibility(View.VISIBLE);
                            Log.d(TAG, "No SMS logs found for deviceId: " + deviceId);
                        } else {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                SmsLogEntry entry = document.toObject(SmsLogEntry.class);
                                smsLogList.add(entry);
                            }
                            smsLogAdapter.notifyDataSetChanged(); // Or use adapter.updateSmsLogs()
                            recyclerViewSmsLogs.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Fetched " + smsLogList.size() + " SMS logs for deviceId: " + deviceId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBarSmsLogs.setVisibility(View.GONE);
                        textViewNoSmsLogs.setText(getString(R.string.error_fetching_sms_logs, e.getMessage()));
                        textViewNoSmsLogs.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Error fetching SMS logs for " + deviceId, e);
                        Toast.makeText(ViewSmsLogsActivity.this, getString(R.string.error_fetching_sms_logs, e.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
