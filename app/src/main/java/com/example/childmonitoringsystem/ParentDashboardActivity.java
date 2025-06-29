package com.example.childmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

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
        // buttonViewMap.setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View v) {
        //         Intent intent = new Intent(ParentDashboardActivity.this, MapViewActivity.class);
        //         startActivity(intent);
        //     }
        // });
    }
}
