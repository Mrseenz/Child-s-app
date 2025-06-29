package com.example.childmonitoringsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ViewChildrenActivity extends AppCompatActivity {

    private ListView listViewChildren;
    private TextView textViewNoChildren;
    private ChildAdapter adapter;
    private List<Child> childrenList; // This should be populated from persistent storage

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_children);

        listViewChildren = findViewById(R.id.listViewChildren);
        textViewNoChildren = findViewById(R.id.textViewNoChildren);
        childrenList = new ArrayList<>();

        // TODO: Load children from SharedPreferences or a database
        // For now, add some mock data
        loadMockChildren();

        adapter = new ChildAdapter(this, childrenList);
        listViewChildren.setAdapter(adapter);

        updateUI();
    }

    private void loadMockChildren() {
        // In a real app, this data would come from SharedPreferences, SQLite, or a server
        // For demonstration, we add a few mock children if the list is empty.
        // In a real scenario, you'd load them in onResume or similar lifecycle methods
        // and ensure AddChildActivity actually saves them where they can be retrieved here.

        // childrenList.add(new Child("Alice", "device123"));
        // childrenList.add(new Child("Bob", "device456"));
        // childrenList.add(new Child("Charlie", "device789"));

        // For now, the list will be empty until actual persistence is implemented
        // and AddChildActivity saves data to it.
    }

    private void updateUI() {
        if (childrenList.isEmpty()) {
            listViewChildren.setVisibility(View.GONE);
            textViewNoChildren.setVisibility(View.VISIBLE);
        } else {
            listViewChildren.setVisibility(View.VISIBLE);
            textViewNoChildren.setVisibility(View.GONE);
        }
        if(adapter != null) {
            adapter.notifyDataSetChanged(); // Refresh the list
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: Refresh the list from the data source in case a child was added/edited
        // For now, this won't do much as data isn't persisted across activities yet.
        // If AddChildActivity actually saved data, we would reload it here.
        updateUI();
    }

    // TODO: Add methods to handle data persistence (saving/loading children)
    // For example, using SharedPreferences or a local SQLite database.
    // When a child is deleted in the adapter, the actual data source needs to be updated.
}
