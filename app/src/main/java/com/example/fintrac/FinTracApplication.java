package com.example.fintrac;

import android.app.Application;
import android.content.Context;

public class FinTracApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Must run before any Firestore/Functions/Auth call so requests carry an App Check token
        AppCheckInstaller.install();

        // Apply saved theme on app start
        SettingHelper.applyTheme(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}