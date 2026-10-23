package com.example.fintrac;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

// Debug builds: prints a debug token to Logcat on first run.
// Register it in Firebase Console > App Check > Apps > Manage debug tokens.
final class AppCheckInstaller {
    private AppCheckInstaller() {}

    static void install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
    }
}
