package com.example.fintrac;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fintrac.archives.ArchivesViewModel;
import com.example.fintrac.data.ArchiveRepository.Entry;
import com.example.fintrac.data.ArchiveRepository.Kind;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/** Archived jars, bills and challenges. Items are deleted automatically after 30 days. */
public class ArchivesActivity extends BaseActivity {

    private ArchivesViewModel viewModel;
    private ArchiveAdapter adapter;
    private RecyclerView list;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archives);
        viewModel = new ViewModelProvider(this).get(ArchivesViewModel.class);

        list = findViewById(R.id.rvArchives);
        tvEmpty = findViewById(R.id.tvEmptyArchive);
        adapter = new ArchiveAdapter();
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.btn_back_archives).setOnClickListener(v -> finish());
        View deleteAll = findViewById(R.id.btn_delete_all);
        deleteAll.setContentDescription("Delete all archived items in this tab");
        deleteAll.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Empty Trash")
                .setMessage("Are you sure you want to permanently delete all archived items in this tab? This cannot be undone.")
                .setPositiveButton("Empty", (d, w) -> viewModel.deleteAll())
                .setNegativeButton("Cancel", null)
                .show());

        ChipGroup chips = findViewById(R.id.chipGroupArchives);
        chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            viewModel.show(id == R.id.chipArchivedBills ? Kind.BILLS
                    : id == R.id.chipArchivedChallenges ? Kind.CHALLENGES : Kind.JARS);
        });

        viewModel.state().observe(this, state -> {
            adapter.submitList(new ArrayList<>(state.entries));
            boolean empty = !state.loading && state.entries.isEmpty();
            list.setVisibility(empty || state.error != null ? View.GONE : View.VISIBLE);
            tvEmpty.setVisibility(empty || state.error != null ? View.VISIBLE : View.GONE);
            if (state.error != null) tvEmpty.setText(state.error);
            else if (empty) tvEmpty.setText("No archived items here.");
        });
        viewModel.messages().observe(this, event -> {
            String text = event.consume();
            if (text != null) Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        });
    }

    private void showOptions(Entry entry) {
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        if (entry.kind == Kind.CHALLENGES && entry.finished) {
            // Nothing to resume: all days are done
        } else {
            labels.add(entry.kind == Kind.CHALLENGES ? "Restore Challenge to Complete"
                    : entry.kind == Kind.BILLS ? "Restore Bill (as unpaid)" : "Restore Jar");
            actions.add(() -> viewModel.restore(entry));
        }
        if (entry.kind == Kind.JARS && !entry.ownedByMe) {
            labels.add("Remove from My Archives");
        } else {
            labels.add("Delete Permanently");
        }
        actions.add(() -> viewModel.delete(entry));

        new AlertDialog.Builder(this)
                .setTitle(entry.title)
                .setItems(labels.toArray(new String[0]), (d, which) -> actions.get(which).run())
                .show();
    }

    private class ArchiveAdapter extends ListAdapter<Entry, ArchiveAdapter.Holder> {

        ArchiveAdapter() {
            super(new DiffUtil.ItemCallback<Entry>() {
                @Override
                public boolean areItemsTheSame(@NonNull Entry a, @NonNull Entry b) {
                    return a.kind == b.kind && a.id.equals(b.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Entry a, @NonNull Entry b) {
                    return a.title.equals(b.title) && ArchivesViewModel.daysLeft(a) == ArchivesViewModel.daysLeft(b);
                }
            });
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archive_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Entry entry = getItem(position);
            holder.title.setText(entry.title);

            int icon, color;
            String category;
            switch (entry.kind) {
                case CHALLENGES:
                    icon = ChallengeIconHelper.getIcon(entry.title);
                    color = ChallengeIconHelper.getColor(entry.title);
                    category = entry.finished ? "Challenges • Completed" : "Challenges";
                    break;
                case BILLS:
                    icon = R.drawable.ic_visual_reports;
                    color = R.color.royal_purple;
                    category = "Bills";
                    break;
                case JARS:
                default:
                    icon = R.drawable.savings;
                    color = R.color.forest_green;
                    category = entry.ownedByMe ? "Jars" : "Jars • Shared with you";
                    break;
            }
            holder.category.setText(category);
            holder.icon.setImageResource(icon);
            holder.iconCard.setCardBackgroundColor(ContextCompat.getColor(ArchivesActivity.this, color));

            long days = ArchivesViewModel.daysLeft(entry);
            holder.daysLeft.setText(days == 1 ? "Deletes permanently in 1 day" : "Deletes permanently in " + days + " days");
            holder.itemView.setOnClickListener(v -> showOptions(entry));
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView title, category, daysLeft;
            final ImageView icon;
            final MaterialCardView iconCard;

            Holder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.tv_archive_title);
                category = itemView.findViewById(R.id.tv_archive_category);
                daysLeft = itemView.findViewById(R.id.tv_days_left);
                icon = itemView.findViewById(R.id.iv_archive_icon);
                iconCard = itemView.findViewById(R.id.card_icon);
            }
        }
    }
}
