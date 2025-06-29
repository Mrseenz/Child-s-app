package com.example.childmonitoringsystem;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


public class MapViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapViewActivity";
    public static final String EXTRA_DEVICE_ID = "device_id_to_track"; // Key for Intent extra

    private GoogleMap mMap;
    private String deviceIdToTrack;
    private FirebaseFirestore db;
    private ListenerRegistration locationListener; // For real-time updates
    private Marker childMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        db = FirebaseFirestore.getInstance();

        deviceIdToTrack = getIntent().getStringExtra(EXTRA_DEVICE_ID);

        if (deviceIdToTrack == null || deviceIdToTrack.isEmpty()) {
            Log.e(TAG, "No device ID provided to track.");
            Toast.makeText(this, getString(R.string.error_no_device_id_for_map), Toast.LENGTH_LONG).show();
            finish(); // Close activity if no device ID
            return;
        }

        setTitle("Location: " + deviceIdToTrack); // Set title dynamically

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map Fragment not found!");
            Toast.makeText(this, getString(R.string.error_map_fragment_not_found), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true); // Enable zoom controls

        // Start listening for location updates for the specific deviceId
        if (deviceIdToTrack != null) {
            startListeningForLocationUpdates(deviceIdToTrack);
        } else {
            // Default view if no device ID somehow (should be caught in onCreate)
            LatLng sydney = new LatLng(-34, 151);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        }
    }

    private void startListeningForLocationUpdates(String deviceId) {
        if (locationListener != null) {
            locationListener.remove(); // Remove any existing listener
        }

        locationListener = db.collection("locations").document(deviceId)
            .addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.w(TAG, "Listen failed for deviceId: " + deviceId, e);
                    // Potentially show a toast or UI indication of error
                    // Toast.makeText(MapViewActivity.this, "Error listening for location updates.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Double latitude = snapshot.getDouble("latitude");
                    Double longitude = snapshot.getDouble("longitude");
                    com.google.firebase.Timestamp firestoreTimestamp = snapshot.getTimestamp("timestamp");

                    if (latitude != null && longitude != null) {
                        LatLng childLocation = new LatLng(latitude, longitude);
                        String snippet = "Last update: " + (firestoreTimestamp != null ? firestoreTimestamp.toDate().toString() : "N/A");

                        if (childMarker == null) {
                            childMarker = mMap.addMarker(new MarkerOptions().position(childLocation).title(deviceIdToTrack).snippet(snippet));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(childLocation, 15f)); // Zoom level 15
                        } else {
                            childMarker.setPosition(childLocation);
                            childMarker.setSnippet(snippet);
                            // Optionally animate camera if marker moves significantly
                            // mMap.animateCamera(CameraUpdateFactory.newLatLng(childLocation));
                        }
                        Log.d(TAG, "Location updated for " + deviceId + ": " + latitude + ", " + longitude);
                    } else {
                        Log.d(TAG, "Latitude or Longitude is null in snapshot for " + deviceId);
                        // Toast.makeText(MapViewActivity.this, "Location data incomplete.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "No location data found for deviceId: " + deviceId);
                    if (childMarker != null) {
                        childMarker.remove(); // Remove marker if document is deleted or doesn't exist
                        childMarker = null;
                    }
                    Toast.makeText(MapViewActivity.this, getString(R.string.no_location_data_available, deviceId), Toast.LENGTH_SHORT).show();
                    // If no marker, means no location, set a default view
                    if (childMarker == null && mMap != null) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 1f)); // World view
                    }
                }
            });
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (locationListener != null) {
            locationListener.remove(); // Stop listening when activity is not visible
            Log.d(TAG, "Stopped location listener for " + deviceIdToTrack);
        }
    }

    // If you want to re-attach the listener when activity comes to foreground after being stopped:
    @Override
    protected void onStart() {
        super.onStart();
        if (mMap != null && deviceIdToTrack != null && locationListener == null) {
            // Or if listener was removed and needs re-attachment based on your app's lifecycle needs
            // This check ensures we don't add multiple listeners if onStart is called multiple times without onStop
            // A more robust way might be to check a boolean flag or if locationListener.remove() was called.
            // For simplicity, if mMap is ready and deviceId known, and listener is null, re-attach.
            Log.d(TAG, "Re-attaching location listener in onStart for " + deviceIdToTrack);
            startListeningForLocationUpdates(deviceIdToTrack);
        }
    }
}
