package com.example.childmonitoringsystem;

import android.os.Bundle;
import android.text.TextUtils; // For TextUtils.isEmpty
import android.util.Log; // For logging
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List; // Required for List<Child>

public class AddChildActivity extends AppCompatActivity {

    private static final String TAG = "AddChildActivity"; // For logging

    private EditText editTextChildName;
    private EditText editTextChildDeviceID;
    private Button buttonSaveChild;

    private boolean isEditMode = false;
    private String originalDeviceIdToEdit;
    private String originalChildNameToEdit;

    private UserChildrenManager userChildrenManager;
    // Keep ChildPersistenceManager for local SharedPreferences operations
    // Decision: For now, we write to both. Loading will prioritize Firestore.
    // A more robust solution might involve a sync mechanism or choosing one source of truth.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        editTextChildName = findViewById(R.id.editTextChildName);
        editTextChildDeviceID = findViewById(R.id.editTextChildDeviceID);
        buttonSaveChild = findViewById(R.id.buttonSaveChild);

        userChildrenManager = new UserChildrenManager();

        if (getIntent().hasExtra(IntentKeys.CHILD_DEVICE_ID_TO_EDIT)) {
            isEditMode = true;
            originalDeviceIdToEdit = getIntent().getStringExtra(IntentKeys.CHILD_DEVICE_ID_TO_EDIT);
            originalChildNameToEdit = getIntent().getStringExtra(IntentKeys.CHILD_NAME_TO_EDIT);

            editTextChildName.setText(originalChildNameToEdit);
            editTextChildDeviceID.setText(originalDeviceIdToEdit);
            setTitle(getString(R.string.edit_child_activity_title));
        } else {
            setTitle(getString(R.string.add_new_child_title));
        }

        buttonSaveChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChildData();
            }
        });
    }

    private void saveChildData() {
        String childName = editTextChildName.getText().toString().trim();
        String childDeviceID = editTextChildDeviceID.getText().toString().trim();

        if (childName.isEmpty() || childDeviceID.isEmpty()) {
            Toast.makeText(AddChildActivity.this, getString(R.string.please_fill_all_fields_toast), Toast.LENGTH_SHORT).show();
            return;
        }

        final Child currentChildDetails = new Child(childName, childDeviceID);

        if (isEditMode) {
            // Local SharedPreferences Uniqueness Check (if device ID changed)
            if (!originalDeviceIdToEdit.equals(childDeviceID)) {
                List<Child> localChildren = ChildPersistenceManager.loadChildren(getApplicationContext());
                for (Child existingChild : localChildren) {
                    if (existingChild.getDeviceId().equals(childDeviceID)) {
                        Toast.makeText(AddChildActivity.this, getString(R.string.child_device_id_exists_toast, childDeviceID) + " (local)", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            // Update local SharedPreferences
            Child originalChildForLocal = new Child(originalChildNameToEdit, originalDeviceIdToEdit);
            boolean localUpdateSuccess = ChildPersistenceManager.updateChild(getApplicationContext(), originalChildForLocal, currentChildDetails);

            if (localUpdateSuccess) {
                // Update Firestore
                UserChildrenManager.OperationStatusListener firestoreListener = new UserChildrenManager.OperationStatusListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(AddChildActivity.this, getString(R.string.child_updated_toast, childName), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(AddChildActivity.this, "Firestore update error: " + errorMessage, Toast.LENGTH_LONG).show();
                        // TODO: Robust error handling: What if local save worked but Firestore failed?
                        // Potentially revert local change or mark for sync.
                        // For now, local change persists, Firestore error shown.
                    }
                };

                if (!originalDeviceIdToEdit.equals(childDeviceID)) {
                    // Device ID changed: delete old, then add new in Firestore.
                    // This is a multi-step operation, more complex for atomicity.
                    userChildrenManager.deleteChildFromFirestore(originalDeviceIdToEdit, new UserChildrenManager.OperationStatusListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Old entry " + originalDeviceIdToEdit + " deleted from Firestore during edit.");
                            userChildrenManager.addChildToFirestore(childDeviceID, childName, firestoreListener);
                        }
                        @Override
                        public void onFailure(String errorMessage) {
                            // Failed to delete old entry. New entry won't be added to avoid inconsistency.
                            Toast.makeText(AddChildActivity.this, "Firestore update error (delete old): " + errorMessage, Toast.LENGTH_LONG).show();
                            // TODO: Revert local SharedPreferences change here as Firestore update failed.
                        }
                    });
                } else {
                    // Device ID did not change, only name might have. Update existing Firestore doc.
                    userChildrenManager.updateChildInFirestore(childDeviceID, childName, firestoreListener);
                }
            } else {
                 Toast.makeText(AddChildActivity.this, getString(R.string.child_update_error_toast, childName) + " (local)", Toast.LENGTH_SHORT).show();
            }

        } else { // Adding a new child
            // Local SharedPreferences Uniqueness Check
            List<Child> localChildren = ChildPersistenceManager.loadChildren(getApplicationContext());
            if (localChildren.contains(currentChildDetails)) {
                Toast.makeText(AddChildActivity.this, getString(R.string.child_device_id_exists_toast, childDeviceID) + " (local)", Toast.LENGTH_LONG).show();
                return;
            }

            // Add to local SharedPreferences first
            ChildPersistenceManager.addChild(getApplicationContext(), currentChildDetails);

            // Then, add to Firestore
            userChildrenManager.addChildToFirestore(childDeviceID, childName, new UserChildrenManager.OperationStatusListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(AddChildActivity.this, getString(R.string.child_added_toast, childName), Toast.LENGTH_SHORT).show();
                    finish();
                }
                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(AddChildActivity.this, "Firestore add failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    // Rollback local add if Firestore add fails to maintain consistency
                    Log.w(TAG, "Firestore add failed, rolling back local SharedPreferences add for " + childDeviceID);
                    ChildPersistenceManager.deleteChild(getApplicationContext(), currentChildDetails);
                    // Inform user of rollback or specific error
                    Toast.makeText(AddChildActivity.this, "Failed to save to cloud. Local data rolled back.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
