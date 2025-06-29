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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_child);

        editTextChildName = findViewById(R.id.editTextChildName);
        editTextChildDeviceID = findViewById(R.id.editTextChildDeviceID);
        buttonSaveChild = findViewById(R.id.buttonSaveChild);

        buttonSaveChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String childName = editTextChildName.getText().toString().trim();
                String childDeviceID = editTextChildDeviceID.getText().toString().trim();

                if (childName.isEmpty() || childDeviceID.isEmpty()) {
                    Toast.makeText(AddChildActivity.this, getString(R.string.please_fill_all_fields_toast), Toast.LENGTH_SHORT).show();
                    return;
                }

                // TODO: Implement actual saving logic (e.g., SharedPreferences, SQLite, backend API)
                // For now, just show a Toast message
                String message = getString(R.string.child_saved_mock_toast_format, childName, childDeviceID);
                Toast.makeText(AddChildActivity.this, message, Toast.LENGTH_LONG).show();

                // Optionally, finish this activity and go back to the dashboard
                // finish();
            }
        });
    }
}
