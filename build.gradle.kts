plugins {
    alias(libs.plugins.android.application) apply false
    // Now this line will work because we added it to the TOML file
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
}