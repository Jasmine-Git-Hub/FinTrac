package com.example.fintrac;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class InviteDialogFragment extends DialogFragment {

    private final String jarId;
    private final String jarName;

    public InviteDialogFragment(String jarId, String jarName) {
        this.jarId = jarId;
        this.jarName = jarName;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_invite, null);

        // UI Binding
        TextView title = view.findViewById(R.id.dialog_title);
        EditText emailEditText = view.findViewById(R.id.email_edit_text);
        Button sendButton = view.findViewById(R.id.send_button);
        Button shareLinkButton = view.findViewById(R.id.share_link_button);

        // FIX: Ensure close button works
        ImageButton closeButton = view.findViewById(R.id.btn_close);

        if (closeButton != null) {
            closeButton.setOnClickListener(v -> {
                dismiss(); // This will close the dialog
            });
        }

        if (title != null) {
            title.setText("Invite to " + jarName);
        }

        sendButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (!email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getActivity(), "Invitation sent to " + email, Toast.LENGTH_SHORT).show();
                dismiss();
            } else {
                emailEditText.setError("Enter a valid email");
            }
        });

        shareLinkButton.setOnClickListener(v -> {
            String shareLink = "https://fintrac.app/join/" + jarId;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Join my shared jar '" + jarName + "' on FinTrac: " + shareLink);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Invitation to join " + jarName);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            dismiss();
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Make sure dialog can be cancelled
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        return dialog;
    }
}