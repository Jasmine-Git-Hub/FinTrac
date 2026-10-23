package com.example.fintrac;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.google.android.material.bottomnavigation.BottomNavigationView;

// Import para sa Firebase
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- FIREBASE OFFLINE MODE CHECKER ---
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (SettingHelper.isOfflineModeEnabled(this)) {
            // Kung naka-ON ang offline mode sa settings, putulin agad ang internet ni Firebase
            db.disableNetwork().addOnCompleteListener(task -> {
                Log.d("FirebaseTest", "App started in OFFLINE mode.");
            });
        } else {
            // Kung naka-OFF, hayaang mag-sync
            db.enableNetwork().addOnCompleteListener(task -> {
                Log.d("FirebaseTest", "App started in ONLINE mode.");
            });
        }

        // Initialize buttons
        Button getStartedButton = findViewById(R.id.get_started_button);
        Button alreadyHaveAccountButton = findViewById(R.id.already_have_account_button);

        if (getStartedButton != null) {
            getStartedButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, sign_in.class);
                startActivity(intent);
            });
        }

        if (alreadyHaveAccountButton != null) {
            alreadyHaveAccountButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LogIn.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingHelper.applyThemeColor(this);
    }

}