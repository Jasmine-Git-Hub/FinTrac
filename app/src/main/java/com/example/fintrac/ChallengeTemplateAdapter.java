package com.example.fintrac;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ChallengeTemplateAdapter extends RecyclerView.Adapter<ChallengeTemplateAdapter.ViewHolder> {

    private final List<ChallengeTemplate> templateList;
    private int selectedPosition = -1;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ChallengeTemplateAdapter(List<ChallengeTemplate> templateList) {
        this.templateList = templateList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        if (oldPosition != -1) notifyItemChanged(oldPosition);
        if (selectedPosition != -1) notifyItemChanged(selectedPosition);
    }

    public ChallengeTemplate getSelectedChallenge() {
        if (selectedPosition != -1 && selectedPosition < templateList.size()) {
            return templateList.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_challenge_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChallengeTemplate template = templateList.get(position);

        holder.tvTitle.setText(template.getName());
        holder.tvDescription.setText(template.getDescription());

        String durationText = template.getDurationDays() + " days";

        // GINAMIT NA NATIN ANG SETTINGHELPER DITO PARA DYNAMIC ANG CURRENCY
        String formattedAmount = SettingHelper.formatAmount(holder.itemView.getContext(), template.getTargetAmount());
        String savingsText = "Save " + formattedAmount;

        holder.tvDuration.setText(durationText);
        holder.tvSavings.setText(savingsText);

        // Highlight selected item
        boolean isSelected = (position == selectedPosition);
        holder.cardView.setStrokeWidth(isSelected ? 4 : 1);

        int strokeColor;
        if (isSelected) {
            strokeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.blue);
        } else {
            strokeColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.light_gray);
        }
        holder.cardView.setStrokeColor(strokeColor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return templateList.size();
    }

    // Made ViewHolder static and private to fix visibility scope
    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView tvTitle, tvDescription, tvDuration, tvSavings;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_challenge);
            tvTitle = itemView.findViewById(R.id.tv_challenge_title);
            tvDescription = itemView.findViewById(R.id.tv_challenge_desc);
            tvDuration = itemView.findViewById(R.id.tv_challenge_duration);
            tvSavings = itemView.findViewById(R.id.tv_challenge_savings);
        }
    }
}