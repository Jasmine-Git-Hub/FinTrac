package com.example.fintrac;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class dashboard extends BaseActivity implements TabNavigator {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme BEFORE super.onCreate and setContentView
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (bottomNavigationView != null) {
            // Apply theme colors to bottom navigation immediately
            applyThemeToBottomNav();

            // Listener for bottom navigation
            bottomNavigationView.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;

                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.navigation_jars) {
                    selectedFragment = new JarsFragment();
                } else if (itemId == R.id.navigation_bills) {
                    selectedFragment = new Bills();
                } else if (itemId == R.id.navigation_challenges) {
                    selectedFragment = new Challenge();
                } else if (itemId == R.id.navigation_history) {
                    selectedFragment = new HistoryFragment();
                } else if (itemId == R.id.navigation_settings) {
                    selectedFragment = new settings();
                }

                if (selectedFragment != null) {
                    loadFragment(selectedFragment);
                    return true;
                } else {
                    showToast(getString(R.string.fragment_not_found));
                    return false;
                }
            });
        }

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply theme when returning to this activity
        SettingHelper.applyTheme(this);
        applyThemeToBottomNav();
    }

    private void applyThemeToBottomNav() {
        try {
            int themeColorResId = SettingHelper.getThemeColor(this);
            ThemeColorManager.applyThemeColor(this, themeColorResId);
        } catch (Exception e) {
            Log.e("dashboard", "Error applying theme to bottom nav: " + e.getMessage());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Method to programmatically select bottom navigation item
     * This is called from fragments like HomeFragment
     */
    @Override
    public void selectBottomNavItem(int menuItemId) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(menuItemId);
        }
    }
}