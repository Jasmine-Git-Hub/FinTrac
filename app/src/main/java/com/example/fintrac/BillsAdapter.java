package com.example.fintrac;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Bills list. Uses DiffUtil, so submitting a new list only redraws rows that actually changed.
 * Rows are bound from immutable snapshots coming from {@link com.example.fintrac.bills.BillsViewModel}.
 */
public class BillsAdapter extends ListAdapter<Bill, BillsAdapter.BillViewHolder> {

    public interface BillActionListener {
        void onBillPaidToggled(Bill bill);
    }

    private static final DiffUtil.ItemCallback<Bill> DIFF = new DiffUtil.ItemCallback<Bill>() {
        @Override
        public boolean areItemsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return oldItem.id != null && oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Bill oldItem, @NonNull Bill newItem) {
            return oldItem.sameContentAs(newItem);
        }
    };

    private final BillActionListener listener;

    public BillsAdapter(BillActionListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public Bill getBillAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bill, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        Bill bill = getItem(position);
        Context context = holder.itemView.getContext();

        holder.tvName.setText(bill.name);
        holder.tvCategory.setText(context.getString(R.string.bill_category_frequency,
                bill.category != null ? bill.category : "",
                bill.frequency != null ? bill.frequency : ""));
        holder.tvDate.setText(bill.dueDate);
        holder.rbPaid.setChecked(bill.isPaid);
        holder.rbPaid.setContentDescription(context.getString(
                bill.isPaid ? R.string.bill_mark_unpaid : R.string.bill_mark_paid, bill.name));

        // Stored amount is in base PHP; formatAmount converts to the user's display currency
        holder.tvAmount.setText(SettingHelper.formatAmount(context, bill.money().toPesos()));

        if (bill.isPaid) {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemView.setAlpha(0.5f);
        } else {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.itemView.setAlpha(1.0f);
        }

        if (bill.logoResId != 0) {
            holder.imgLogo.setImageResource(bill.logoResId);
            holder.imgLogo.setVisibility(View.VISIBLE);
        } else {
            holder.imgLogo.setVisibility(View.INVISIBLE);
        }

        holder.rbPaid.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                // Revert the visual toggle; the row redraws from the real state once Firestore confirms
                holder.rbPaid.setChecked(getItem(pos).isPaid);
                listener.onBillPaidToggled(getItem(pos));
            }
        });
    }

    static class BillViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName, tvCategory, tvAmount, tvDate;
        final ImageView imgLogo;
        final RadioButton rbPaid;

        BillViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvBillName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount = itemView.findViewById(R.id.tvBillAmount);
            tvDate = itemView.findViewById(R.id.tvDueDate);
            imgLogo = itemView.findViewById(R.id.ivBillLogo);
            rbPaid = itemView.findViewById(R.id.rbSelect);
        }
    }
}
