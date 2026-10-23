package com.example.fintrac;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ArchivesActivity extends AppCompatActivity {

    private RecyclerView rvArchives;
    private TextView tvEmptyArchive;
    private ChipGroup chipGroupArchives;
    private ArchiveAdapter adapter;
    private List<ArchiveItem> archiveList;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String currentFilter = "jars"; // Default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archives);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        rvArchives = findViewById(R.id.rvArchives);
        tvEmptyArchive = findViewById(R.id.tvEmptyArchive);
        chipGroupArchives = findViewById(R.id.chipGroupArchives);
        ImageButton btnBack = findViewById(R.id.btn_back_archives);
        ImageButton btnDeleteAll = findViewById(R.id.btn_delete_all);

        btnBack.setOnClickListener(v -> finish());
        btnDeleteAll.setOnClickListener(v -> showDeleteAllConfirmation());

        archiveList = new ArrayList<>();
        rvArchives.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ArchiveAdapter(archiveList);
        rvArchives.setAdapter(adapter);

        chipGroupArchives.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int id = checkedIds.get(0);
                if (id == R.id.chipArchivedJars) {
                    currentFilter = "jars";
                } else if (id == R.id.chipArchivedBills) {
                    currentFilter = "bills";
                } else if (id == R.id.chipArchivedChallenges) {
                    currentFilter = "challenges";
                }
                loadArchivedItems();
            }
        });

        loadArchivedItems();
    }

    private void loadArchivedItems() {
        if (currentUser == null) return;

        archiveList.clear();
        String userId = currentUser.getUid();

        String collectionPath = "";
        if (currentFilter.equals("jars")) {
            collectionPath = "jars";
        } else if (currentFilter.equals("bills")) {
            collectionPath = "users/" + userId + "/bills";
        } else if (currentFilter.equals("challenges")) {
            collectionPath = "users/" + userId + "/challenges";
        }

        db.collection(collectionPath)
                .whereEqualTo("isArchived", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        // 🔥 ADDED THIS: Security filter for root collections like 'jars' 🔥
                        if (currentFilter.equals("jars")) {
                            List<String> participants = (List<String>) document.get("participants");
                            if (participants == null || !participants.contains(userId)) {
                                continue; // Skip jars that don't belong to this user
                            }
                        }

                        Date archivedDate = null;
                        if (document.contains("archivedAt") && document.getTimestamp("archivedAt") != null) {
                            archivedDate = document.getTimestamp("archivedAt").toDate();
                        } else {
                            archivedDate = new Date();
                        }

                        long diffInMillies = Math.abs(new Date().getTime() - archivedDate.getTime());
                        long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

                        if (diffInDays >= 30) {
                            document.getReference().delete();
                            continue;
                        }

                        long daysLeft = 30 - diffInDays;

                        String title = document.contains("name") ? document.getString("name") : "Unnamed Item";
                        String subtitle = currentFilter.substring(0, 1).toUpperCase() + currentFilter.substring(1);

                        // Kunin ang Icon at Color gamit ang Helper Class para 100% consistent!
                        int iconResId;
                        int colorResId;

                        if (currentFilter.equals("challenges")) {
                            iconResId = ChallengeIconHelper.getIcon(title);
                            colorResId = ChallengeIconHelper.getColor(title);
                        } else if (currentFilter.equals("bills")) {
                            Long logoLong = document.getLong("logoResId");
                            iconResId = (logoLong != null) ? logoLong.intValue() : R.drawable.hat;
                            colorResId = R.color.royal_purple; // Default fallback para sa bills
                        } else { // jars
                            iconResId = R.drawable.savings; // Siguraduhin na may default savings icon ka
                            colorResId = R.color.forest_green; // Default fallback para sa jars
                        }

                        archiveList.add(new ArchiveItem(
                                document.getId(),
                                title,
                                subtitle,
                                document.getReference().getPath(),
                                daysLeft,
                                iconResId,
                                colorResId
                        ));
                    }

                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("ArchivesActivity", "Failed to load archives", e);
                    Toast.makeText(this, "Failed to load archives.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        adapter.notifyDataSetChanged();
        if (archiveList.isEmpty()) {
            rvArchives.setVisibility(View.GONE);
            tvEmptyArchive.setVisibility(View.VISIBLE);
        } else {
            rvArchives.setVisibility(View.VISIBLE);
            tvEmptyArchive.setVisibility(View.GONE);
        }
    }

    private void showDeleteAllConfirmation() {
        if (archiveList.isEmpty()) {
            Toast.makeText(this, "No items to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Empty Trash")
                .setMessage("Are you sure you want to permanently delete all archived items in this tab? This cannot be undone.")
                .setPositiveButton("Empty", (dialog, which) -> {
                    for (ArchiveItem item : archiveList) {
                        db.document(item.documentPath).delete();
                    }
                    archiveList.clear();
                    updateUI();
                    Toast.makeText(this, "Archives emptied.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    static class ArchiveItem {
        String id;
        String title;
        String category;
        String documentPath;
        long daysLeft;
        int iconResId;
        int colorResId;

        ArchiveItem(String id, String title, String category, String documentPath, long daysLeft, int iconResId, int colorResId) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.documentPath = documentPath;
            this.daysLeft = daysLeft;
            this.iconResId = iconResId;
            this.colorResId = colorResId;
        }
    }

    class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ViewHolder> {
        private List<ArchiveItem> items;

        ArchiveAdapter(List<ArchiveItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archive_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ArchiveItem item = items.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvCategory.setText(item.category);
            holder.tvDaysLeft.setText("Deletes permanently in " + item.daysLeft + " days");

            Context context = holder.itemView.getContext();

            // Set the Icon properly
            holder.ivIcon.setImageResource(item.iconResId);

            // Set the Color Background properly
            try {
                int color = ContextCompat.getColor(context, item.colorResId);
                holder.cardIcon.setCardBackgroundColor(color);
            } catch (Exception e) {
                // Kung mag-fail sa pag-resolve ng color, default lang
                e.printStackTrace();
            }

            holder.itemView.setOnClickListener(v -> {
                showRestoreOrDeleteDialog(item, position);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvCategory, tvDaysLeft;
            ImageView ivIcon;
            MaterialCardView cardIcon;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_archive_title);
                tvCategory = itemView.findViewById(R.id.tv_archive_category);
                tvDaysLeft = itemView.findViewById(R.id.tv_days_left);
                ivIcon = itemView.findViewById(R.id.iv_archive_icon);
                cardIcon = itemView.findViewById(R.id.card_icon);
            }
        }
    }

    private void showRestoreOrDeleteDialog(ArchiveItem item, int position) {
        String restoreOptionText = item.category.equalsIgnoreCase("challenges") ? "Restore Challenge to Complete" : "Restore Item";
        String[] options = {restoreOptionText, "Delete Permanently"};

        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        db.document(item.documentPath).update("isArchived", false).addOnSuccessListener(aVoid -> {
                            archiveList.remove(position);
                            updateUI();
                            Toast.makeText(this, "Item restored.", Toast.LENGTH_SHORT).show();
                        });
                    } else if (which == 1) {
                        db.document(item.documentPath).delete().addOnSuccessListener(aVoid -> {
                            archiveList.remove(position);
                            updateUI();
                            Toast.makeText(this, "Item deleted permanently.", Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .show();
    }
}