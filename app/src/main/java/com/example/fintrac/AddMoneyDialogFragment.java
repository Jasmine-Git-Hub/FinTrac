package com.example.fintrac;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AddMoneyDialogFragment extends DialogFragment {

    private static final String ARG_JAR_ID = "jar_id";
    private static final String ARG_JAR_NAME = "jar_name";

    // IMPORTANTE: Ito ang tinatawag ng SharedJarsDetails
    public static AddMoneyDialogFragment newInstance(String jarId, String jarName) {
        AddMoneyDialogFragment fragment = new AddMoneyDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JAR_ID, jarId);
        args.putString(ARG_JAR_NAME, jarName);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String jarId = "";
        String jarName = "";

        if (getArguments() != null) {
            jarId = getArguments().getString(ARG_JAR_ID);
            jarName = getArguments().getString(ARG_JAR_NAME);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_money, null);

        TextView title = view.findViewById(R.id.dialog_title);
        EditText amountEditText = view.findViewById(R.id.amount_edit_text);
        Button addButton = view.findViewById(R.id.add_button);
        Button cancelButton = view.findViewById(R.id.cancel_button);

        if (title != null) title.setText("Add Money to " + jarName);

        String finalJarId = jarId;

        if (addButton != null) {
            addButton.setOnClickListener(v -> {
                String amountStr = amountEditText.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    amountEditText.setError("Required");
                    return;
                }

                try {
                    int amount = Integer.parseInt(amountStr);
                    Bundle result = new Bundle();
                    result.putInt("AMOUNT", amount);
                    result.putString("JAR_ID", finalJarId);

                    // Ipapasa ang result pabalik sa SharedJarsDetails
                    getParentFragmentManager().setFragmentResult("addMoneyRequest", result);
                    dismiss();
                } catch (NumberFormatException e) {
                    amountEditText.setError("Invalid number");
                }
            });
        }

        if (cancelButton != null) cancelButton.setOnClickListener(v -> dismiss());

        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }
}