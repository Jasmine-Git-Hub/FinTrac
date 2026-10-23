package com.example.fintrac;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.List;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {

    private final List<ChallengeItem> challengeList;
    private final Context context;
    private final OnChallengeActionListener actionListener;

    public interface OnChallengeActionListener {
        void onCheckIn(ChallengeItem item, int position);
        void onGiveUp(ChallengeItem item, int position);
    }

    public ChallengeAdapter(List<ChallengeItem> challengeList, Context context, OnChallengeActionListener listener) {
        this.challengeList = challengeList;
        this.context = context;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
        return new ChallengeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
        ChallengeItem challenge = challengeList.get(position);

        holder.tvName.setText(challenge.getName());
        holder.tvDesc.setText(challenge.getDescription());

        int current = challenge.getCurrentDay();
        int total = challenge.getTotalDays();
        int progress = (int) (((double) current / total) * 100);

        holder.pbChallenge.setProgress(progress);

        // Show exact percentage
        holder.tvPercentDone.setText(progress + "% (" + current + "/" + total + ")");
        holder.tvDaysLeft.setText((total - current) + " days left");

        holder.tvProgressStart.setText("Day " + current);
        holder.tvProgressEnd.setText("Day " + total);

        // Eto yung fix: I-set yung tamang icon galing sa helper at tanggalin yung tint
        holder.ivIcon.setImageResource(challenge.getIconResId());
        holder.ivIcon.clearColorFilter();

        holder.itemView.setOnClickListener(v -> showChallengeDetailsDialog(challenge, position));
    }

    /**
     * Checks if check-in is available based on 8AM daily reset.
     * A new "check-in day" starts at 8:00 AM, not midnight.
     */
    private boolean isCheckInAvailableToday(ChallengeItem challenge) {
        long lastCheckIn = challenge.getLastCheckInTime();
        if (lastCheckIn == 0) return true;

        long now = System.currentTimeMillis();

        // Get the 8AM boundary for today
        Calendar today8AM = Calendar.getInstance();
        today8AM.set(Calendar.HOUR_OF_DAY, 8);
        today8AM.set(Calendar.MINUTE, 0);
        today8AM.set(Calendar.SECOND, 0);
        today8AM.set(Calendar.MILLISECOND, 0);

        // If current time is before 8AM today, use yesterday's 8AM as the boundary
        if (now < today8AM.getTimeInMillis()) {
            today8AM.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Check-in is available if last check-in was BEFORE the most recent 8AM boundary
        return lastCheckIn < today8AM.getTimeInMillis();
    }

    private void showChallengeDetailsDialog(ChallengeItem challenge, int position) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_challenge_detail, null);
            builder.setView(view);
            AlertDialog dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            TextView tvName = view.findViewById(R.id.tv_challenge_name);
            TextView tvDesc = view.findViewById(R.id.tv_challenge_desc);
            TextView tvCurrentDay = view.findViewById(R.id.tv_current_day);
            TextView tvSavedAmount = view.findViewById(R.id.tv_saved_amount);
            ImageView ivIcon = view.findViewById(R.id.iv_challenge_icon);
            ImageView btnClose = view.findViewById(R.id.btn_close);
            MaterialButton btnCheckIn = view.findViewById(R.id.btn_check_in);
            MaterialButton btnGiveUp = view.findViewById(R.id.btn_give_up);
            MaterialCardView cardIconContainer = view.findViewById(R.id.card_icon_container);

            tvName.setText(challenge.getName());
            tvDesc.setText(challenge.getDescription());
            tvCurrentDay.setText("Day " + challenge.getCurrentDay() + " of " + challenge.getTotalDays());

            // Calculate exact saved amount based on days completed
            double dailySaveAmount = challenge.getTargetAmount() / challenge.getTotalDays();
            double savedValue = dailySaveAmount * challenge.getCurrentDay();
            tvSavedAmount.setText(SettingHelper.formatAmount(context, savedValue));

            // Dark mode text colors
            boolean isDark = SettingHelper.isDarkMode(context);
            int primaryTextColor = isDark ? Color.WHITE : Color.parseColor("#37474F");
            int secondaryTextColor = isDark ? Color.parseColor("#B0BEC5") : Color.parseColor("#546E7A");

            tvName.setTextColor(primaryTextColor);
            tvDesc.setTextColor(secondaryTextColor);
            tvCurrentDay.setTextColor(primaryTextColor);

            // Eto yung fix sa dialog: I-set yung tamang icon tsaka tanggalin yung hardcoded tint
            ivIcon.setImageResource(challenge.getIconResId());
            ivIcon.clearColorFilter();

            cardIconContainer.setCardBackgroundColor(isDark ? Color.parseColor("#1DE9B6") : Color.parseColor("#E0F2F1"));
            if (isDark) cardIconContainer.setAlpha(0.2f);

            // ============================================================
            // CHECK-IN BUTTON: Disable if already checked in today (8AM reset)
            // ============================================================
            boolean canCheckIn = isCheckInAvailableToday(challenge);
            btnCheckIn.setEnabled(canCheckIn);
            if (!canCheckIn) {
                btnCheckIn.setText("✓ Checked In Today");
                btnCheckIn.setAlpha(0.5f);

                // Show next available check-in time hint
                Calendar next8AM = Calendar.getInstance();
                next8AM.set(Calendar.HOUR_OF_DAY, 8);
                next8AM.set(Calendar.MINUTE, 0);
                next8AM.set(Calendar.SECOND, 0);
                next8AM.set(Calendar.MILLISECOND, 0);
                // If it's past 8AM, next reset is tomorrow 8AM
                if (System.currentTimeMillis() >= next8AM.getTimeInMillis()) {
                    next8AM.add(Calendar.DAY_OF_YEAR, 1);
                }
                tvCurrentDay.setText("Day " + challenge.getCurrentDay() + " of " + challenge.getTotalDays()
                        + "\nNext check-in at 8:00 AM");
            } else {
                // Show how much will be saved on check-in
                double dailyAmount = challenge.getTargetAmount() / challenge.getTotalDays();
                String formattedDaily = SettingHelper.formatAmount(context, dailyAmount);
                btnCheckIn.setText("Check In Today (+" + formattedDaily + ")");
                btnCheckIn.setAlpha(1.0f);
            }

            btnCheckIn.setOnClickListener(v -> {
                if (!isCheckInAvailableToday(challenge)) {
                    Toast.makeText(context, "Already checked in today! Come back after 8:00 AM.", Toast.LENGTH_SHORT).show();
                    return;
                }
                actionListener.onCheckIn(challenge, position);
                dialog.dismiss();
            });

            btnGiveUp.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Challenge")
                        .setMessage("Are you sure you want to delete this challenge?")
                        .setPositiveButton("Yes", (d, w) -> {
                            actionListener.onGiveUp(challenge, position);
                            dialog.dismiss();
                        })
                        .setNegativeButton("No", null)
                        .show();
            });

            btnClose.setOnClickListener(v -> dialog.dismiss());
            dialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return challengeList.size();
    }

    public static class ChallengeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvDaysLeft, tvPercentDone, tvProgressStart, tvProgressEnd;
        ProgressBar pbChallenge;
        ImageView ivIcon;

        public ChallengeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_challenge_name);
            tvDesc = itemView.findViewById(R.id.tv_challenge_desc);
            tvDaysLeft = itemView.findViewById(R.id.tv_days_left);
            tvPercentDone = itemView.findViewById(R.id.tv_percent_done);
            pbChallenge = itemView.findViewById(R.id.pb_challenge);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvProgressStart = itemView.findViewById(R.id.tv_progress_start);
            tvProgressEnd = itemView.findViewById(R.id.tv_progress_end);
        }
    }
}