package com.example.fintrac;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

/** Active challenges list. Tapping a row opens its details (handled by the screen). */
public class ChallengeAdapter extends ListAdapter<ChallengeItem, ChallengeAdapter.ChallengeViewHolder> {

    public interface OnChallengeClickListener {
        void onChallengeClicked(ChallengeItem item);
    }

    private static final DiffUtil.ItemCallback<ChallengeItem> DIFF = new DiffUtil.ItemCallback<ChallengeItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChallengeItem a, @NonNull ChallengeItem b) {
            return Objects.equals(a.getDocumentId(), b.getDocumentId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChallengeItem a, @NonNull ChallengeItem b) {
            return a.sameContentAs(b);
        }
    };

    private final OnChallengeClickListener listener;

    public ChallengeAdapter(OnChallengeClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
        return new ChallengeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
        ChallengeItem challenge = getItem(position);
        int current = challenge.getCurrentDay();
        int total = Math.max(challenge.getTotalDays(), 1);
        int progress = Math.min(current * 100 / total, 100);

        holder.tvName.setText(challenge.getName());
        holder.tvDesc.setText(challenge.getDescription());
        holder.pbChallenge.setProgress(progress);
        holder.tvPercentDone.setText(progress + "% (" + current + "/" + total + ")");
        holder.tvDaysLeft.setText((total - current) + " days left");
        holder.tvProgressStart.setText("Day " + current);
        holder.tvProgressEnd.setText("Day " + total);
        holder.ivIcon.setImageResource(challenge.getIconResId());
        holder.ivIcon.clearColorFilter();

        holder.itemView.setContentDescription(challenge.getName() + ", day " + current + " of " + total);
        holder.itemView.setOnClickListener(v -> listener.onChallengeClicked(challenge));
    }

    static class ChallengeViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvDesc, tvDaysLeft, tvPercentDone, tvProgressStart, tvProgressEnd;
        final ProgressBar pbChallenge;
        final ImageView ivIcon;

        ChallengeViewHolder(@NonNull View itemView) {
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
