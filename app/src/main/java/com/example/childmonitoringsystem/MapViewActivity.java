package com.example.childmonitoringsystem;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

// For actual map integration, you would likely need:
// import com.google.android.gms.maps.GoogleMap;
// import com.google.android.gms.maps.OnMapReadyCallback;
// import com.google.android.gms.maps.SupportMapFragment;
// import com.google.android.gms.maps.model.LatLng;
// import com.google.android.gms.maps.model.MarkerOptions;

public class MapViewActivity extends AppCompatActivity /* implements OnMapReadyCallback */ {

    // private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        //setTitle("Child's Location"); // You can set the title here or in the manifest

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        //         .findFragmentById(R.id.map);
        // if (mapFragment != null) {
        //     mapFragment.getMapAsync(this);
        // } else {
        //     // Handle case where map fragment is not found, perhaps show an error
        //     Toast.makeText(this, getString(R.string.error_map_fragment_not_found), Toast.LENGTH_LONG).show();
        // }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    // @Override
    // public void onMapReady(GoogleMap googleMap) {
    //     mMap = googleMap;

    //     // TODO: Get child's actual coordinates
    //     LatLng childLocation = new LatLng(-34, 151); // Placeholder: Sydney
    //     mMap.addMarker(new MarkerOptions().position(childLocation).title("Child's Location"));
    //     mMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(childLocation, 15));
    // }
}
