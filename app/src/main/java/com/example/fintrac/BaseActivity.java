package com.example.fintrac;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public abstract class BaseActivity extends AppCompatActivity {

    // AFK Timer Variables
    private Handler disconnectHandler = new Handler(Looper.getMainLooper());
    private static final long DISCONNECT_TIMEOUT = 15 * 60 * 1000; // 15 minutes in milliseconds

    private Runnable disconnectCallback = () -> {
        // Auto-logout ang user sa Firebase
        FirebaseAuth.getInstance().signOut();
        OtpManager.clearSession(BaseActivity.this);

        Toast.makeText(BaseActivity.this, "Session expired due to 15 minutes of inactivity.", Toast.LENGTH_LONG).show();

        // I-redirect pabalik sa LogIn screen
        Intent intent = new Intent(BaseActivity.this, LogIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply theme before super.onCreate (Existing logic mo)
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
    }

    public void resetDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback);
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT);
    }

    public void stopDisconnectTimer() {
        disconnectHandler.removeCallbacks(disconnectCallback);
    }

    // Nade-detect niya tuwing tina-tap ng user ang screen
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        resetDisconnectTimer(); // Ire-reset yung 15 mins timer
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Apply theme color when activity resumes (Existing logic mo)
        SettingHelper.applyThemeColor(this);
        resetDisconnectTimer(); // Start/Reset timer kapag naka-open ang app
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDisconnectTimer(); // Stop timer kapag naka-minimize ang app
    }
}