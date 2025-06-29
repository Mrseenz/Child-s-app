package com.example.childmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ParentDashboardActivity extends AppCompatActivity {

    private Button buttonAddChild;
    private Button buttonViewChildren;
    // private Button buttonViewMap; // Removed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        buttonAddChild = findViewById(R.id.buttonAddChild);
        buttonViewChildren = findViewById(R.id.buttonViewChildren);
        // buttonViewMap = findViewById(R.id.buttonViewMap); // Removed

        buttonAddChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParentDashboardActivity.this, AddChildActivity.class);
                startActivity(intent);
            }
        });

        buttonViewChildren.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ParentDashboardActivity.this, ViewChildrenActivity.class);
                startActivity(intent);
            }
        });

        // Removed OnClickListener for buttonViewMap
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logoutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, getString(R.string.logout_successful_toast), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ParentDashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish ParentDashboardActivity
    }
}
