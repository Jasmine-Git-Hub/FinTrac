package com.example.fintrac;

import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

// Release builds: attest the app with Play Integrity so only genuine installs reach Firebase.
final class AppCheckInstaller {
    private AppCheckInstaller() {}

    static void install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
