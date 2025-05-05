package com.example.snapgpmail;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmailConfigActivity extends AppCompatActivity {
    private EditText etEmail, etPassword, etRecipient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_config);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRecipient = findViewById(R.id.etRecipient);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        etEmail.setText(prefs.getString("sender_email", ""));
        etPassword.setText(prefs.getString("app_password", ""));
        etRecipient.setText(prefs.getString("recipient_email", ""));

        findViewById(R.id.btnSave).setOnClickListener(v -> saveConfig());
    }

    private void saveConfig() {
        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(this).edit();

        editor.putString("sender_email", etEmail.getText().toString());
        editor.putString("app_password", etPassword.getText().toString());
        editor.putString("recipient_email", etRecipient.getText().toString());
        editor.apply();

        Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}
