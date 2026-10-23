package com.example.fintrac;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Second step of login: the user enters the 6-digit code emailed by {@link LogIn}.
 * The code itself is never passed here; {@link OtpManager} holds only its hash.
 */
public class OtpVerificationActivity extends BaseActivity {

    public static final String EXTRA_EMAIL = "USER_EMAIL";

    private String userEmail;
    private EditText etOtp;
    private Button btnVerify;
    private TextView tvResendOtp;
    private CountDownTimer cooldownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        if (user == null || userEmail == null) {
            backToLogin();
            return;
        }

        etOtp = findViewById(R.id.et_otp_input);
        btnVerify = findViewById(R.id.btn_verify_otp);
        tvResendOtp = findViewById(R.id.tv_resend_otp);

        // "Remember this device" relied on a forgeable local flag; it's no longer offered
        View cbSaveOtp = findViewById(R.id.cb_save_otp);
        if (cbSaveOtp != null) cbSaveOtp.setVisibility(View.GONE);

        btnVerify.setOnClickListener(v -> verify(user));
        tvResendOtp.setOnClickListener(v -> resend());

        // Leaving this screen without verifying cancels the login
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backToLogin();
            }
        });

        startResendCooldown(OtpManager.cooldownRemainingMs());
    }

    private void verify(FirebaseUser user) {
        String entered = etOtp.getText().toString().trim();
        if (entered.length() != 6) {
            etOtp.setError(getString(R.string.enter_6_digit_otp));
            return;
        }

        switch (OtpManager.verify(userEmail, entered)) {
            case OK:
                OtpManager.markSessionVerified(this, user.getUid());
                Toast.makeText(this, R.string.verification_successful, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, dashboard.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                break;
            case WRONG:
                etOtp.setError(getString(R.string.incorrect_otp_code)
                        + " (" + OtpManager.attemptsLeft() + " " + getString(R.string.attempts_left) + ")");
                break;
            case LOCKED:
                etOtp.setText("");
                etOtp.setError("Too many wrong codes. Request a new one.");
                btnVerify.setEnabled(false);
                break;
            case EXPIRED:
                etOtp.setText("");
                Toast.makeText(this, R.string.otp_expired_request_new, Toast.LENGTH_LONG).show();
                btnVerify.setEnabled(false);
                break;
        }
    }

    private void resend() {
        if (OtpManager.cooldownRemainingMs() > 0) return;

        tvResendOtp.setEnabled(false);
        tvResendOtp.setText(R.string.sending);

        String code = OtpManager.issue(userEmail);
        EmailOtpSender.send(userEmail, code, new EmailOtpSender.Callback() {
            @Override
            public void onSuccess() {
                if (isFinishing()) return;
                etOtp.setText("");
                btnVerify.setEnabled(true);
                Toast.makeText(OtpVerificationActivity.this,
                        "New code sent. It expires in " + OtpManager.VALIDITY_MINUTES + " minutes.",
                        Toast.LENGTH_LONG).show();
                startResendCooldown(OtpManager.RESEND_COOLDOWN_MS);
            }

            @Override
            public void onFailure(String error) {
                if (isFinishing()) return;
                Toast.makeText(OtpVerificationActivity.this,
                        EmailOtpSender.describeFailure(error), Toast.LENGTH_LONG).show();
                tvResendOtp.setEnabled(true);
                tvResendOtp.setText(R.string.resend);
            }
        });
    }

    private void startResendCooldown(long millis) {
        if (cooldownTimer != null) cooldownTimer.cancel();
        if (millis <= 0) {
            enableResend();
            return;
        }
        tvResendOtp.setEnabled(false);
        cooldownTimer = new CountDownTimer(millis, 1000) {
            public void onTick(long millisUntilFinished) {
                tvResendOtp.setText(getString(R.string.resend_in_seconds, millisUntilFinished / 1000));
                tvResendOtp.setTextColor(ContextCompat.getColor(OtpVerificationActivity.this, R.color.gray));
            }

            public void onFinish() {
                enableResend();
            }
        }.start();
    }

    private void enableResend() {
        tvResendOtp.setEnabled(true);
        tvResendOtp.setText(R.string.resend);
        tvResendOtp.setTextColor(ContextCompat.getColor(this, R.color.blue));
    }

    private void backToLogin() {
        FirebaseAuth.getInstance().signOut();
        OtpManager.clearSession(this);
        Intent intent = new Intent(this, LogIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (cooldownTimer != null) cooldownTimer.cancel();
        super.onDestroy();
    }
}
