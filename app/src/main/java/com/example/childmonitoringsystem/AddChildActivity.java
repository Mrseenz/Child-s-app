package com.example.childmonitoringsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddChildActivity extends AppCompatActivity {

    private EditText editTextChildName;
    private EditText editTextChildDeviceID;
    private Button buttonSaveChild;

    private boolean isEditMode = false;
    private String originalDeviceIdToEdit; // Used to identify the child if deviceId itself is changed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        editTextChildName = findViewById(R.id.editTextChildName);
        editTextChildDeviceID = findViewById(R.id.editTextChildDeviceID);
        buttonSaveChild = findViewById(R.id.buttonSaveChild);

        // Check if launched in edit mode
        if (getIntent().hasExtra(IntentKeys.CHILD_DEVICE_ID_TO_EDIT)) {
            isEditMode = true;
            originalDeviceIdToEdit = getIntent().getStringExtra(IntentKeys.CHILD_DEVICE_ID_TO_EDIT);
            String nameToEdit = getIntent().getStringExtra(IntentKeys.CHILD_NAME_TO_EDIT);

            editTextChildName.setText(nameToEdit);
            editTextChildDeviceID.setText(originalDeviceIdToEdit);
            // Optionally, make deviceId non-editable in edit mode if it's a primary key
            // editTextChildDeviceID.setEnabled(false);
            setTitle("Edit Child"); // Update activity title
        } else {
            setTitle("Add New Child");
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

        Child currentChildDetails = new Child(childName, childDeviceID);

        if (isEditMode) {
            Child originalChild = new Child("", originalDeviceIdToEdit); // Create a shell Child object with the original ID for lookup
            boolean updated = ChildPersistenceManager.updateChild(getApplicationContext(), originalChild, currentChildDetails);
            if (updated) {
                Toast.makeText(AddChildActivity.this, childName + " updated.", Toast.LENGTH_SHORT).show();
                finish(); // Go back after successful update
            } else {
                Toast.makeText(AddChildActivity.this, "Error updating " + childName, Toast.LENGTH_SHORT).show();
            }
        } else {
            // Check if child with this device ID already exists before adding
            List<Child> existingChildren = ChildPersistenceManager.loadChildren(getApplicationContext());
            if (existingChildren.contains(currentChildDetails)) { // Relies on Child.equals()
                Toast.makeText(AddChildActivity.this, "Child with Device ID " + childDeviceID + " already exists.", Toast.LENGTH_LONG).show();
                return;
            }

            ChildPersistenceManager.addChild(getApplicationContext(), currentChildDetails);
            Toast.makeText(AddChildActivity.this, childName + " added.", Toast.LENGTH_SHORT).show();
            finish(); // Go back after successful add
        }
    }
}
