package com.example.fintrac;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * The user's "Push Notifications" choice plus Android's runtime permission (Android 13+).
 * Every reminder source checks {@link #canNotify} before posting.
 */
public final class NotificationPrefs {

    private static final String PREFS = "SettingsPrefs";
    private static final String KEY_ENABLED = "push_notifications_enabled";

    private NotificationPrefs() {}

    public static boolean isEnabledByUser(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabledByUser(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean hasSystemPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /** True only if the user wants reminders and Android allows showing them. */
    public static boolean canNotify(Context context) {
        return isEnabledByUser(context) && hasSystemPermission(context);
    }

    /** Asks for the Android 13+ permission if the user wants notifications and it isn't granted. */
    public static void requestIfNeeded(Context context, ActivityResultLauncher<String> launcher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && isEnabledByUser(context) && !hasSystemPermission(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /** Opens this app's notification settings (for when the permission was permanently denied). */
    public static void openSystemSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
