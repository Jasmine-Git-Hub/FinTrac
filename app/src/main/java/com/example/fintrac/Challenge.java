package com.example.fintrac;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
public class Challenge extends Fragment implements ChallengeAdapter.OnChallengeActionListener {

    private ChallengeAdapter adapter;
    private List<ChallengeItem> challengeList;
    private FirebaseFirestore db;
    private String userID;

    private TextView tvActiveCount;
    private TextView tvSavedTotal;

    private MaterialCardView cardNoCafe;
    private MaterialCardView cardDaily20Pesos;

    public Challenge() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_challenge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userID = FirebaseAuth.getInstance().getUid();

        RecyclerView recyclerView = view.findViewById(R.id.rv_active_challenges);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        tvActiveCount = view.findViewById(R.id.tv_active_count);
        tvSavedTotal = view.findViewById(R.id.tv_saved_total);

        cardNoCafe = view.findViewById(R.id.card_no_cafe);
        cardDaily20Pesos = view.findViewById(R.id.card_daily_20p);

        challengeList = new ArrayList<>();
        adapter = new ChallengeAdapter(challengeList, getContext(), this);
        recyclerView.setAdapter(adapter);

        loadChallengesFromFirebase();
        setupQuickStartClickListeners();

        FloatingActionButton fabAddChallenge = view.findViewById(R.id.fab_add_challenge);
        fabAddChallenge.setOnClickListener(v -> showTemplatesDialogFragment());

        getChildFragmentManager().setFragmentResultListener(TemplatesDialogFragment.REQUEST_KEY, this, (requestKey, result) -> {
            String action = result.getString(TemplatesDialogFragment.KEY_ACTION, "");

            if (TemplatesDialogFragment.ACTION_SELECT_TEMPLATE.equals(action)) {
                String name = result.getString(TemplatesDialogFragment.KEY_NAME);
                String desc = result.getString(TemplatesDialogFragment.KEY_DESC);
                int duration = result.getInt(TemplatesDialogFragment.KEY_DURATION);
                double target = result.getDouble(TemplatesDialogFragment.KEY_TARGET);
                showCustomChallengeDialog(name, desc, duration, target);

            } else if (TemplatesDialogFragment.ACTION_CREATE_CUSTOM.equals(action)) {
                String name = result.getString(TemplatesDialogFragment.KEY_NAME);
                String desc = result.getString(TemplatesDialogFragment.KEY_DESC);
                int duration = result.getInt(TemplatesDialogFragment.KEY_DURATION);
                double target = result.getDouble(TemplatesDialogFragment.KEY_TARGET);

                ChallengeItem newChallenge = new ChallengeItem(
                        name, desc, ChallengeIconHelper.getIcon(name), ChallengeIconHelper.getColor(name),
                        0, duration, target
                );
                saveChallengeToFirebase(newChallenge);
            }
        });
    }

    private void setupQuickStartClickListeners() {
        cardNoCafe.setOnClickListener(v -> showCustomChallengeDialog("No-Café Week", "Skip buying coffee for 7 days.", 7, 1050));
        // Fixed: Daily 20 Pesos Challenge - 30 days * 20 pesos = 600 pesos total target
        cardDaily20Pesos.setOnClickListener(v -> showCustomChallengeDialog("Daily 20 Pesos Challenge", "Save 20 pesos every day for 30 days", 30, 600.0));
    }

    private void showCustomChallengeDialog(String templateName, String templateDesc, int templateDuration, double templateTarget) {
        CustomChallengeDialogFragment dialog = new CustomChallengeDialogFragment();

        Bundle args = new Bundle();
        args.putString("template_name", templateName);
        args.putString("template_desc", templateDesc);
        args.putInt("template_duration", templateDuration);
        args.putDouble("template_target", templateTarget);
        dialog.setArguments(args);

        dialog.setOnCustomChallengeCreatedListener(challenge -> {
            ChallengeItem newChallenge = new ChallengeItem(
                    challenge.getName(),
                    challenge.getDescription(),
                    ChallengeIconHelper.getIcon(challenge.getName()),
                    ChallengeIconHelper.getColor(challenge.getName()),
                    0,
                    challenge.getDurationDays(),
                    challenge.getTargetAmount()
            );

            newChallenge.setArchived(false);
            saveChallengeToFirebase(newChallenge);
        });

        dialog.show(getChildFragmentManager(), "custom_challenge_dialog");
    }

    private void updateSummary() {
        if (tvActiveCount == null || tvSavedTotal == null) return;

        int activeCount = challengeList.size();
        tvActiveCount.setText(String.valueOf(activeCount));

        double totalSavedPHP = 0;
        for (ChallengeItem item : challengeList) {
            // Calculate the exact amount saved based on days completed
            // Each check-in represents saving a fixed daily amount
            double dailySaveAmount = item.getTargetAmount() / item.getTotalDays();
            double amountSaved = dailySaveAmount * item.getCurrentDay();
            totalSavedPHP += amountSaved;
        }

        tvSavedTotal.setText(SettingHelper.formatAmount(requireContext(), totalSavedPHP));
    }

    private void loadChallengesFromFirebase() {
        if (userID == null) return;

        db.collection("users").document(userID).collection("challenges")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    challengeList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ChallengeItem item = document.toObject(ChallengeItem.class);
                        item.setDocumentId(document.getId());

                        // Para 100% sure na babasahin yung tamang "isArchived" field mula sa database
                        Boolean isArchivedDb = document.getBoolean("isArchived");
                        boolean archived = (isArchivedDb != null && isArchivedDb);

                        // I-check kung 100% completed na ang challenge
                        boolean isCompleted = item.getTotalDays() > 0 && item.getCurrentDay() >= item.getTotalDays();

                        if (!archived) {
                            if (isCompleted) {
                                // Kapag 100% done na pero hindi pa naka-archive (mga nastuck na lumang challenge),
                                // i-aarchive na natin agad sa background (-1) para HINDI NA TUMAMBAY sa screen.
                                archiveChallenge(item, -1);
                            } else {
                                // Kung active at di pa 100%, tsaka lang idadagdag sa listahan
                                item.setIconResId(ChallengeIconHelper.getIcon(item.getName()));
                                item.setColorResId(ChallengeIconHelper.getColor(item.getName()));
                                item.updateCalculatedFields();

                                challengeList.add(item);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateSummary();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveChallengeToFirebase(ChallengeItem challenge) {
        if (userID == null) return;

        db.collection("users").document(userID).collection("challenges")
                .add(challenge)
                .addOnSuccessListener(documentReference -> {
                    challenge.setDocumentId(documentReference.getId());
                    challengeList.add(challenge);
                    adapter.notifyItemInserted(challengeList.size() - 1);
                    updateSummary();
                    Toast.makeText(getContext(), "Challenge Started!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onCheckIn(ChallengeItem item, int position) {
        if (item.getDocumentId() == null || userID == null) return;

        int newDay = item.getCurrentDay() + 1;
        long currentTime = System.currentTimeMillis();

        item.setCurrentDay(newDay);
        item.setLastCheckInTime(currentTime);

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentDay", newDay);
        updates.put("lastCheckInTime", currentTime);

        boolean isCompleted = newDay >= item.getTotalDays();
        if (isCompleted) {
            updates.put("isCompleted", true);
        }

        db.collection("users").document(userID)
                .collection("challenges").document(item.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    adapter.notifyItemChanged(position);
                    updateSummary();
                    clearPendingNotifications(item.getDocumentId());

                    // Calculate and show exact amount saved this check-in
                    double dailyAmount = item.getTargetAmount() / item.getTotalDays();
                    String formattedAmount = SettingHelper.formatAmount(getContext(), dailyAmount);

                    if (isCompleted) {
                        showChallengeCompletedDialog(item, position);
                    } else {
                        Toast.makeText(getContext(), "Checked In! +" + formattedAmount + " saved!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    item.setCurrentDay(newDay - 1);
                    Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showChallengeCompletedDialog(ChallengeItem item, int position) {
        if (getContext() == null) return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_challenge_completed, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_completed_title);
        TextView tvDesc = dialogView.findViewById(R.id.tv_completed_desc);
        com.google.android.material.button.MaterialButton btnContinue = dialogView.findViewById(R.id.btn_continue_success);

        tvTitle.setText("You've conquered the " + item.getName() + "!");
        String formattedAmount = SettingHelper.formatAmount(getContext(), item.getTargetAmount());
        tvDesc.setText("Incredible discipline! You successfully saved " + formattedAmount + " in " + item.getTotalDays() + " days. Keep up the great work!");

        // Rekta archive na agad pagka-click ng continue
        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            archiveChallenge(item, position);
        });

        dialog.show();
    }

    private void archiveChallenge(ChallengeItem item, int position) {
        if (item.getDocumentId() == null || userID == null) return;

        Map<String, Object> archiveData = new HashMap<>();
        archiveData.put("isArchived", true);
        archiveData.put("archivedAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userID)
                .collection("challenges").document(item.getDocumentId())
                .update(archiveData)
                .addOnSuccessListener(aVoid -> {
                    if (position >= 0 && position < challengeList.size()) {
                        challengeList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, challengeList.size());
                        updateSummary();
                        Toast.makeText(getContext(), "Challenge completed and moved to Archives", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (position >= 0) { // Para hindi magpakita ng error kapag nag-background cleanup tayo
                        Toast.makeText(getContext(), "Failed to archive: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteChallengePermanently(ChallengeItem item, int position) {
        if (item.getDocumentId() == null || userID == null) return;

        db.collection("users").document(userID)
                .collection("challenges").document(item.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (position >= 0 && position < challengeList.size()) {
                        challengeList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, challengeList.size());
                        updateSummary();
                        Toast.makeText(getContext(), "Challenge deleted permanently", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onGiveUp(ChallengeItem item, int position) {
        if (item.getDocumentId() == null) return;

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Stop Challenge")
                .setMessage("Do you want to archive this challenge or delete it permanently?")
                .setPositiveButton("Archive", (dialog, which) -> archiveChallenge(item, position))
                .setNegativeButton("Delete Permanently", (dialog, which) -> deleteChallengePermanently(item, position))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showTemplatesDialogFragment() {
        TemplatesDialogFragment dialog = new TemplatesDialogFragment();
        dialog.show(getChildFragmentManager(), "templates_dialog");
    }

    private void clearPendingNotifications(String itemId) {
        if (userID == null || itemId == null) return;

        FirebaseFirestore.getInstance().collection("users").document(userID)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String notifId = doc.getString("id");
                        if (notifId != null && notifId.contains(itemId)) {
                            doc.getReference().update("isRead", true, "read", true);
                        }
                    }
                });
    }
}