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

        // childrenList will be initialized in onResume/loadChildrenData
        // adapter will also be initialized/updated there.
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildrenData();
    }

    private void loadChildrenData() {
        childrenList = ChildPersistenceManager.loadChildren(getApplicationContext());
        if (childrenList == null) { // Should not happen with current PersistenceManager returning new ArrayList
            childrenList = new ArrayList<>();
        }

        if (adapter == null) {
            adapter = new ChildAdapter(this, childrenList);
            listViewChildren.setAdapter(adapter);
        } else {
            // Clear adapter's old data and add all new data
            // This is important if the list instance itself changes
            adapter.clear();
            adapter.addAll(childrenList);
            adapter.notifyDataSetChanged();
        }
        updateUI();
    }

    private void updateUI() {
        if (childrenList == null || childrenList.isEmpty()) { // Added null check for robustness
            listViewChildren.setVisibility(View.GONE);
            textViewNoChildren.setVisibility(View.VISIBLE);
        } else {
            listViewChildren.setVisibility(View.VISIBLE);
            textViewNoChildren.setVisibility(View.GONE);
        }
        if(adapter != null) {
            // adapter.notifyDataSetChanged(); // Already called in loadChildrenData if adapter exists
        }
    }

    // Note: onResume now calls loadChildrenData which handles adapter refresh.
    // The old onResume logic is removed.

    // TODO: Add methods to handle data persistence (saving/loading children) - This is now handled by ChildPersistenceManager
    // For example, using SharedPreferences or a local SQLite database.
    // When a child is deleted in the adapter, the actual data source needs to be updated.
}
