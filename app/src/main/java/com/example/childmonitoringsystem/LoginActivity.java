package com.example.childmonitoringsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                // Mock login logic
                if (email.equals("parent@example.com") && password.equals("password")) {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_successful_toast), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, ParentDashboardActivity.class);
                    startActivity(intent);
                    finish(); // Finish LoginActivity so user can't go back to it with back button
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.invalid_credentials_toast), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
