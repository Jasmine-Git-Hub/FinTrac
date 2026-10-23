package com.example.fintrac;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class settings extends Fragment {

    private static final String TAG = "SettingsFragment";

    // Views
    private TextView tvSelectedCurrency;
    private MaterialCardView cardCurrencySelector;
    private MaterialCardView btnLogout;
    private MaterialCardView btnArchives;
    private MaterialCardView btnAbout;
    private TextView btnEditProfile, btnChangePassword;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchPushNotifications;
    private SwitchMaterial switchOfflineMode;

    // Theme Buttons
    private Button btnOceanBlue, btnForestGreen, btnRoyalPurple, btnSunsetOrange;

    // About Content
    private View aboutContent;

    // Currency Data
    private final String[] currencies = {
            "Philippine Peso (PHP)", "US Dollar (USD)", "Euro (EUR)",
            "Japanese Yen (JPY)", "British Pound (GBP)", "South Korean Won (KRW)",
            "Singapore Dollar (SGD)", "Australian Dollar (AUD)", "Canadian Dollar (CAD)",
            "Chinese Yuan (CNY)", "Indian Rupee (INR)", "Swiss Franc (CHF)",
            "New Zealand Dollar (NZD)", "Thai Baht (THB)", "Vietnamese Dong (VND)"
    };

    private int selectedCurrencyIndex = 0;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase Auth and Firestore safely
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error: " + e.getMessage());
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // 1. Initialize Views with null checks
        initializeViews(view);

        // 2. Load saved preferences only if context exists
        if (getContext() != null) {
            loadSavedPreferences();
        }

        // 3. Setup click listeners
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        try {
            tvSelectedCurrency = view.findViewById(R.id.tvSelectedCurrency);
            cardCurrencySelector = view.findViewById(R.id.cardCurrencySelector);
            btnLogout = view.findViewById(R.id.btnLogout);
            btnEditProfile = view.findViewById(R.id.btnEditProfile);
            btnChangePassword = view.findViewById(R.id.btnChangePassword);
            switchDarkMode = view.findViewById(R.id.switchDarkMode);
            switchPushNotifications = view.findViewById(R.id.switchNotifications);
            switchOfflineMode = view.findViewById(R.id.switchOfflineMode);
            btnArchives = view.findViewById(R.id.btnArchives);
            btnAbout = view.findViewById(R.id.btnAbout);
            aboutContent = view.findViewById(R.id.aboutContent);

            // Initialize Theme Color Buttons
            btnOceanBlue = view.findViewById(R.id.btnOceanBlue);
            btnForestGreen = view.findViewById(R.id.btnForestGreen);
            btnRoyalPurple = view.findViewById(R.id.btnRoyalPurple);
            btnSunsetOrange = view.findViewById(R.id.btnSunsetOrange);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
        }
    }

    private void loadSavedPreferences() {
        Context context = getContext();
        if (context == null) return;

        try {
            // Load currency
            String savedCurrency = SettingHelper.getCurrency(context);
            if (tvSelectedCurrency != null) {
                tvSelectedCurrency.setText(savedCurrency);
            }

            for (int i = 0; i < currencies.length; i++) {
                if (currencies[i].equals(savedCurrency)) {
                    selectedCurrencyIndex = i;
                    break;
                }
            }

            // Load dark mode
            if (switchDarkMode != null) {
                boolean isDark = SettingHelper.isDarkMode(context);
                switchDarkMode.setChecked(isDark);
            }

            // Load offline mode
            if (switchOfflineMode != null) {
                boolean isOfflineEnabled = SettingHelper.isOfflineModeEnabled(context);
                switchOfflineMode.setChecked(isOfflineEnabled);
            }

            // Load notifications setting
            if (switchPushNotifications != null) {
                SharedPreferences prefs = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE);
                boolean isPushEnabled = prefs.getBoolean("push_notifications_enabled", true);
                switchPushNotifications.setChecked(isPushEnabled);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading preferences: " + e.getMessage());
        }
    }

    private void setupClickListeners() {
        Context context = getContext();
        if (context == null) return;

        // Dark Mode Listener
        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Context ctx = getContext();
                if (ctx != null) {
                    SettingHelper.saveDarkModeSetting(ctx, isChecked);
                    showToast(ctx, isChecked ? "Dark mode enabled" : "Light mode enabled");
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                }
            });
        }

        // OFFLINE MODE LISTENER
        if (switchOfflineMode != null) {
            switchOfflineMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Context ctx = getContext();
                if (ctx != null) {
                    try {
                        // Save to SettingHelper first
                        SettingHelper.saveOfflineMode(ctx, isChecked);

                        // Check if Firestore is initialized
                        if (db == null) {
                            try {
                                db = FirebaseFirestore.getInstance();
                            } catch (Exception e) {
                                Log.e(TAG, "Firestore not available: " + e.getMessage());
                                showToast(ctx, "Offline mode saved (Firebase unavailable)");
                                return;
                            }
                        }

                        if (isChecked) {
                            // ENABLE OFFLINE MODE - Disable network
                            db.disableNetwork().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    showToast(ctx, "Offline mode enabled. Data will be saved locally.");
                                    Log.d(TAG, "Firestore Network Disabled.");
                                } else {
                                    Log.e(TAG, "Failed to disable network", task.getException());
                                    showToast(ctx, "Offline mode saved but network may still be active");
                                }
                            });
                        } else {
                            // DISABLE OFFLINE MODE - Enable network and sync
                            db.enableNetwork().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    showToast(ctx, "Back Online. Syncing data...");
                                    Log.d(TAG, "Firestore Network Enabled. Syncing...");
                                } else {
                                    Log.e(TAG, "Failed to enable network", task.getException());
                                    showToast(ctx, "Failed to reconnect. Please check your connection.");
                                }
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error toggling offline mode: " + e.getMessage());
                        showToast(ctx, "Error updating offline mode");
                    }
                }
            });
        }

        // Currency Selector
        if (cardCurrencySelector != null) {
            cardCurrencySelector.setOnClickListener(v -> showCurrencyDialog());
        }

        // Archives
        if (btnArchives != null) {
            btnArchives.setOnClickListener(v -> {
                try {
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), ArchivesActivity.class);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Archives: " + e.getMessage());
                    showToast(getContext(), "Unable to open Archives");
                }
            });
        }

        // About (Expandable)
        if (btnAbout != null && aboutContent != null) {
            btnAbout.setOnClickListener(v -> {
                if (aboutContent.getVisibility() == View.GONE) {
                    aboutContent.setVisibility(View.VISIBLE);
                } else {
                    aboutContent.setVisibility(View.GONE);
                }
            });
        }

        // Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        }

        // Edit Profile
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                try {
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Edit Profile: " + e.getMessage());
                    showToast(getContext(), "Edit Profile not available");
                }
            });
        }

        // Change Password
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                try {
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error opening Change Password: " + e.getMessage());
                    showToast(getContext(), "Change Password not available");
                }
            });
        }

        // Push Notifications
        if (switchPushNotifications != null) {
            switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Context ctx = getContext();
                if (ctx != null) {
                    try {
                        SharedPreferences prefs = ctx.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putBoolean("push_notifications_enabled", isChecked).apply();
                        showToast(ctx, "Push Notifications " + (isChecked ? "enabled" : "disabled"));
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving notification setting: " + e.getMessage());
                    }
                }
            });
        }

        // Theme Color Buttons
        setupColorButton(btnOceanBlue, R.color.ocean_blue, "Ocean Blue");
        setupColorButton(btnForestGreen, R.color.forest_green, "Forest Green");
        setupColorButton(btnRoyalPurple, R.color.royal_purple, "Royal Purple");
        setupColorButton(btnSunsetOrange, R.color.sunset_orange, "Sunset Orange");
    }

    private void setupColorButton(Button btn, int colorResId, String colorName) {
        if (btn == null) return;

        btn.setOnClickListener(v -> {
            Context context = getContext();
            if (context != null) {
                try {
                    // Save using SettingHelper
                    SettingHelper.saveThemeColor(context, colorResId);
                    showToast(context, "Theme set to " + colorName);

                    if (getActivity() != null) {
                        // Apply theme color immediately using ThemeColorManager
                        ThemeColorManager.applyThemeColor(getActivity(), colorResId);

                        // Recreate activity to apply all theme changes
                        getActivity().recreate();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error setting theme color: " + e.getMessage());
                }
            }
        });
    }

    private void showCurrencyDialog() {
        Context context = getContext();
        if (context == null) return;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Select Currency");

            builder.setSingleChoiceItems(currencies, selectedCurrencyIndex, (dialog, which) -> {
                Context ctx = getContext();
                if (ctx == null) return;

                selectedCurrencyIndex = which;
                String selected = currencies[which];

                if (tvSelectedCurrency != null) {
                    tvSelectedCurrency.setText(selected);
                }

                SettingHelper.saveCurrency(ctx, selected);

                // Update exchange rates when currency changes
                SettingHelper.updateRatesIfNecessary(ctx);

                showToast(ctx, "Currency updated to " + selected);
                dialog.dismiss();
            });

            builder.setNegativeButton("Cancel", null);
            builder.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing currency dialog: " + e.getMessage());
        }
    }

    private void showLogoutConfirmation() {
        Context context = getContext();
        if (context == null) return;

        try {
            new AlertDialog.Builder(context)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        try {
                            // Sign out from Firebase
                            if (mAuth != null) {
                                mAuth.signOut();
                                GoogleCredentialHelper.clearState(requireContext());
                                OtpManager.clearSession(requireContext());
                            }

                            showToast(getContext(), "Logged out successfully");

                            if (getActivity() != null) {
                                Intent intent = new Intent(getActivity(), LogIn.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                getActivity().finish();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error during logout: " + e.getMessage());
                            showToast(getContext(), "Error during logout");
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing logout dialog: " + e.getMessage());
        }
    }

    private void showToast(Context context, String message) {
        if (context != null && message != null) {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast: " + e.getMessage());
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment resumes
        if (getContext() != null && isAdded()) {
            loadSavedPreferences();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear references to avoid memory leaks
        tvSelectedCurrency = null;
        cardCurrencySelector = null;
        btnLogout = null;
        btnArchives = null;
        btnAbout = null;
        btnEditProfile = null;
        btnChangePassword = null;
        switchDarkMode = null;
        switchPushNotifications = null;
        switchOfflineMode = null;
        aboutContent = null;
        btnOceanBlue = null;
        btnForestGreen = null;
        btnRoyalPurple = null;
        btnSunsetOrange = null;
    }
}