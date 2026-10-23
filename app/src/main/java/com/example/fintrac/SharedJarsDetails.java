package com.example.fintrac;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fintrac.core.Money;
import com.example.fintrac.data.UserProfile;
import com.example.fintrac.jars.JarDetailsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * One jar's details. Everything shown comes from {@link JarDetailsViewModel}, which keeps the
 * jar and its activity live, so there's no manual reloading after actions or on resume.
 */
public class SharedJarsDetails extends BaseActivity {

    private JarDetailsViewModel viewModel;

    private String jarName;
    private String jarType;
    private int color;

    private TextView totalSavedView, totalGoalView, progressPercentage, jarTypeView, suggestionText;
    private LinearProgressIndicator progressBar;
    private LinearLayout participantsContainer;
    private ActivityAdapter activityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_jars_details);

        Intent intent = getIntent();
        String jarId = intent.getStringExtra("JAR_ID");
        if (jarId == null) {
            Toast.makeText(this, "Error: Jar ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Shown immediately from the list while the live jar loads
        jarName = intent.getStringExtra("JAR_NAME");
        jarType = intent.getStringExtra("JAR_TYPE");
        color = intent.getIntExtra("COLOR", 0xFF2196F3);

        viewModel = new ViewModelProvider(this, JarDetailsViewModel.factory(jarId)).get(JarDetailsViewModel.class);

        initViews(intent.getStringExtra("ICON_NAME"));
        renderTotals(intent.getDoubleExtra("JAR_SAVED", 0), intent.getDoubleExtra("JAR_GOAL", 0));
        observe();
    }

    private void initViews(String iconName) {
        totalSavedView = findViewById(R.id.total_saved);
        totalGoalView = findViewById(R.id.total_goal);
        progressPercentage = findViewById(R.id.progress_percentage);
        progressBar = findViewById(R.id.progress_bar);
        participantsContainer = findViewById(R.id.participants_container);
        jarTypeView = findViewById(R.id.jar_type);
        suggestionText = findViewById(R.id.progress_suggestion_text);

        ImageView jarIcon = findViewById(R.id.jar_icon);
        jarIcon.setImageResource(iconFor(iconName));
        jarIcon.setBackgroundTintList(ColorStateList.valueOf(color));
        jarIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        renderHeader();

        // The current layout shows the activity feed on its own screen ("View activity");
        // an inline list is only wired up if a layout provides one.
        activityAdapter = new ActivityAdapter();
        RecyclerView activityList = findViewById(R.id.transactions_recycler_view);
        if (activityList != null) {
            activityList.setLayoutManager(new LinearLayoutManager(this));
            activityList.setAdapter(activityAdapter);
        }

        ImageButton back = findViewById(R.id.back_button);
        back.setOnClickListener(v -> finish());
        findViewById(R.id.add_money_button).setOnClickListener(v -> showAddMoneyDialog());
        findViewById(R.id.invite_button).setOnClickListener(v -> showInviteDialog());
        findViewById(R.id.view_activity_button).setOnClickListener(v -> {
            Intent log = new Intent(this, ActivityLogActivity.class);
            log.putExtra("JAR_ID", getIntent().getStringExtra("JAR_ID"));
            log.putExtra("JAR_NAME", jarName);
            startActivity(log);
        });
    }

    private void observe() {
        viewModel.jar().observe(this, jar -> {
            jarName = jar.getName();
            jarType = jar.getType();
            renderHeader();
            renderTotals(jar.getSavedAmount(), jar.getGoalAmount());
            renderParticipants(jar, viewModel.profiles().getValue());
        });
        viewModel.profiles().observe(this, profiles -> {
            JarFirestoreModel jar = viewModel.jar().getValue();
            if (jar != null) renderParticipants(jar, profiles);
            activityAdapter.setProfiles(profiles);
        });
        viewModel.activity().observe(this, activityAdapter::submitList);

        viewModel.messages().observe(this, event -> {
            String text = event.consume();
            if (text != null) toast(text);
        });
        viewModel.deposited().observe(this, event -> {
            Money amount = event.consume();
            if (amount != null) toast("Added " + format(amount.toPesos()));
        });
        viewModel.limitMessages().observe(this, event -> {
            JarDetailsViewModel.LimitMessage limit = event.consume();
            if (limit != null) toast(describe(limit));
        });
        viewModel.goalReached().observe(this, event -> {
            if (event.consume() != null) showGoalReachedDialog();
        });
        viewModel.closeScreen().observe(this, event -> {
            if (event.consume() != null) finish();
        });
    }

    // ---- Rendering ----

    private void renderHeader() {
        TextView title = findViewById(R.id.jar_title);
        title.setText(jarName != null ? jarName : "Shared Jar");

        jarTypeView.setText(jarType != null ? jarType : "Shared");
        int text, background;
        if ("Mandatory".equalsIgnoreCase(jarType)) {
            text = R.color.red_dark;
            background = R.color.red_light;
        } else if ("Flexible".equalsIgnoreCase(jarType)) {
            text = R.color.green_dark;
            background = R.color.green_light;
        } else {
            text = R.color.blue;
            background = R.color.blue_light;
        }
        jarTypeView.setTextColor(ContextCompat.getColor(this, text));
        if (jarTypeView.getParent() instanceof MaterialCardView) {
            ((MaterialCardView) jarTypeView.getParent()).setCardBackgroundColor(ContextCompat.getColor(this, background));
        }
    }

    private void renderTotals(double saved, double goal) {
        totalSavedView.setText(format(saved));
        totalGoalView.setText("Goal: " + format(goal));
        if (goal > 0) {
            int progress = (int) Math.min(Math.round(saved / goal * 100), 100);
            progressBar.setMax(100);
            progressBar.setProgressCompat(progress, true);
            progressBar.setIndicatorColor(color);
            progressPercentage.setText(progress + "% complete");
        }
    }

    private void renderParticipants(JarFirestoreModel jar, Map<String, UserProfile> profiles) {
        participantsContainer.removeAllViews();
        String me = viewModel.currentUserId();
        Money share = viewModel.suggestedShare();
        boolean owner = viewModel.isOwner();

        for (String uid : jar.getParticipants()) {
            boolean isMe = uid.equals(me);
            UserProfile profile = profiles != null ? profiles.get(uid) : null;
            String name = isMe ? "You" : profile != null ? profile.displayName : "Loading...";
            Double contribution = jar.getParticipantContributions().get(uid);

            View row = LayoutInflater.from(this).inflate(R.layout.item_participant, participantsContainer, false);
            ((TextView) row.findViewById(R.id.participant_name)).setText(name);
            ((TextView) row.findViewById(R.id.participant_contribution))
                    .setText("Contributed: " + format(contribution != null ? contribution : 0));

            TextView suggested = row.findViewById(R.id.participant_suggested);
            if (isMe && share.isPositive()) {
                suggested.setVisibility(View.VISIBLE);
                suggested.setText("Suggested: " + format(share.toPesos()));
            }

            ImageButton remove = row.findViewById(R.id.btn_remove_participant);
            if (owner && !isMe) {
                remove.setVisibility(View.VISIBLE);
                remove.setContentDescription("Remove " + name);
                remove.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                        .setTitle("Remove Participant")
                        .setMessage("Are you sure you want to remove " + name + " from this jar?")
                        .setPositiveButton("Remove", (d, w) -> viewModel.removeParticipant(uid, name))
                        .setNegativeButton("Cancel", null)
                        .show());
            }

            loadAvatar(row.findViewById(R.id.participant_avatar), profile, isMe);
            participantsContainer.addView(row);
        }

        if (share.isPositive()) {
            suggestionText.setText("Suggested contribution: " + format(share.toPesos()) + " each to reach goal");
        }
    }

    private void loadAvatar(ImageView view, UserProfile profile, boolean isMe) {
        if (profile != null && profile.avatarUri != null && !profile.avatarUri.isEmpty()) {
            showAvatar(view, Uri.parse(profile.avatarUri));
            return;
        }
        if (profile != null && profile.avatarName != null && !profile.avatarName.isEmpty()) {
            int resId = getResources().getIdentifier(profile.avatarName, "drawable", getPackageName());
            if (resId == 0) {
                resId = getResources().getIdentifier("avatar_" + profile.avatarName, "drawable", getPackageName());
            }
            if (resId != 0) {
                showAvatar(view, resId);
                return;
            }
        }
        view.setImageResource(R.drawable.ic_person);
        view.setPadding(8, 8, 8, 8);
        view.setBackgroundTintList(ColorStateList.valueOf(isMe ? color : ContextCompat.getColor(this, R.color.gray)));
        view.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
    }

    private void showAvatar(ImageView view, Object source) {
        Glide.with(this)
                .load(source)
                .circleCrop()
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(view);
        view.setPadding(0, 0, 0, 0);
        view.setBackgroundTintList(null);
        view.setImageTintList(null);
    }

    // ---- Dialogs ----

    private void showAddMoneyDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_money, null);
        EditText amountInput = dialogView.findViewById(R.id.amount_edit_text);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create();

        dialogView.findViewById(R.id.cancel_button).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.add_button).setOnClickListener(v -> {
            Money typed = Money.parse(amountInput.getText().toString());
            if (!typed.isPositive()) {
                amountInput.setError("Please enter a valid amount");
                return;
            }
            // Typed in display currency; jars are stored in base PHP
            viewModel.addMoney(Money.ofPesos(SettingHelper.convertToBasePHP(this, typed.toPesos())));
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showInviteDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite, null);
        TextInputEditText emailInput = dialogView.findViewById(R.id.email_edit_text);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(false).create();

        ImageButton close = dialogView.findViewById(R.id.btn_close);
        if (close != null) close.setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.send_button).setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Valid email required");
                return;
            }
            viewModel.invite(email);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.share_link_button).setOnClickListener(v -> {
            JarFirestoreModel jar = viewModel.jar().getValue();
            String goal = jar != null ? format(jar.getGoalAmount()) : "";
            String saved = jar != null ? format(jar.getSavedAmount()) : "";
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Join my jar: " + jarName);
            share.putExtra(Intent.EXTRA_TEXT,
                    "Join my jar '" + jarName + "' with goal of " + goal + ". Current savings: " + saved);
            startActivity(Intent.createChooser(share, "Invite to Jar"));
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showGoalReachedDialog() {
        if (isFinishing() || isDestroyed()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_challenge_completed, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        JarFirestoreModel jar = viewModel.jar().getValue();
        ((TextView) dialogView.findViewById(R.id.tv_completed_title)).setText("Goal Reached!");
        ((TextView) dialogView.findViewById(R.id.tv_completed_desc)).setText("You've successfully saved "
                + format(jar != null ? jar.getGoalAmount() : 0) + " for " + jarName);
        MaterialButton btnContinue = dialogView.findViewById(R.id.btn_continue_success);
        btnContinue.setText("Awesome");
        btnContinue.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.archiveAfterGoal();
        });
        dialog.show();
    }

    private String describe(JarDetailsViewModel.LimitMessage limit) {
        String amount = format(limit.amount.toPesos());
        switch (limit.kind) {
            case GOAL_ALREADY_REACHED:
                return "This jar has already reached its goal.";
            case OVER_GOAL:
                return "Only " + amount + " needed to reach goal";
            case SHARE_ALREADY_PAID:
                return "You have already paid your full share (" + amount + ")";
            case OVER_SHARE:
            default:
                return "Fair share limit exceeded. You can only add up to " + amount;
        }
    }

    private String format(double amountInPHP) {
        return SettingHelper.formatAmount(this, amountInPHP);
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private static int iconFor(String iconName) {
        if (iconName == null) return R.drawable.hat;
        switch (iconName.toLowerCase(Locale.US)) {
            case "house": return R.drawable.house;
            case "plane":
            case "airplane": return R.drawable.airplane;
            case "laptop": return R.drawable.laptop;
            case "car": return R.drawable.car;
            case "game":
            case "controller": return R.drawable.controller;
            case "shirt":
            case "tshirt": return R.drawable.tshirt;
            case "book": return R.drawable.book;
            case "burger": return R.drawable.burger;
            case "gift":
            case "ic_gift": return R.drawable.ic_gift;
            default: return R.drawable.hat;
        }
    }

    // ---- Activity feed ----

    private class ActivityAdapter extends ListAdapter<JarTransaction, ActivityAdapter.Holder> {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        private Map<String, UserProfile> profiles = Collections.emptyMap();

        ActivityAdapter() {
            super(new DiffUtil.ItemCallback<JarTransaction>() {
                @Override
                public boolean areItemsTheSame(@NonNull JarTransaction a, @NonNull JarTransaction b) {
                    return Objects.equals(a.getId(), b.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull JarTransaction a, @NonNull JarTransaction b) {
                    return a.getAmount() == b.getAmount()
                            && Objects.equals(a.getNote(), b.getNote())
                            && Objects.equals(a.getUserName(), b.getUserName())
                            && Objects.equals(a.getTimestamp(), b.getTimestamp());
                }
            });
        }

        void setProfiles(Map<String, UserProfile> updated) {
            profiles = updated;
            notifyItemRangeChanged(0, getItemCount());
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            JarTransaction item = getItem(position);

            UserProfile profile = profiles.get(item.getUserId());
            String name = profile != null ? profile.displayName : item.getUserName();
            if (name == null || name.isEmpty()) name = "User";
            if (Objects.equals(item.getUserId(), viewModel.currentUserId())) {
                holder.title.setText(name + " (You)");
                holder.title.setTextColor(color);
            } else {
                holder.title.setText(name);
                holder.title.setTextColor(ContextCompat.getColor(SharedJarsDetails.this, R.color.text_primary));
            }

            if (item.getAmount() > 0) {
                holder.amount.setText("+ " + format(item.getAmount()));
                holder.amount.setTextColor(ContextCompat.getColor(SharedJarsDetails.this, R.color.green));
            } else {
                holder.amount.setText(format(item.getAmount()));
                holder.amount.setTextColor(ContextCompat.getColor(SharedJarsDetails.this, R.color.red_dark));
            }

            String note = item.getNote();
            holder.desc.setVisibility(note != null && !note.isEmpty() ? View.VISIBLE : View.GONE);
            holder.desc.setText(note);
            holder.date.setText(item.getTimestamp() != null
                    ? dateFormat.format(item.getTimestamp().toDate()) : "Just now");

            int icon, background;
            if ("CREATE".equals(item.getType())) {
                icon = R.drawable.ic_gift;
                background = R.color.purple_light;
            } else if ("ADD_PARTICIPANT".equals(item.getType())) {
                icon = R.drawable.ic_add_person;
                background = R.color.orange_light;
            } else {
                icon = R.drawable.ic_add;
                background = R.color.blue_light;
            }
            holder.icon.setImageResource(icon);
            holder.iconCard.setCardBackgroundColor(ContextCompat.getColor(SharedJarsDetails.this, background));
        }

        class Holder extends RecyclerView.ViewHolder {
            final CardView iconCard;
            final ImageView icon;
            final TextView title, date, amount, desc;

            Holder(@NonNull View itemView) {
                super(itemView);
                iconCard = itemView.findViewById(R.id.icon_background_card);
                icon = itemView.findViewById(R.id.icon_image);
                title = itemView.findViewById(R.id.text_title);
                date = itemView.findViewById(R.id.text_date);
                amount = itemView.findViewById(R.id.text_amount);
                desc = itemView.findViewById(R.id.text_desc);
            }
        }
    }
}
