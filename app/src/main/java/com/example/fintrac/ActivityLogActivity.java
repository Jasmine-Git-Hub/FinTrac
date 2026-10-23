package com.example.fintrac;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ActivityLogActivity extends BaseActivity {
    private static final String TAG = "ActivityLogActivity";
    private TextView titleView;
    private TextView messageView;
    private RecyclerView recyclerView;
    private ActivityLogAdapter adapter;

    private JarFirestoreManager jarFirestoreManager;
    private String jarId;
    private String jarName;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_log);

        jarFirestoreManager = new JarFirestoreManager();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Get Data from Intent
        jarId = getIntent().getStringExtra("JAR_ID");
        jarName = getIntent().getStringExtra("JAR_NAME");

        initializeViews();
        setupBackButton();

        if (jarId != null && !jarId.isEmpty()) {
            loadActivityLog();
        } else {
            showEmptyState();
        }
    }

    private void initializeViews() {
        titleView = findViewById(R.id.title_text);
        messageView = findViewById(R.id.message);
        recyclerView = findViewById(R.id.recycler_view_activity_log);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (titleView != null) {
            if (jarName != null && !jarName.isEmpty()) {
                titleView.setText(jarName + " - Activity Log");
            } else {
                titleView.setText("Activity Log");
            }
        }
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void loadActivityLog() {
        jarFirestoreManager.getTransactions(jarId, new JarFirestoreManager.FirestoreCallback<List<JarTransaction>>() {
            @Override
            public void onSuccess(List<JarTransaction> transactions) {
                runOnUiThread(() -> displayTransactions(transactions));
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error loading transactions", e);
                runOnUiThread(() -> {
                    messageView.setText("Error loading activity log: " + e.getMessage());
                    messageView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
            }
        });
    }

    private void displayTransactions(List<JarTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            showEmptyState();
            return;
        }

        messageView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        adapter = new ActivityLogAdapter(this, transactions, currentUser);
        recyclerView.setAdapter(adapter);
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        messageView.setVisibility(View.VISIBLE);
        messageView.setText("No activity recorded yet.\n\n" +
                "Add money to your jar or invite participants to see activity here.");
    }

    // --- INNER CLASS ADAPTER PARA SA RECYCLERVIEW ---
    public static class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.ViewHolder> {
        private final List<JarTransaction> list;
        private final Context context;
        private final FirebaseUser currentUser;
        private final SimpleDateFormat dateFormat;

        public ActivityLogAdapter(Context context, List<JarTransaction> list, FirebaseUser currentUser) {
            this.context = context;
            this.list = list;
            this.currentUser = currentUser;
            this.dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_jar_activity, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JarTransaction transaction = list.get(position);

            String userName = transaction.getUserName();
            if (userName == null || userName.isEmpty()) {
                userName = "Someone";
            }

            String type = transaction.getType();
            double amount = transaction.getAmount();
            String note = transaction.getNote();

            String titleText = "";
            int iconRes = R.drawable.ic_notifications;
            String bgColor = "#F1F3F4"; // Default Gray
            String tintColor = "#5F6368";

            // Check if this transaction belongs to current user
            boolean isCurrentUserTransaction = currentUser != null &&
                    transaction.getUserId() != null &&
                    currentUser.getUid().equals(transaction.getUserId());

            // LOGIC PARA SA UI STYLING DEPENDE SA ACTION
            if ("CREATE".equals(type)) {
                titleText = userName + " created this jar";
                if (amount > 0) {
                    if (isCurrentUserTransaction) {
                        // Show actual amount for current user's own creation
                        titleText += " with initial amount of " + SettingHelper.formatAmount(context, amount);
                    } else {
                        // Mask amount for other users' creations
                        titleText += " with initial amount of ****";
                    }
                }
                iconRes = R.drawable.ic_goal;
                bgColor = "#FEF7E0"; // Light Yellow
                tintColor = "#F29900";

            } else if ("ADD_MONEY".equals(type)) {
                if (isCurrentUserTransaction) {
                    // Show actual amount for current user's own transactions
                    titleText = userName + " added " + SettingHelper.formatAmount(context, amount);
                } else {
                    // Mask amount for other users' transactions
                    titleText = userName + " added ****";
                }
                iconRes = R.drawable.ic_add;
                bgColor = "#E6F4EA"; // Light Green
                tintColor = "#4CAF50"; // Green

            } else if ("ADD_PARTICIPANT".equals(type)) {
                titleText = userName + " " + (note != null ? note : "joined the jar");
                iconRes = R.drawable.ic_add_person;
                bgColor = "#E8F0FE"; // Light Blue
                tintColor = "#1976D2"; // Blue

            } else {
                titleText = userName + " updated the jar";
            }

            // Set Title
            holder.tvTitle.setText(titleText);

            // Set Note if it exists and is meaningful
            if (note != null && !note.isEmpty() && !note.equals("Added money to jar") && !type.equals("ADD_PARTICIPANT")) {
                holder.tvNote.setVisibility(View.VISIBLE);
                holder.tvNote.setText("Note: " + note);
            } else {
                holder.tvNote.setVisibility(View.GONE);
            }

            // Set Timestamp
            if (transaction.getTimestamp() != null) {
                holder.tvTime.setText(dateFormat.format(transaction.getTimestamp().toDate()));
            } else {
                holder.tvTime.setText("Just now");
            }

            // Apply Icon and Colors
            holder.activityIcon.setImageResource(iconRes);

            // Adjust for Dark Mode (if applicable)
            if (SettingHelper.isDarkMode(context)) {
                holder.iconCard.setCardBackgroundColor(Color.parseColor(tintColor));
                holder.activityIcon.setColorFilter(Color.WHITE);
            } else {
                holder.iconCard.setCardBackgroundColor(Color.parseColor(bgColor));
                holder.activityIcon.setColorFilter(Color.parseColor(tintColor));
            }
        }

        @Override
        public int getItemCount() {
            return list == null ? 0 : list.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvNote, tvTime;
            CardView iconCard;
            ImageView activityIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_activity_title);
                tvNote = itemView.findViewById(R.id.tv_activity_note);
                tvTime = itemView.findViewById(R.id.tv_activity_time);
                iconCard = itemView.findViewById(R.id.icon_card);
                activityIcon = itemView.findViewById(R.id.activity_icon);
            }
        }
    }
}