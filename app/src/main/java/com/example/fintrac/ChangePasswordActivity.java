package com.example.fintrac;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends BaseActivity {

    private ImageView btnBack;
    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private MaterialButton btnUpdatePassword;
    private LinearLayout passwordRequirementsContainer;
    private TextView confirmPasswordError;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 1. Initialize Views
        btnBack = findViewById(R.id.btnBack);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnUpdatePassword = findViewById(R.id.btnUpdatePassword);
        passwordRequirementsContainer = findViewById(R.id.password_requirements_container);
        confirmPasswordError = findViewById(R.id.confirm_password_error);

        // 2. Back Button Action
        btnBack.setOnClickListener(v -> finish());

        // Google sign-in accounts have no FinTrac password to change
        if (!hasPasswordLogin(mAuth.getCurrentUser())) {
            etCurrentPassword.setEnabled(false);
            etNewPassword.setEnabled(false);
            etConfirmPassword.setEnabled(false);
            btnUpdatePassword.setEnabled(false);
            btnUpdatePassword.setText("Managed by Google");
            Toast.makeText(this, "You sign in with Google, so your password is managed in your Google account.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Setup real-time password validation
        setupPasswordValidation();

        // 4. Setup confirm password validation
        setupConfirmPasswordValidation();

        // 5. Update Button Action
        btnUpdatePassword.setOnClickListener(v -> {
            String currentPass = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
            String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
            String confirmPass = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

            // Validation 1: Check if fields are empty
            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validation 2: Check if new password is the same as the old one
            if (newPass.equals(currentPass)) {
                Toast.makeText(this, "This was your previous password. Try again and create a new one.", Toast.LENGTH_LONG).show();
                return;
            }

            // Validation 3: Check if new passwords match
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validation 4: Check password strength
            if (!isValidPassword(newPass)) {
                Toast.makeText(this, "Password must meet all requirements shown below", Toast.LENGTH_LONG).show();
                // Show requirements if not already visible
                if (passwordRequirementsContainer != null) {
                    passwordRequirementsContainer.setVisibility(View.VISIBLE);
                }
                return;
            }

            // Kung pasado sa lahat ng validation, proceed sa Firebase Update
            updatePasswordInFirebase(currentPass, newPass);
        });
    }

    /**
     * Setup real-time password validation with visual indicators
     */
    private void setupPasswordValidation() {
        if (etNewPassword == null || passwordRequirementsContainer == null) return;

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                updatePasswordRequirements(password);

                // Also validate confirm password when password changes
                if (etConfirmPassword != null && etConfirmPassword.getText().length() > 0) {
                    validateConfirmPassword();
                }
            }
        });
    }

    /**
     * Update password requirements display with colors matching the button
     */
    private void updatePasswordRequirements(String password) {
        boolean hasLength = password.length() >= 8;
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        // Get colors from theme
        int validColor = getButtonColor(); // Primary button color for valid requirements
        int invalidColor = ContextCompat.getColor(this, android.R.color.darker_gray); // Gray for invalid

        // Update each requirement with checkmark and color
        updateRequirementText(R.id.req_length, getString(R.string.req_length), hasLength, validColor, invalidColor);
        updateRequirementText(R.id.req_uppercase, getString(R.string.req_uppercase), hasUppercase, validColor, invalidColor);
        updateRequirementText(R.id.req_lowercase, getString(R.string.req_lowercase), hasLowercase, validColor, invalidColor);
        updateRequirementText(R.id.req_number, getString(R.string.req_number), hasDigit, validColor, invalidColor);
        updateRequirementText(R.id.req_special, getString(R.string.req_special), hasSpecial, validColor, invalidColor);

        // Show/hide requirements based on whether password is empty
        if (password.isEmpty()) {
            passwordRequirementsContainer.setVisibility(View.GONE);
        } else {
            passwordRequirementsContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Get the button's color from theme attributes
     */
    private int getButtonColor() {
        // Try to get colorPrimary from theme
        android.util.TypedValue typedValue = new android.util.TypedValue();
        android.content.res.Resources.Theme theme = getTheme();
        if (theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            return typedValue.data;
        }
        // Fallback color
        return ContextCompat.getColor(this, R.color.blue);
    }

    /**
     * Helper method to update individual requirement text
     */
    private void updateRequirementText(int textViewId, String text, boolean isValid, int validColor, int invalidColor) {
        TextView requirementView = findViewById(textViewId);
        if (requirementView != null) {
            if (isValid) {
                requirementView.setText(text);
                requirementView.setTextColor(validColor);
            } else {
                requirementView.setText(text);
                requirementView.setTextColor(invalidColor);
            }
        }
    }

    /**
     * Setup confirm password validation with real-time checking
     */
    private void setupConfirmPasswordValidation() {
        if (etConfirmPassword == null) return;

        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateConfirmPassword();
            }
        });
    }

    /**
     * Validate that confirm password matches password
     */
    private void validateConfirmPassword() {
        String password = etNewPassword != null ? etNewPassword.getText().toString() : "";
        String confirmPassword = etConfirmPassword.getText().toString();

        if (confirmPasswordError == null) return;

        int validColor = getButtonColor();
        int invalidColor = ContextCompat.getColor(this, android.R.color.darker_gray);

        if (!confirmPassword.isEmpty()) {
            if (password.equals(confirmPassword)) {
                confirmPasswordError.setText(R.string.passwords_match);
                confirmPasswordError.setTextColor(validColor);
                confirmPasswordError.setVisibility(View.VISIBLE);
            } else {
                confirmPasswordError.setText(R.string.passwords_do_not_match);
                confirmPasswordError.setTextColor(invalidColor);
                confirmPasswordError.setVisibility(View.VISIBLE);
            }
        } else {
            confirmPasswordError.setVisibility(View.GONE);
        }
    }

    // Helper method para sa Strong Password Validation Rule
    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&   // At least one uppercase
                password.matches(".*[a-z].*") &&   // At least one lowercase
                password.matches(".*[0-9].*") &&   // At least one number
                password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*"); // At least one special character
    }

    // Helper method para sa Firebase Authentication at Update
    private void updatePasswordInFirebase(String currentPass, String newPass) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null && user.getEmail() != null) {
            // I-disable muna yung button para hindi ma-spam click
            btnUpdatePassword.setEnabled(false);
            btnUpdatePassword.setText("Updating...");

            // Step 1: Re-authenticate ang user (Requirement ng Firebase)
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Step 2: Pag successful ang re-auth, tsaka natin papalitan yung password
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        // I-enable ulit yung button at ibalik yung text
                        btnUpdatePassword.setEnabled(true);
                        btnUpdatePassword.setText(R.string.update_password);

                        if (updateTask.isSuccessful()) {
                            Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                            finish(); // I-close na natin yung activity pag success
                        } else {
                            Toast.makeText(this, "Failed to update password: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // Fail sa re-authentication (Mali ang current password)
                    btnUpdatePassword.setEnabled(true);
                    btnUpdatePassword.setText(R.string.update_password);
                    Toast.makeText(this, "Incorrect current password.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "No authenticated user found. Please log in again.", Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean hasPasswordLogin(FirebaseUser user) {
        if (user == null) return false;
        for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) return true;
        }
        return false;
    }
}
