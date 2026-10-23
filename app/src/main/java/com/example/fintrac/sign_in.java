package com.example.fintrac;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

public class sign_in extends AppCompatActivity {

    EditText passwordInput, confirmPasswordInput, emailInput, fullNameInput;
    CheckBox termsCheckbox;
    ImageView eyePassword, eyeConfirm;
    LinearLayout passwordRequirementsContainer;
    TextView confirmPasswordError, errorTextView;
    ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirestoreHelper firestoreHelper;
    private MaterialButton googleButton;
    private Button createAccountButton;

    boolean isPasswordVisible = false;
    boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        firestoreHelper = FirestoreHelper.getInstance();

        initializeViews();
        setupListeners();
        setupPasswordValidation();
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.email_input);
        fullNameInput = findViewById(R.id.full_name_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        termsCheckbox = findViewById(R.id.terms_checkbox);
        passwordRequirementsContainer = findViewById(R.id.password_requirements_container);
        confirmPasswordError = findViewById(R.id.confirm_password_error);
        googleButton = findViewById(R.id.google_button);
        createAccountButton = findViewById(R.id.create_account_button);
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);
    }

    private void setupListeners() {
        // Back button
        TextView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Make "Sign in" text blue and clickable
        setupSignInLink();

        // Create account button
        if (createAccountButton != null) {
            createAccountButton.setOnClickListener(v -> registerUser());
        }

        // Google Sign-In button
        if (googleButton != null) {
            googleButton.setOnClickListener(v -> signInWithGoogle());
        }

        // Setup Terms checkbox with clickable link
        setupTermsCheckbox();

        // Setup password toggles
        setupPasswordToggles();

        // Setup confirm password validation
        setupConfirmPasswordValidation();
    }

    private void setupSignInLink() {
        TextView tvHaveAccount = findViewById(R.id.tvHaveAccount);
        if (tvHaveAccount != null) {
            String text = "I have an account? Sign in";
            SpannableString spannable = new SpannableString(text);

            int blueColor = ContextCompat.getColor(this, R.color.blue);

            int signInStart = text.indexOf("Sign in");
            int signInEnd = signInStart + "Sign in".length();

            ClickableSpan signInSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Intent intent = new Intent(sign_in.this, LogIn.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(blueColor);
                    ds.setUnderlineText(false);
                }
            };

            spannable.setSpan(signInSpan, signInStart, signInEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvHaveAccount.setText(spannable);
            tvHaveAccount.setMovementMethod(LinkMovementMethod.getInstance());
            tvHaveAccount.setHighlightColor(android.graphics.Color.TRANSPARENT);
        }
    }

    private void setupTermsCheckbox() {
        if (termsCheckbox == null) return;

        String fullText = getString(R.string.i_agree_to_the_terms_conditions_and_privacy_policy);
        SpannableString spannable = new SpannableString(fullText);

        int blueColor = ContextCompat.getColor(this, R.color.blue);

        // Find the clickable text range
        int termsStart = fullText.indexOf("Terms & Conditions");
        int privacyEnd = fullText.indexOf("Privacy Policy") + "Privacy Policy".length();

        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Open the Terms & Privacy dialog
                TermsAndConditions.showTermsDialog(sign_in.this, termsCheckbox);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(blueColor);
                ds.setUnderlineText(false);
            }
        };

        spannable.setSpan(termsSpan, termsStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        termsCheckbox.setText(spannable);
        termsCheckbox.setMovementMethod(LinkMovementMethod.getInstance());
        termsCheckbox.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void setupPasswordToggles() {
        eyePassword = findViewById(R.id.imageView6);
        eyeConfirm = findViewById(R.id.imageView4);

        if (eyePassword != null && passwordInput != null) {
            eyePassword.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    eyePassword.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    eyePassword.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                passwordInput.setSelection(passwordInput.getText().length());
                isPasswordVisible = !isPasswordVisible;
            });
        }

        if (eyeConfirm != null && confirmPasswordInput != null) {
            eyeConfirm.setOnClickListener(v -> {
                if (isConfirmPasswordVisible) {
                    confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    eyeConfirm.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    eyeConfirm.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                confirmPasswordInput.setSelection(confirmPasswordInput.getText().length());
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
            });
        }
    }

    private void setupPasswordValidation() {
        if (passwordInput == null || passwordRequirementsContainer == null) return;

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String password = s.toString();
                updatePasswordRequirements(password);

                // Also validate confirm password when password changes
                if (confirmPasswordInput != null && confirmPasswordInput.getText().length() > 0) {
                    validateConfirmPassword();
                }
            }
        });
    }

    private void updatePasswordRequirements(String password) {
        boolean hasLength = password.length() >= 8;
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        int validColor = getButtonColor();
        int invalidColor = ContextCompat.getColor(this, android.R.color.darker_gray);

        updateRequirementText(R.id.req_length, getString(R.string.req_length), hasLength, validColor, invalidColor);
        updateRequirementText(R.id.req_uppercase, getString(R.string.req_uppercase), hasUppercase, validColor, invalidColor);
        updateRequirementText(R.id.req_lowercase, getString(R.string.req_lowercase), hasLowercase, validColor, invalidColor);
        updateRequirementText(R.id.req_number, getString(R.string.req_number), hasDigit, validColor, invalidColor);
        updateRequirementText(R.id.req_special, getString(R.string.req_special), hasSpecial, validColor, invalidColor);

        if (password.isEmpty()) {
            passwordRequirementsContainer.setVisibility(View.GONE);
        } else {
            passwordRequirementsContainer.setVisibility(View.VISIBLE);
        }
    }

    private int getButtonColor() {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        android.content.res.Resources.Theme theme = getTheme();
        if (theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            return typedValue.data;
        }
        return ContextCompat.getColor(this, R.color.blue);
    }

    private void updateRequirementText(int textViewId, String text, boolean isValid, int validColor, int invalidColor) {
        TextView requirementView = findViewById(textViewId);
        if (requirementView != null) {
            if (isValid) {
                requirementView.setText("✓ " + text);
                requirementView.setTextColor(validColor);
            } else {
                requirementView.setText("✗ " + text);
                requirementView.setTextColor(invalidColor);
            }
        }
    }

    private void setupConfirmPasswordValidation() {
        if (confirmPasswordInput == null) return;

        confirmPasswordInput.addTextChangedListener(new TextWatcher() {
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

    private void validateConfirmPassword() {
        String password = passwordInput != null ? passwordInput.getText().toString() : "";
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (confirmPasswordError == null) return;

        int validColor = getButtonColor();
        int invalidColor = ContextCompat.getColor(this, android.R.color.darker_gray);

        if (!confirmPassword.isEmpty()) {
            if (password.equals(confirmPassword)) {
                confirmPasswordError.setText("✓ " + getString(R.string.passwords_match));
                confirmPasswordError.setTextColor(validColor);
                confirmPasswordError.setVisibility(View.VISIBLE);
            } else {
                confirmPasswordError.setText("✗ " + getString(R.string.passwords_do_not_match));
                confirmPasswordError.setTextColor(invalidColor);
                confirmPasswordError.setVisibility(View.VISIBLE);
            }
        } else {
            confirmPasswordError.setVisibility(View.GONE);
        }
    }

    private void signInWithGoogle() {
        // Show loading indicator
        setLoadingState(true);

        GoogleCredentialHelper.signIn(this, new GoogleCredentialHelper.Callback() {
            @Override
            public void onIdToken(String idToken) {
                firebaseAuthWithGoogle(idToken);
            }

            @Override
            public void onCancelled() {
                setLoadingState(false);
            }

            @Override
            public void onError(String message) {
                setLoadingState(false);
                showError(getString(R.string.google_sign_in_failed) + message);
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String fullName = user.getDisplayName();
                            String email = user.getEmail();

                            // Check if user already exists in Firestore
                            firestoreHelper.checkUserExists(user.getUid(), new FirestoreHelper.OnUserExistsListener() {
                                @Override
                                public void onResult(boolean exists) {
                                    if (!exists) {
                                        // New user - save to Firestore
                                        String nameToSave = (fullName != null && !fullName.isEmpty()) ? fullName : email.split("@")[0];
                                        saveUserToFirestore(user, nameToSave);
                                    } else {
                                        // Existing user - just show success and navigate
                                        runOnUiThread(() -> {
                                            setLoadingState(false);
                                            Toast.makeText(sign_in.this,
                                                    getString(R.string.google_sign_in_success),
                                                    Toast.LENGTH_SHORT).show();
                                            navigateToDashboard();
                                        });
                                    }
                                }

                                @Override
                                public void onFailure(String error) {
                                    // Even if check fails, try to save or navigate
                                    String nameToSave = (fullName != null && !fullName.isEmpty()) ? fullName : email.split("@")[0];
                                    saveUserToFirestore(user, nameToSave);
                                }
                            });
                        }
                    } else {
                        setLoadingState(false);
                        String errorMsg = getFriendlyErrorMessage(task.getException());
                        showError(getString(R.string.google_sign_in_failed) + errorMsg);
                    }
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (createAccountButton != null) {
            createAccountButton.setEnabled(!isLoading);
            createAccountButton.setText(isLoading ? getString(R.string.authenticating) : getString(R.string.create_account));
        }

        if (googleButton != null) {
            googleButton.setEnabled(!isLoading);
        }

        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (errorTextView != null && !isLoading) {
            errorTextView.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        if (errorTextView != null) {
            errorTextView.setText(message);
            errorTextView.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String fullName = fullNameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        boolean acceptedTerms = termsCheckbox.isChecked();

        if (!validateInputs(email, fullName, password, confirmPassword, acceptedTerms)) {
            return;
        }

        // Prove the user owns this email (EmailJS OTP) before creating the account
        setLoadingState(true);

        EmailOtpSender.send(email, OtpManager.issue(email), new EmailOtpSender.Callback() {
            @Override
            public void onSuccess() {
                setLoadingState(false);
                showOTPDialog(email, fullName, password);
            }

            @Override
            public void onFailure(String error) {
                setLoadingState(false);
                showError(EmailOtpSender.describeFailure(error));
            }
        });
    }

    private void showOTPDialog(String email, String fullName, String password) {
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.enter_6_digit_otp);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(6)});

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = 50;
        params.rightMargin = 50;
        input.setLayoutParams(params);
        container.addView(input);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.verify_your_email)
                .setMessage(getString(R.string.otp_message, email))
                .setView(container)
                .setPositiveButton(R.string.verify, null)
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false)
                .create();

        // Custom click handler so a wrong code keeps the dialog open
        dialog.setOnShowListener(d -> dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    switch (OtpManager.verify(email, input.getText().toString())) {
                        case OK:
                            dialog.dismiss();
                            Toast.makeText(this, R.string.email_verified_successfully, Toast.LENGTH_SHORT).show();
                            createFirebaseAccount(email, fullName, password);
                            break;
                        case WRONG:
                            input.setError(getString(R.string.incorrect_otp)
                                    + " (" + OtpManager.attemptsLeft() + " " + getString(R.string.attempts_left) + ")");
                            break;
                        default:
                            dialog.dismiss();
                            showError(getString(R.string.otp_expired_request_new));
                            break;
                    }
                }));
        dialog.show();
    }

    private void createFirebaseAccount(String email, String fullName, String password) {
        setLoadingState(true);

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                updateUserProfile(mAuth.getCurrentUser(), fullName);
            } else {
                setLoadingState(false);
                showError(getFriendlyErrorMessage(task.getException()));
            }
        });
    }

    private boolean validateInputs(String email, String fullName, String password,
                                   String confirmPassword, boolean acceptedTerms) {
        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.error_email_required));
            emailInput.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.error_valid_email));
            emailInput.requestFocus();
            return false;
        }
        if (fullName.isEmpty()) {
            fullNameInput.setError(getString(R.string.error_name_required));
            fullNameInput.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.error_password_required));
            passwordInput.requestFocus();
            return false;
        }

        if (!isValidPassword(password)) {
            passwordInput.setError(getString(R.string.error_weak_password));
            passwordInput.requestFocus();
            if (passwordRequirementsContainer != null) {
                passwordRequirementsContainer.setVisibility(View.VISIBLE);
            }
            return false;
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError(getString(R.string.error_confirm_password));
            confirmPasswordInput.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.error_password_mismatch));
            confirmPasswordInput.requestFocus();
            return false;
        }
        if (!acceptedTerms) {
            Toast.makeText(this, R.string.terms_acceptance_required, Toast.LENGTH_SHORT).show();
            termsCheckbox.requestFocus();
            return false;
        }
        return true;
    }

    private void updateUserProfile(FirebaseUser user, String fullName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                saveUserToFirestore(user, fullName);
            } else {
                Toast.makeText(sign_in.this,
                        R.string.account_created_profile_failed,
                        Toast.LENGTH_SHORT).show();
                saveUserToFirestore(user, fullName);
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser user, String fullName) {
        firestoreHelper.saveUserData(user.getUid(), fullName, user.getEmail(),
                new FirestoreHelper.OnUserSavedListener() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(sign_in.this,
                                    R.string.registration_successful,
                                    Toast.LENGTH_SHORT).show();
                            navigateToDashboard();
                        });
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(sign_in.this,
                                    getString(R.string.account_created_data_failed) + error,
                                    Toast.LENGTH_SHORT).show();
                            navigateToDashboard();
                        });
                    }
                });
    }

    private void navigateToDashboard() {
        // New accounts already proved their email (OTP above, or Google for Google sign-up)
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            OtpManager.markSessionVerified(this, user.getUid());
        }
        Intent intent = new Intent(sign_in.this, dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getFriendlyErrorMessage(Exception exception) {
        if (exception == null) return getString(R.string.error_registration_failed);
        String errorMessage = exception.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("email address is already in use")) {
                return getString(R.string.error_email_in_use);
            } else if (errorMessage.contains("password is invalid") ||
                    errorMessage.contains("weak password")) {
                return getString(R.string.error_weak_password);
            } else if (errorMessage.contains("network error") ||
                    errorMessage.contains("no internet")) {
                return getString(R.string.error_network);
            } else if (errorMessage.contains("invalid email")) {
                return getString(R.string.error_invalid_email);
            }
        }
        return getString(R.string.error_registration_failed) + " " +
                (errorMessage != null ? errorMessage : getString(R.string.error_unknown));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingHelper.applyTheme(this);
        setLoadingState(false);
        if (errorTextView != null) {
            errorTextView.setVisibility(View.GONE);
        }
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[0-9].*") &&
                password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
    }
}