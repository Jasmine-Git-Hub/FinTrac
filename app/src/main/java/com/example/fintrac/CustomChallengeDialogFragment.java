package com.example.fintrac;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class CustomChallengeDialogFragment extends DialogFragment {

    private TextInputEditText etChallengeName;
    private TextInputEditText etDuration;
    private TextInputEditText etTargetAmount;
    private TextInputLayout tilTargetAmount;
    private MaterialButton btnStartChallenge;
    private OnCustomChallengeCreatedListener listener;

    public interface OnCustomChallengeCreatedListener {
        void onCustomChallengeCreated(ChallengeTemplate challenge);
    }

    public void setOnCustomChallengeCreatedListener(OnCustomChallengeCreatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TINANGGAL: R.style.AppDialogTheme kasi baka iforce niya sa Light Mode.
        // Zero (0) ang ilagay natin para sumunod sa context theme.
        setStyle(DialogFragment.STYLE_NORMAL, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // I-APPLY ANG THEME BAGO I-INFLATE ANG LAYOUT PARA GUMANA ANG DARK MODE
        Context context = requireContext();
        SettingHelper.applyTheme(context);

        // Gumamit tayo ng clone ng inflater na may tamang theme context
        LayoutInflater themedInflater = inflater.cloneInContext(context);
        View view = themedInflater.inflate(R.layout.fragments_custom_challenge_dialog, container, false);

        etChallengeName = view.findViewById(R.id.et_challenge_name);
        etDuration = view.findViewById(R.id.et_duration);
        etTargetAmount = view.findViewById(R.id.et_target_amount);
        tilTargetAmount = view.findViewById(R.id.til_target_amount);
        btnStartChallenge = view.findViewById(R.id.btn_start_challenge);
        ImageView btnClose = view.findViewById(R.id.btn_close);

        // Pre-fill with template data if available
        if (getArguments() != null) {
            String templateName = getArguments().getString("template_name", "");
            int templateDuration = getArguments().getInt("template_duration", 0);
            double templateTarget = getArguments().getDouble("template_target", 0.0);

            // Pre-fill the fields
            if (!templateName.isEmpty()) {
                etChallengeName.setText(templateName);
            }
            if (templateDuration > 0) {
                etDuration.setText(String.valueOf(templateDuration));
            }
            if (templateTarget > 0) {
                etTargetAmount.setText(String.valueOf(templateTarget));
            }
            btnStartChallenge.setText("Start This Challenge");
        }

        // ===============================================
        // DYNAMIC CURRENCY SETUP PARA SA TARGET AMOUNT
        // ===============================================
        try {
            String selectedCurrency = SettingHelper.getCurrency(context);
            String currencySymbol = SettingHelper.getCurrencySymbol(selectedCurrency);

            // I-set yung Hint na may symbol (e.g. "Target Amount (₱)")
            tilTargetAmount.setHint("Target Amount (" + currencySymbol + ")");
            // I-set yung mismong prefix icon sa loob ng text box!
            tilTargetAmount.setPrefixText(currencySymbol);

        } catch (Exception e) {
            e.printStackTrace();
        }

        btnStartChallenge.setOnClickListener(v -> {
            if (validateInputs()) {
                createCustomChallenge();
            }
        });

        btnClose.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            // Para sumunod yung kanto ng background card, gawin nating transparent yung root window
            getDialog().getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
    }

    private boolean validateInputs() {
        String name = etChallengeName.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String amountStr = etTargetAmount.getText().toString().trim();

        if (name.isEmpty()) {
            etChallengeName.setError("Challenge name is required");
            return false;
        }

        if (durationStr.isEmpty()) {
            etDuration.setError("Duration is required");
            return false;
        }

        if (amountStr.isEmpty()) {
            etTargetAmount.setError("Target amount is required");
            return false;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            if (duration <= 0) {
                etDuration.setError("Duration must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            etDuration.setError("Invalid number");
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etTargetAmount.setError("Amount must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            etTargetAmount.setError("Invalid amount");
            return false;
        }

        return true;
    }

    private void createCustomChallenge() {
        String name = etChallengeName.getText().toString().trim();
        int duration = Integer.parseInt(etDuration.getText().toString().trim());
        double targetAmount = Double.parseDouble(etTargetAmount.getText().toString().trim());

        // Check if this was based on a template
        String description = "Custom challenge: " + name;
        if (getArguments() != null) {
            String templateDesc = getArguments().getString("template_desc", "");
            if (!templateDesc.isEmpty()) {
                description = templateDesc;
            }
        }

        ChallengeTemplate customChallenge = new ChallengeTemplate(
                name,
                description,
                duration,
                targetAmount
        );

        if (listener != null) {
            listener.onCustomChallengeCreated(customChallenge);
            dismiss();
        } else {
            Toast.makeText(getContext(), "Error: Listener not set", Toast.LENGTH_SHORT).show();
        }
    }
}