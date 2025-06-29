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
    private List<Child> childrenList;
    private UserChildrenManager userChildrenManager;
    // ProgressBar and loading state management might be needed here too
    private android.widget.ProgressBar progressBarViewChildren;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_children);

        listViewChildren = findViewById(R.id.listViewChildren);
        textViewNoChildren = findViewById(R.id.textViewNoChildren);
        progressBarViewChildren = findViewById(R.id.progressBarViewChildren); // Assuming you add this ID to XML

        userChildrenManager = new UserChildrenManager();
        childrenList = new ArrayList<>(); // Initialize empty list
        adapter = new ChildAdapter(this, childrenList);
        listViewChildren.setAdapter(adapter);

        // Initial UI state
        progressBarViewChildren.setVisibility(View.VISIBLE);
        listViewChildren.setVisibility(View.GONE);
        textViewNoChildren.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChildrenFromFirestore();
    }

    private void loadChildrenFromFirestore() {
        progressBarViewChildren.setVisibility(View.VISIBLE);
        listViewChildren.setVisibility(View.GONE);
        textViewNoChildren.setVisibility(View.GONE);

        userChildrenManager.loadChildrenFromFirestore(new UserChildrenManager.UserChildrenListener() {
            @Override
            public void onChildrenLoaded(List<Child> loadedChildren) {
                progressBarViewChildren.setVisibility(View.GONE);
                childrenList.clear();
                if (loadedChildren != null) {
                    childrenList.addAll(loadedChildren);
                }
                adapter.notifyDataSetChanged(); // Or adapter.updateChildren(childrenList) if you add such method
                updateUI();
            }

            @Override
            public void onError(String errorMessage) {
                progressBarViewChildren.setVisibility(View.GONE);
                Log.e("ViewChildrenActivity", "Error loading children: " + errorMessage);
                Toast.makeText(ViewChildrenActivity.this, getString(R.string.children_load_failure_message) + "\n" + errorMessage, Toast.LENGTH_LONG).show();
                textViewNoChildren.setText(getString(R.string.children_load_failure_message));
                updateUI(); // Will show "no children" text due to empty list (and the error text set above)
            }
        });
    }

    // updateUI is still relevant
    public void updateUI() { // Made public so adapter can call it after delete
        if (childrenList == null || childrenList.isEmpty()) {
            listViewChildren.setVisibility(View.GONE);
            // Ensure "no children" text is appropriate if it was an error message before
            if (progressBarViewChildren.getVisibility() == View.GONE) { // Only set if not loading
                 textViewNoChildren.setText(R.string.no_children_added_text); // Reset to default "no children"
            }
            textViewNoChildren.setVisibility(View.VISIBLE);
        } else {
            listViewChildren.setVisibility(View.VISIBLE);
            textViewNoChildren.setVisibility(View.GONE);
        }
    }

    // Note: onResume now calls loadChildrenFromFirestore.
    // Local SharedPreferences (ChildPersistenceManager) is no longer the primary source for this view.
}
