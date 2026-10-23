package com.example.fintrac;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class NewTransactionDialogFragment extends BottomSheetDialogFragment {

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        // Apply theme to the dialog
        if (getActivity() != null) {
            SettingHelper.applyTheme(getActivity());
        }

        return dialog;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Apply theme when fragment attaches
        if (getActivity() != null) {
            SettingHelper.applyTheme(getActivity());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Apply theme before inflating the view
        if (getActivity() != null) {
            SettingHelper.applyTheme(getActivity());
        }

        return inflater.inflate(R.layout.dialog_new_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Apply theme again to ensure it's applied
        if (getActivity() != null) {
            SettingHelper.applyTheme(getActivity());
        }

        view.findViewById(R.id.close_button).setOnClickListener(v -> dismiss());

        // Apply theme to all important views
        applyThemeToViews(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-apply theme when dialog resumes
        if (getActivity() != null) {
            SettingHelper.applyTheme(getActivity());
        }
    }

    private void applyThemeToViews(View view) {


        View btnExpense = view.findViewById(R.id.btn_type_expense);
        View btnIncome = view.findViewById(R.id.btn_type_income);
        View btnSave = view.findViewById(R.id.btn_save_transaction);

        if (btnExpense != null && btnIncome != null && btnSave != null && getContext() != null) {
            if (SettingHelper.isDarkMode(getContext())) {
                // Apply dark mode specific styling
            } else {
                // Apply light mode specific styling
            }
        }
    }
}