package com.example.fintrac;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Transaction> transactions;
    private Context context;

    public HistoryAdapter(List<Transaction> transactions, Context context) {
        this.transactions = transactions;
        this.context = context;
    }

    public void setTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction item = transactions.get(position);

        holder.tvTitle.setText(item.getTitle());

        // Piliin ang subtitle (kung walang description, category ang ilalabas)
        String subtitle = item.getDescription();
        if (subtitle == null || subtitle.isEmpty()) {
            subtitle = item.getCategory();
        }
        holder.tvSubtitle.setText(subtitle);

        // Gumamit ng Formatted Date at Time galing sa Transaction.java mo
        holder.tvTime.setText(item.getFormattedDate() + " • " + item.getFormattedTime());

        // Dynamic Currency Format
        String formattedAmount = SettingHelper.formatAmount(context, item.getAmount());

        // Don't show zero amounts (extra safety)
        if (item.getAmount() == 0.0) {
            holder.tvAmount.setText("₱0.00");
            holder.tvAmount.setTextColor(Color.GRAY);
        } else if (!item.isExpense()) { // Income
            holder.tvAmount.setText("+" + formattedAmount);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.green));
            holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.green));
        } else { // Expense
            holder.tvAmount.setText("-" + formattedAmount);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.red));
            holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
            holder.ivIcon.setColorFilter(ContextCompat.getColor(context, R.color.red));
        }

        // Icon by what the entry is: bill payments and savings get their own icons,
        // everything else an up (income) or down (expense) arrow, matching Home
        String category = item.getCategory() != null ? item.getCategory().toLowerCase(java.util.Locale.US) : "";
        String title = item.getTitle() != null ? item.getTitle() : "";
        if (category.equals("bills") || title.startsWith("Paid Bill: ") || title.startsWith("Paid: ")) {
            holder.ivIcon.setImageResource(R.drawable.house);
        } else if (category.equals("jars") || category.equals("challenges") || category.equals("savings")
                || title.startsWith("Saved to Jar: ")) {
            holder.ivIcon.setImageResource(R.drawable.piggy);
        } else {
            holder.ivIcon.setImageResource(item.isExpense() ? R.drawable.ic_arrow_down : R.drawable.ic_arrow_up);
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvTime, tvAmount;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}