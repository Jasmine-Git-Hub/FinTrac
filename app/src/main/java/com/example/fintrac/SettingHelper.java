package com.example.fintrac;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class SettingHelper {

    private static final String PREFS_NAME = "AppSettings";
    private static final String THEME_PREF = "isDarkMode";
    private static final String COLOR_PREF = "themeColor";
    private static final String CURRENCY_PREF = "selectedCurrency";
    private static final String LAST_UPDATED_PREF = "LAST_UPDATED";
    private static final String OFFLINE_MODE_PREF = "offline_mode";

    // Default theme color
    private static final int DEFAULT_THEME_COLOR = R.color.ocean_blue;

    /**
     * Apply both dark mode and theme color to the activity
     */
    public static void applyTheme(Activity activity) {
        if (activity == null) return;

        try {
            // Apply dark mode
            boolean isDarkMode = isDarkMode(activity);
            AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

            // Apply theme color to status bar
            int savedColor = getThemeColor(activity);
            activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, savedColor));

            // Adjust light/dark status bar icons based on theme
            View decorView = activity.getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            if (isDarkMode) {
                // Dark mode - use light status bar icons (default)
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                // Light mode - use dark status bar icons
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            decorView.setSystemUiVisibility(flags);

        } catch (Exception e) {
            Log.e("SettingHelper", "Error applying theme: " + e.getMessage());
        }
    }

    /**
     * Simple apply theme for non-activity contexts
     */
    public static void applyTheme(Context context) {
        if (context == null) return;
        boolean isDarkMode = isDarkMode(context);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    // ==================== OFFLINE MODE METHODS ====================

    /**
     * Check if offline mode is enabled
     */
    public static boolean isOfflineModeEnabled(Context context) {
        if (context == null) return false;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(OFFLINE_MODE_PREF, false);
        } catch (Exception e) {
            Log.e("SettingHelper", "Error checking offline mode: " + e.getMessage());
            return false;
        }
    }

    /**
     * Save offline mode setting
     */
    public static void saveOfflineMode(Context context, boolean enabled) {
        if (context == null) return;

        try {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit();
            editor.putBoolean(OFFLINE_MODE_PREF, enabled);
            editor.apply();

            Log.d("SettingHelper", "Offline mode saved: " + enabled);
        } catch (Exception e) {
            Log.e("SettingHelper", "Error saving offline mode: " + e.getMessage());
        }
    }

    /**
     * Check if device is connected to the internet (respects offline mode setting)
     */
    public static boolean isOnline(Context context) {
        if (context == null) return false;

        // If offline mode is enabled, always return false (pretend we're offline)
        if (isOfflineModeEnabled(context)) {
            return false;
        }

        // Otherwise, check actual network connection
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;

            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        } catch (Exception e) {
            Log.e("SettingHelper", "Error checking network: " + e.getMessage());
            return false;
        }
    }

    // ==================== EXISTING METHODS ====================

    /**
     * Check if dark mode is enabled
     */
    public static boolean isDarkMode(Context context) {
        if (context == null) return false;

        try {
            // Check SharedPreferences first
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean savedMode = prefs.getBoolean(THEME_PREF, false);

            // If not saved, check system default
            if (!prefs.contains(THEME_PREF)) {
                int currentNightMode = context.getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK;
                return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
            }

            return savedMode;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Save dark mode setting
     */
    public static void saveDarkModeSetting(Context context, boolean isDarkMode) {
        if (context == null) return;

        try {
            SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit();
            editor.putBoolean(THEME_PREF, isDarkMode);
            editor.apply();

            // Apply immediately
            AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } catch (Exception e) {
            Log.e("SettingHelper", "Error saving dark mode: " + e.getMessage());
        }
    }

    /**
     * Get saved theme color
     */
    public static int getThemeColor(Context context) {
        if (context == null) return DEFAULT_THEME_COLOR;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getInt(COLOR_PREF, DEFAULT_THEME_COLOR);
        } catch (Exception e) {
            return DEFAULT_THEME_COLOR;
        }
    }

    /**
     * Save theme color
     */
    public static void saveThemeColor(Context context, int colorResId) {
        if (context == null) return;

        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putInt(COLOR_PREF, colorResId).apply();
        } catch (Exception e) {
            Log.e("SettingHelper", "Error saving theme color: " + e.getMessage());
        }
    }

    /**
     * Get theme color name for display
     */
    public static String getThemeColorName(Context context) {
        int colorResId = getThemeColor(context);

        if (colorResId == R.color.ocean_blue) {
            return "Ocean Blue";
        } else if (colorResId == R.color.forest_green) {
            return "Forest Green";
        } else if (colorResId == R.color.royal_purple) {
            return "Royal Purple";
        } else if (colorResId == R.color.sunset_orange) {
            return "Sunset Orange";
        }

        return "Ocean Blue";
    }

    // ==================== CURRENCY METHODS ====================

    /**
     * Save selected currency
     */
    public static void saveCurrency(Context context, String currencyName) {
        if (context == null || currencyName == null) return;

        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(CURRENCY_PREF, currencyName).apply();
        } catch (Exception e) {
            Log.e("SettingHelper", "Error saving currency: " + e.getMessage());
        }
    }

    /**
     * Get selected currency
     */
    public static String getCurrency(Context context) {
        if (context == null) return "Philippine Peso (PHP)";

        try {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(CURRENCY_PREF, "Philippine Peso (PHP)");
        } catch (Exception e) {
            return "Philippine Peso (PHP)";
        }
    }

    /**
     * Update exchange rates if needed (called periodically)
     * Respects offline mode setting
     */
    public static void updateRatesIfNecessary(Context context) {
        if (context == null) return;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            // Skip if offline mode is enabled
            if (isOfflineModeEnabled(context)) {
                Log.d("SettingHelper", "Offline mode enabled - skipping rate update");
                return;
            }

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) return;

            NetworkInfo net = cm.getActiveNetworkInfo();

            if (net != null && net.isConnected() &&
                    (System.currentTimeMillis() - prefs.getLong(LAST_UPDATED_PREF, 0) > 3600000)) {
                fetchRealTimeRates(context);
            }
        } catch (Exception e) {
            Log.e("SettingHelper", "Error checking rates update: " + e.getMessage());
        }
    }

    /**
     * Fetch real-time exchange rates from API
     * Now respects offline mode (won't be called if offline mode is enabled)
     */
    private static void fetchRealTimeRates(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL("https://open.er-api.com/v6/latest/PHP");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) res.append(line);
                    in.close();

                    JSONObject rates = new JSONObject(res.toString()).getJSONObject("rates");
                    SharedPreferences.Editor ed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();

                    String[] codes = {"USD", "EUR", "JPY", "GBP", "KRW", "SGD", "AUD", "CAD", "CNY", "INR", "CHF", "NZD", "THB", "VND"};
                    for (String c : codes) {
                        ed.putFloat("RATE_" + c, (float) rates.optDouble(c, 1.0));
                    }

                    ed.putLong(LAST_UPDATED_PREF, System.currentTimeMillis()).apply();
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("SettingHelper", "API fetch failed", e);
            }
        });
    }

    /**
     * Get exchange rate for a specific currency
     */
    public static double getExchangeRate(Context context, String currencyName) {
        if (currencyName == null || context == null) return 1.0;

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            // Extract currency code from string like "Philippine Peso (PHP)"
            int startIdx = currencyName.indexOf("(");
            int endIdx = currencyName.indexOf(")");

            if (startIdx == -1 || endIdx == -1) return 1.0;

            String code = currencyName.substring(startIdx + 1, endIdx);

            // Always return 1.0 for PHP (base currency)
            if ("PHP".equals(code)) return 1.0;

            return prefs.getFloat("RATE_" + code, 1.0f);
        } catch (Exception e) {
            Log.e("SettingHelper", "Error getting exchange rate: " + e.getMessage());
            return 1.0;
        }
    }

    /**
     * Get currency symbol based on currency name
     */
    public static String getCurrencySymbol(String currencyName) {
        if (currencyName == null) return "₱";

        try {
            if (currencyName.contains("USD") || currencyName.contains("SGD") ||
                    currencyName.contains("AUD") || currencyName.contains("CAD") ||
                    currencyName.contains("NZD")) return "$";
            if (currencyName.contains("EUR")) return "€";
            if (currencyName.contains("JPY") || currencyName.contains("CNY")) return "¥";
            if (currencyName.contains("GBP")) return "£";
            if (currencyName.contains("KRW")) return "₩";
            if (currencyName.contains("INR")) return "₹";
            if (currencyName.contains("THB")) return "฿";
            if (currencyName.contains("VND")) return "₫";
        } catch (Exception e) {
            Log.e("SettingHelper", "Error getting currency symbol: " + e.getMessage());
        }

        return "₱";
    }

    /**
     * Format amount with currency symbol based on selected currency
     */
    public static String formatAmount(Context context, double amountInPHP) {
        if (context == null) return "₱0.00";

        try {
            String curr = getCurrency(context);
            double converted = amountInPHP * getExchangeRate(context, curr);
            return getCurrencySymbol(curr) + String.format(java.util.Locale.getDefault(), "%,.2f", converted);
        } catch (Exception e) {
            Log.e("SettingHelper", "Error formatting amount: " + e.getMessage());
            return "₱" + String.format(java.util.Locale.getDefault(), "%,.2f", amountInPHP);
        }
    }

    /**
     * Convert amount from selected currency to base PHP
     */
    public static double convertToBasePHP(Context context, double inputAmount) {
        if (context == null) return inputAmount;

        try {
            String curr = getCurrency(context);
            double rate = getExchangeRate(context, curr);

            // Avoid division by zero
            if (rate == 0) return inputAmount;

            return inputAmount / rate;
        } catch (Exception e) {
            Log.e("SettingHelper", "Error converting to base PHP: " + e.getMessage());
            return inputAmount;
        }
    }

    /**
     * Apply theme color to activity
     */
    public static void applyThemeColor(Activity activity) {
        if (activity == null) return;

        try {
            int themeColor = getThemeColor(activity);

            // Set status bar color
            activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, themeColor));

            // Set navigation bar color (for newer Android versions)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setNavigationBarColor(ContextCompat.getColor(activity, themeColor));
            }

            // Update system UI for light/dark icons based on background color
            updateSystemUiColors(activity, ContextCompat.getColor(activity, themeColor));

        } catch (Exception e) {
            Log.e("SettingHelper", "Error applying theme color: " + e.getMessage());
        }
    }

    // Helper method for UI colors
    private static void updateSystemUiColors(Activity activity, int backgroundColor) {
        // Calculate if background is light or dark
        boolean isLightBackground = isLightColor(backgroundColor);

        View decorView = activity.getWindow().getDecorView();
        int flags = decorView.getSystemUiVisibility();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (isLightBackground) {
                // Light background - use dark status bar icons
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                // Dark background - use light status bar icons
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
        }

        decorView.setSystemUiVisibility(flags);
    }

    // Helper method to determine if color is light
    private static boolean isLightColor(int color) {
        // Calculate brightness - if > 128, it's considered light
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        double brightness = (0.299 * red + 0.587 * green + 0.114 * blue);
        return brightness > 128;
    }

    // Overloaded method for fragments/context
    public static void applyThemeColor(Context context) {
        if (context == null || !(context instanceof Activity)) return;
        applyThemeColor((Activity) context);
    }
}