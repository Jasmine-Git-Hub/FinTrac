package com.example.fintrac;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class TemplatesDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "create_challenge_request";
    public static final String KEY_NAME = "challenge_name";
    public static final String KEY_DESC = "challenge_desc";
    public static final String KEY_DURATION = "challenge_duration";
    public static final String KEY_TARGET = "challenge_target";
    public static final String KEY_ACTION = "action";
    public static final String ACTION_SELECT_TEMPLATE = "select_template";
    public static final String ACTION_CREATE_CUSTOM = "create_custom";

    private RecyclerView recyclerView;
    private View customForm;
    private ChallengeTemplateAdapter adapter;
    private List<ChallengeTemplate> templateList;

    // Inputs for Custom
    private TextInputEditText etName, etDuration, etTarget;

    // Tabs
    private MaterialButton btnTabTemplates, btnTabCustom;

    private boolean isCustomMode = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragments_template_dialog, null);

        // Initialize Views
        recyclerView = view.findViewById(R.id.recycler_view_challenges);
        customForm = view.findViewById(R.id.sv_custom_form);
        etName = view.findViewById(R.id.et_challenge_name);
        etDuration = view.findViewById(R.id.et_duration);
        etTarget = view.findViewById(R.id.et_target_amount);
        btnTabTemplates = view.findViewById(R.id.btn_tab_templates);
        btnTabCustom = view.findViewById(R.id.btn_tab_custom);
        ImageView btnClose = view.findViewById(R.id.btn_close);
        Button btnStart = view.findViewById(R.id.btn_start_challenge);

        // 1. SETUP TEMPLATES DATA (The 10 Challenges)
        setupTemplatesList();

        // 2. SETUP RECYCLERVIEW
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChallengeTemplateAdapter(templateList);
        recyclerView.setAdapter(adapter);

        // Handle Item Clicks on Templates - FIXED: This should immediately start the challenge with the selected template
        adapter.setOnItemClickListener(position -> {
            // When a template is clicked, select it AND immediately send the result
            adapter.setSelectedPosition(position);
            // Automatically start the challenge with the selected template
            startTemplateChallenge();
        });

        // 3. TAB SWITCHING LOGIC
        btnTabTemplates.setOnClickListener(v -> switchTab(false));
        btnTabCustom.setOnClickListener(v -> switchTab(true));

        // 4. START CHALLENGE LOGIC
        btnStart.setOnClickListener(v -> {
            if (isCustomMode) {
                startCustomChallenge();
            } else {
                startTemplateChallenge();
            }
        });

        // Close Button
        if (btnClose != null) btnClose.setOnClickListener(v -> dismiss());

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    // --- DITO NATIN NILAGAY ANG 10 TEMPLATES ---
    private void setupTemplatesList() {
        templateList = new ArrayList<>();

        templateList.add(new ChallengeTemplate("No-Café Week", "Skip buying coffee for 7 days.", 7, 1050));
        templateList.add(new ChallengeTemplate("Fast Food Fast", "Cook at home instead of ordering out.", 14, 3000));
        templateList.add(new ChallengeTemplate("Water Only", "Drink only water for a month.", 30, 900));
        templateList.add(new ChallengeTemplate("Baon Warrior", "Bring packed lunch to work/school.", 30, 4000));
        templateList.add(new ChallengeTemplate("Walkathon", "Walk short distances instead of trike.", 7, 350));
        templateList.add(new ChallengeTemplate("Shopping Detox", "No checking out online carts!", 14, 2500));
        templateList.add(new ChallengeTemplate("Sweets Ban", "Skip desserts and sugary snacks.", 7, 500));
        templateList.add(new ChallengeTemplate("The P50 Challenge", "Save every P50 bill you receive.", 30, 1500));
        templateList.add(new ChallengeTemplate("Wi-Fi Only", "Don't buy extra load, use free Wi-Fi.", 7, 100));
        templateList.add(new ChallengeTemplate("Weekend Homebody", "Stay home, no mall trips this weekend.", 2, 2000));
    }

    private void switchTab(boolean custom) {
        isCustomMode = custom;
        if (custom) {
            // Show Custom Form
            recyclerView.setVisibility(View.GONE);
            customForm.setVisibility(View.VISIBLE);

            // Update Buttons Styling (Orange for Active, Gray for Inactive)
            btnTabCustom.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")));
            btnTabCustom.setTextColor(Color.WHITE);

            btnTabTemplates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnTabTemplates.setTextColor(Color.GRAY);

        } else {
            // Show Templates List
            recyclerView.setVisibility(View.VISIBLE);
            customForm.setVisibility(View.GONE);

            // Update Buttons Styling
            btnTabTemplates.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")));
            btnTabTemplates.setTextColor(Color.WHITE);

            btnTabCustom.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btnTabCustom.setTextColor(Color.GRAY);
        }
    }

    private void startTemplateChallenge() {
        ChallengeTemplate selected = adapter.getSelectedChallenge();
        if (selected == null) {
            Toast.makeText(getContext(), "Please select a template first", Toast.LENGTH_SHORT).show();
            return;
        }
        // FIXED: Add the action parameter to indicate this is a template selection
        sendResult(ACTION_SELECT_TEMPLATE, selected.getName(), selected.getDescription(),
                selected.getDurationDays(), selected.getTargetAmount());
    }

    private void startCustomChallenge() {
        String name = etName.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String targetStr = etTarget.getText().toString().trim();

        if (name.isEmpty() || durationStr.isEmpty() || targetStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            double target = Double.parseDouble(targetStr);
            // FIXED: Add the action parameter to indicate this is a custom challenge creation
            sendResult(ACTION_CREATE_CUSTOM, name, "Custom Challenge", duration, target);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendResult(String action, String name, String desc, int duration, double target) {
        Bundle result = new Bundle();
        result.putString(KEY_ACTION, action);
        result.putString(KEY_NAME, name);
        result.putString(KEY_DESC, desc);
        result.putInt(KEY_DURATION, duration);
        result.putDouble(KEY_TARGET, target);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        dismiss();
    }
}