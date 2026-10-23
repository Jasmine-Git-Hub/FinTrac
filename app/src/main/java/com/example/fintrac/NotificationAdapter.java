package com.example.fintrac;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> notifications;
    private Context context;
    private OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onAcceptInvite(NotificationItem notification);
        void onDeclineInvite(NotificationItem notification);
        void onNotificationClick(NotificationItem notification);
        void onMarkAsRead(NotificationItem notification);
    }

    public NotificationAdapter(Context context, List<NotificationItem> notifications,
                               OnNotificationActionListener listener) {
        this.context = context;
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ginamit na natin yung bago mong layout dito
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_notification_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem notification = notifications.get(position);

        // Set text views map based on your new layout IDs
        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());
        holder.timeText.setText(getTimeAgo(notification.getTimestamp()));

        // Default visibilities
        holder.actionButtons.setVisibility(View.GONE);
        holder.btnDismiss.setVisibility(View.GONE);
        holder.dueTag.setVisibility(View.GONE);

        // Kung "Done" (isRead = true) na siya, wag na ipakita ang dismiss button
        if (!notification.isRead()) {
            holder.btnDismiss.setVisibility(View.VISIBLE);
        }

        int iconResId;
        int iconColor;
        switch (notification.getType()) {
            case "BILL_PAID":
                iconResId = R.drawable.ic_visual_reports;
                iconColor = R.color.green;
                break;
            case "JAR_INVITE":
                iconResId = R.drawable.ic_add_person;
                iconColor = R.color.blue;
                if (!notification.isRead()) {
                    holder.actionButtons.setVisibility(View.VISIBLE);
                    holder.btnDismiss.setVisibility(View.GONE); // Hide dismiss if invite, use accept/decline instead
                }
                break;
            case "BILL_DUE":
                iconResId = R.drawable.ic_visual_reports;
                iconColor = R.color.red;
                holder.dueTag.setVisibility(View.VISIBLE);
                holder.dueTag.setText("BILL DUE");
                break;
            case "JAR_GOAL_REACHED":
                iconResId = R.drawable.ic_goal;
                iconColor = R.color.green;
                break;
            case "JAR_DEADLINE":
                iconResId = R.drawable.ic_notifications;
                iconColor = R.color.red;
                holder.dueTag.setVisibility(View.VISIBLE);
                holder.dueTag.setText("DUE SOON");
                break;
            case "CHALLENGE_REMINDER":
                iconResId = R.drawable.ic_notifications;
                iconColor = R.color.jar_color_orange;
                break;
            case "CHALLENGE_DEADLINE":
                iconResId = R.drawable.ic_goal;
                iconColor = R.color.red;
                holder.dueTag.setVisibility(View.VISIBLE);
                holder.dueTag.setText("URGENT");
                break;
            default:
                iconResId = R.drawable.ic_notifications;
                iconColor = R.color.gray;
                break;
        }

        holder.icon.setImageResource(iconResId);
        holder.iconCard.setCardBackgroundColor(ContextCompat.getColor(context, iconColor));
        holder.unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        // Click Listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(notification);
        });

        if (holder.btnAccept != null) {
            holder.btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAcceptInvite(notification);
            });
        }

        if (holder.btnDecline != null) {
            holder.btnDecline.setOnClickListener(v -> {
                if (listener != null) listener.onDeclineInvite(notification);
            });
        }

        // Action for Dismiss Button
        if (holder.btnDismiss != null) {
            holder.btnDismiss.setOnClickListener(v -> {
                if (listener != null) {
                    // Mark as read pushes it to the Done tab
                    listener.onMarkAsRead(notification);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String getTimeAgo(Date date) {
        if (date == null) return "";
        long now = System.currentTimeMillis();
        long diff = now - date.getTime();
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) return "Just now";
        else if (minutes < 60) return minutes + "m ago";
        else if (hours < 24) return hours + "h ago";
        else if (days < 7) return days + "d ago";
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(date);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView iconCard;
        ImageView icon;
        TextView title, message, timeText, dueTag;
        LinearLayout actionButtons;
        MaterialButton btnAccept, btnDecline, btnDismiss;
        View unreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Updated mapping to match item_notification_reminder.xml
            iconCard = itemView.findViewById(R.id.icon_card);
            icon = itemView.findViewById(R.id.notification_icon);
            title = itemView.findViewById(R.id.tv_reminder_title);
            message = itemView.findViewById(R.id.tv_deadline); // Reusing deadline text for message
            timeText = itemView.findViewById(R.id.time_text);
            dueTag = itemView.findViewById(R.id.tv_due_tag);
            actionButtons = itemView.findViewById(R.id.action_buttons_container);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
            btnDismiss = itemView.findViewById(R.id.btn_dismiss);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }
}   