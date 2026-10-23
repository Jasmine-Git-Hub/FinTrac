package com.example.fintrac;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ThemeColorManager {

    private static final String TAG = "ThemeColorManager";

    public static void applyThemeColor(Activity activity, @ColorRes int colorResId) {
        if (activity == null) return;

        try {
            int color = ContextCompat.getColor(activity, colorResId);

            // Set status bar color
            activity.getWindow().setStatusBarColor(color);

            // Set navigation bar color (for newer Android versions)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setNavigationBarColor(color);
            }

            // Update action bar if exists
            if (activity instanceof AppCompatActivity) {
                ActionBar actionBar = ((AppCompatActivity) activity).getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setBackgroundDrawable(new ColorDrawable(color));
                }
            }

            // Apply to bottom navigation if it exists
            applyToBottomNavigation(activity, color);

            // Update system UI for light/dark icons based on background color
            updateSystemUiColors(activity, color);

        } catch (Exception e) {
            Log.e(TAG, "Error applying theme color: " + e.getMessage());
        }
    }

    private static void applyToBottomNavigation(Activity activity, int themeColor) {
        try {
            BottomNavigationView bottomNav = activity.findViewById(R.id.bottom_navigation);

            if (bottomNav != null) {
                // Get the current dark mode setting
                boolean isDarkMode = SettingHelper.isDarkMode(activity);

                // Create color state list for selected/unselected states
                int[][] states = new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                };

                int unselectedColor;
                if (isDarkMode) {
                    unselectedColor = Color.parseColor("#9EA3AE"); // Light gray for dark mode
                } else {
                    unselectedColor = ContextCompat.getColor(activity, R.color.gray); // Gray for light mode
                }

                int[] colors = new int[]{
                        themeColor,
                        unselectedColor
                };

                ColorStateList colorStateList = new ColorStateList(states, colors);

                // Apply to bottom navigation
                bottomNav.setItemIconTintList(colorStateList);
                bottomNav.setItemTextColor(colorStateList);

                Log.d(TAG, "Bottom navigation colors updated to: " + Integer.toHexString(themeColor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error applying to bottom nav: " + e.getMessage());
        }
    }

    private static void updateSystemUiColors(Activity activity, int backgroundColor) {
        // Calculate if background is light or dark
        boolean isLightBackground = isLightColor(backgroundColor);

        View decorView = activity.getWindow().getDecorView();
        int flags = decorView.getSystemUiVisibility();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

    private static boolean isLightColor(int color) {
        // Calculate brightness - if > 128, it's considered light
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        double brightness = (0.299 * red + 0.587 * green + 0.114 * blue);
        return brightness > 128;
    }

    public static int getThemeColorResId(Context context, String themeName) {
        if (themeName == null) return R.color.ocean_blue;

        switch (themeName) {
            case "Ocean Blue":
                return R.color.ocean_blue;
            case "Forest Green":
                return R.color.forest_green;
            case "Royal Purple":
                return R.color.royal_purple;
            case "Sunset Orange":
                return R.color.sunset_orange;
            default:
                return R.color.ocean_blue;
        }
    }
}