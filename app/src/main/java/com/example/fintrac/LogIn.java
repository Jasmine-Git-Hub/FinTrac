package com.example.fintrac;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LogIn extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_TIME = "lockout_time";
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 5 * 60 * 1000; // 5 minutes

    private EditText emailInput, passwordInput;
    private TextView forgotPasswordText, signUpText, errorTextView;
    private MaterialButton googleButton;
    private Button signInButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        initializeViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Skip login only if this signed-in user already passed the OTP step
        if (currentUser != null && OtpManager.isSessionVerified(this, currentUser.getUid())) {
            navigateToDashboard();
        } else if (currentUser != null) {
            mAuth.signOut();
            OtpManager.clearSession(this);
        }
    }

    /** Password/Google sign-in succeeded: email a code and move to the OTP screen. */
    private void startOtpStep(FirebaseUser user) {
        String email = user.getEmail();
        if (email == null) {
            mAuth.signOut();
            setLoadingState(false);
            showError(getString(R.string.otp_send_failed));
            return;
        }

        OtpManager.clearSession(this);
        setStatusMessage(getString(R.string.sending_otp));

        EmailOtpSender.send(email, OtpManager.issue(email), new EmailOtpSender.Callback() {
            @Override
            public void onSuccess() {
                setLoadingState(false);
                Intent intent = new Intent(LogIn.this, OtpVerificationActivity.class);
                intent.putExtra(OtpVerificationActivity.EXTRA_EMAIL, email);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String error) {
                mAuth.signOut();
                setLoadingState(false);
                showError(EmailOtpSender.describeFailure(error));
            }
        });
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        forgotPasswordText = findViewById(R.id.forgot_password_text);
        signUpText = findViewById(R.id.sign_up_text);
        googleButton = findViewById(R.id.google_button);
        signInButton = findViewById(R.id.sign_in_button);
        progressBar = findViewById(R.id.progressBar);
        errorTextView = findViewById(R.id.errorTextView);
    }

    private void setupClickListeners() {
        TextView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        if (forgotPasswordText != null) {
            forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());
        }

        if (signInButton != null) {
            signInButton.setOnClickListener(v -> loginUser());
        }

        if (signUpText != null) {
            signUpText.setOnClickListener(v -> {
                startActivity(new Intent(LogIn.this, sign_in.class));
                finish();
            });
        }

        if (googleButton != null) {
            googleButton.setOnClickListener(v -> signInWithGoogle());
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
                        // Reset failed attempts
                        sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply();
                        sharedPreferences.edit().putLong(KEY_LOCKOUT_TIME, 0).apply();

                        startOtpStep(mAuth.getCurrentUser());
                    } else {
                        setLoadingState(false);
                        // If sign in fails, display a message to the user.
                        String errorMsg = getFriendlyErrorMessage(task.getException());
                        showError(getString(R.string.google_sign_in_failed) + errorMsg);
                    }
                });
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        // Check lockout
        long lockoutTime = sharedPreferences.getLong(KEY_LOCKOUT_TIME, 0);
        if (System.currentTimeMillis() < lockoutTime + LOCKOUT_DURATION) {
            long remaining = (lockoutTime + LOCKOUT_DURATION) - System.currentTimeMillis();
            long minutes = (remaining / 1000) / 60;
            long seconds = (remaining / 1000) % 60;
            showError(getString(R.string.account_locked) + minutes + "m " + seconds + "s.");
            return;
        }

        setLoadingState(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Reset failed attempts
                        sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, 0).apply();
                        sharedPreferences.edit().putLong(KEY_LOCKOUT_TIME, 0).apply();

                        startOtpStep(mAuth.getCurrentUser());
                    } else {
                        setLoadingState(false);

                        int attempts = sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
                        sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply();

                        if (attempts >= MAX_ATTEMPTS) {
                            sharedPreferences.edit().putLong(KEY_LOCKOUT_TIME, System.currentTimeMillis()).apply();
                            showError(getString(R.string.account_locked_message));
                        } else {
                            int left = MAX_ATTEMPTS - attempts;
                            String errorMsg = getFriendlyErrorMessage(task.getException());
                            showError(errorMsg + " (" + left + " " + getString(R.string.attempts_left) + ")");
                        }
                    }
                });
    }

    private void setLoadingState(boolean isLoading) {
        if (signInButton != null) {
            signInButton.setEnabled(!isLoading);
            signInButton.setText(isLoading ? getString(R.string.authenticating) : getString(R.string.sign_in));
        }

        if (googleButton != null) {
            googleButton.setEnabled(!isLoading);
        }

        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        if (errorTextView != null && !isLoading) {
            errorTextView.setVisibility(android.view.View.GONE);
        }
    }

    private void setStatusMessage(String message) {
        if (signInButton != null) {
            signInButton.setText(message);
        }
    }

    private void showError(String message) {
        if (errorTextView != null) {
            errorTextView.setText(message);
            errorTextView.setVisibility(android.view.View.VISIBLE);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.email_required));
            emailInput.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.valid_email));
            emailInput.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.password_required));
            passwordInput.requestFocus();
            return false;
        }
        return true;
    }

    private String getFriendlyErrorMessage(Exception exception) {
        if (exception == null) return getString(R.string.login_failed);
        String msg = exception.getMessage();
        if (msg == null) return getString(R.string.login_failed);

        if (msg.contains("invalid login credentials") ||
                msg.contains("no user record") ||
                msg.contains("password is invalid") ||
                msg.contains("ERROR_WRONG_PASSWORD") ||
                msg.contains("ERROR_USER_NOT_FOUND")) {
            return getString(R.string.invalid_credentials);
        } else if (msg.contains("network error") || msg.contains("no internet")) {
            return getString(R.string.no_internet);
        } else if (msg.contains("too many requests")) {
            return getString(R.string.too_many_attempts);
        } else if (msg.contains("user disabled")) {
            return getString(R.string.account_disabled);
        } else if (msg.contains("user-not-found")) {
            return getString(R.string.no_account);
        } else if (msg.contains("invalid-email")) {
            return getString(R.string.invalid_email_format);
        }
        return getString(R.string.login_failed) + msg;
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(LogIn.this, dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        String email = emailInput.getText().toString().trim();
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.enter_valid_email));
            emailInput.requestFocus();
            return;
        }

        setLoadingState(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(LogIn.this,
                                getString(R.string.reset_link_sent) + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        String error = getResetPasswordErrorMessage(task.getException());
                        showError(error);
                    }
                });
    }

    private String getResetPasswordErrorMessage(Exception exception) {
        if (exception == null) return getString(R.string.reset_email_failed);
        String exMsg = exception.getMessage();
        if (exMsg == null) return getString(R.string.reset_email_failed);

        if (exMsg.contains("user-not-found"))
            return getString(R.string.no_account_with_email);
        else if (exMsg.contains("network error"))
            return getString(R.string.network_error);
        else
            return exMsg;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingHelper.applyTheme(this);
        setLoadingState(false);
        if (errorTextView != null) {
            errorTextView.setVisibility(android.view.View.GONE);
        }
    }
}