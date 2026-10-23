package com.example.fintrac;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/** Jars list. DiffUtil-backed, so live updates only redraw the jars that changed. */
public class JarAdapter extends ListAdapter<JarItem, JarAdapter.JarViewHolder> {

    private static final DiffUtil.ItemCallback<JarItem> DIFF = new DiffUtil.ItemCallback<JarItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull JarItem oldItem, @NonNull JarItem newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull JarItem oldItem, @NonNull JarItem newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    public JarAdapter() {
        super(DIFF);
    }

    public JarItem getJarAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public JarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_jar, parent, false);
        return new JarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JarViewHolder holder, int position) {
        JarItem jar = getItem(position);
        Context context = holder.itemView.getContext();

        int progress = jar.getProgressPercentage();
        holder.progressBar.setMax(100);
        holder.progressBar.setProgress(progress);
        holder.progressPercentage.setText(context.getString(R.string.percent_complete, progress));

        // Amounts are stored in base PHP; formatAmount converts to the display currency
        holder.savedAmount.setText(SettingHelper.formatAmount(context, jar.getSavedAmount()));
        holder.goalAmount.setText(SettingHelper.formatAmount(context, jar.getGoalAmount()));
        holder.weeklyAmount.setText(context.getString(R.string.weekly_amount_format,
                SettingHelper.formatAmount(context, jar.getGoalAmount() / 52.0)));
        holder.daysLeft.setText(jar.getDaysLeft());

        holder.jarIcon.setImageResource(iconFor(jar.getIconName()));
        holder.jarIcon.setBackgroundTintList(ColorStateList.valueOf(jar.getJarColor()));
        holder.progressBar.setProgressTintList(ColorStateList.valueOf(jar.getJarColor()));

        if (jar.isLocked()) {
            holder.itemView.setAlpha(0.6f);
            holder.jarTitle.setText("🔒 " + jar.getName());
            holder.jarTypeBadge.setText("LOCKED");
            holder.jarTypeBadge.setBackgroundResource(R.drawable.bg_shared_badge);
            holder.jarTypeBadge.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.jarTitle.setText(jar.getName());
            holder.jarTypeBadge.setText(jar.getType());
            holder.jarTypeBadge.setBackgroundTintList(null);
            holder.jarTypeBadge.setBackgroundResource(badgeFor(jar.getType()));
        }

        holder.itemView.setOnClickListener(v -> {
            if (jar.isLocked()) {
                Toast.makeText(context, "Locked! Please put initial savings in your Mandatory jar first.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(context, SharedJarsDetails.class);
            intent.putExtra("JAR_ID", jar.getId());
            intent.putExtra("JAR_NAME", jar.getName());
            intent.putExtra("JAR_SAVED", jar.getSavedAmount());
            intent.putExtra("JAR_GOAL", jar.getGoalAmount());
            intent.putExtra("ICON_NAME", jar.getIconName());
            intent.putExtra("COLOR", jar.getJarColor());
            intent.putExtra("JAR_TYPE", jar.getType());
            context.startActivity(intent);
        });
    }

    private static int badgeFor(String type) {
        if ("Mandatory".equalsIgnoreCase(type)) return R.drawable.bg_mandatory_badge;
        if ("Flexible".equalsIgnoreCase(type)) return R.drawable.bg_flexible_badge;
        return R.drawable.bg_shared_badge;
    }

    private static int iconFor(String iconName) {
        if (iconName == null) return R.drawable.hat;
        switch (iconName) {
            case "house": return R.drawable.house;
            case "airplane": return R.drawable.airplane;
            case "laptop": return R.drawable.laptop;
            case "car": return R.drawable.car;
            case "controller": return R.drawable.controller;
            case "tshirt": return R.drawable.tshirt;
            case "book": return R.drawable.book;
            case "burger": return R.drawable.burger;
            case "ic_gift": return R.drawable.ic_gift;
            default: return R.drawable.hat;
        }
    }

    static class JarViewHolder extends RecyclerView.ViewHolder {
        final TextView jarTitle, jarTypeBadge, savedAmount, goalAmount, daysLeft, weeklyAmount, progressPercentage;
        final ProgressBar progressBar;
        final ImageView jarIcon;

        JarViewHolder(@NonNull View itemView) {
            super(itemView);
            jarTitle = itemView.findViewById(R.id.jar_title);
            jarTypeBadge = itemView.findViewById(R.id.jar_type_badge);
            savedAmount = itemView.findViewById(R.id.saved_amount);
            goalAmount = itemView.findViewById(R.id.goal_amount);
            daysLeft = itemView.findViewById(R.id.days_left);
            weeklyAmount = itemView.findViewById(R.id.weekly_amount);
            progressBar = itemView.findViewById(R.id.progress_bar);
            jarIcon = itemView.findViewById(R.id.jar_icon);
            progressPercentage = itemView.findViewById(R.id.progress_percentage);
        }
    }
}
