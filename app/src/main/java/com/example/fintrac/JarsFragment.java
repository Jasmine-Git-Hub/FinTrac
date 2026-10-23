package com.example.fintrac;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.fintrac.core.Money;
import com.example.fintrac.jars.JarsUiState;
import com.example.fintrac.jars.JarsViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Jars screen. Draws {@link JarsUiState} from {@link JarsViewModel}; the list updates live,
 * so returning from a jar's details or another device's change needs no manual reload.
 */
public class JarsFragment extends Fragment {

    private JarsViewModel viewModel;
    private JarAdapter adapter;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView totalSaved, totalGoals, tvMessage;
    private View messageContainer;
    private MaterialButton btnRetry;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_jars, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JarsViewModel.class);
        SettingHelper.updateRatesIfNecessary(requireContext());

        totalSaved = view.findViewById(R.id.total_saved_amount);
        totalGoals = view.findViewById(R.id.total_goals_amount);
        messageContainer = view.findViewById(R.id.jarsMessageContainer);
        tvMessage = view.findViewById(R.id.tvJarsMessage);
        btnRetry = view.findViewById(R.id.btnJarsRetry);
        btnRetry.setOnClickListener(v -> viewModel.retry());

        adapter = new JarAdapter();
        recyclerView = view.findViewById(R.id.jars_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        swipeRefresh = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefresh.setOnRefreshListener(viewModel::retry);
        if (SettingHelper.isDarkMode(requireContext())) {
            swipeRefresh.setProgressBackgroundColorSchemeColor(Color.parseColor("#424242"));
            swipeRefresh.setColorSchemeColors(Color.WHITE);
        } else {
            swipeRefresh.setProgressBackgroundColorSchemeColor(Color.WHITE);
            swipeRefresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.blue));
        }

        FloatingActionButton fab = view.findViewById(R.id.add_jar_fab);
        fab.setOnClickListener(v ->
                new CreateJarDialogFragment().show(getParentFragmentManager(), "CreateJarDialog"));
        getParentFragmentManager().setFragmentResultListener(
                CreateJarDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (key, bundle) -> onJarFormResult(bundle));

        setupChips(view);
        setupSwipeToDelete();

        viewModel.state().observe(getViewLifecycleOwner(), this::render);
        viewModel.messages().observe(getViewLifecycleOwner(), event -> {
            String text = event.consume();
            if (text != null) Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
        });
    }

    private void render(JarsUiState state) {
        adapter.submitList(state.visibleJars);
        swipeRefresh.setRefreshing(state.loading);
        totalSaved.setText(SettingHelper.formatAmount(requireContext(), state.totalSaved.toPesos()));
        totalGoals.setText(SettingHelper.formatAmount(requireContext(), state.totalGoal.toPesos()));

        if (state.error != null) {
            tvMessage.setText(state.error);
            btnRetry.setVisibility(View.VISIBLE);
            messageContainer.setVisibility(View.VISIBLE);
        } else if (state.isEmpty()) {
            tvMessage.setText(state.filteredOut ? R.string.jars_empty_filtered : R.string.jars_empty);
            btnRetry.setVisibility(View.GONE);
            messageContainer.setVisibility(View.VISIBLE);
        } else {
            messageContainer.setVisibility(View.GONE);
        }
    }

    private void onJarFormResult(Bundle bundle) {
        String name = bundle.getString(CreateJarDialogFragment.KEY_JAR_NAME);
        String target = bundle.getString(CreateJarDialogFragment.KEY_TARGET_AMOUNT);

        // The form takes the goal in the user's display currency; jars are stored in base PHP
        Money typed = Money.parse(target);
        Money targetInBase = typed.isPositive()
                ? Money.ofPesos(SettingHelper.convertToBasePHP(requireContext(), typed.toPesos()))
                : null;

        String error = viewModel.create(name, targetInBase,
                bundle.getString(CreateJarDialogFragment.KEY_JAR_TYPE),
                bundle.getString(CreateJarDialogFragment.KEY_SELECTED_ICON),
                bundle.getInt(CreateJarDialogFragment.KEY_SELECTED_COLOR),
                bundle.getString(CreateJarDialogFragment.KEY_DEADLINE));
        if (error != null) Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
    }

    private void setupChips(View view) {
        Chip chipAll = view.findViewById(R.id.chip_all);
        if (chipAll != null) chipAll.setChecked(true);

        ChipGroup chipGroup = view.findViewById(R.id.chip_group);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setFilter(JarsViewModel.ALL);
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            viewModel.setFilter(chip != null ? chip.getText().toString() : JarsViewModel.ALL);
        });
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                // Snap back; the live list removes the row if the delete goes through
                adapter.notifyItemChanged(position);
                JarItem jar = adapter.getJarAt(position);
                if (!viewModel.canDelete(jar)) {
                    Toast.makeText(requireContext(), "Only the creator can delete this jar", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Jar")
                        .setMessage("Are you sure you want to delete '" + jar.getName() + "'? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> viewModel.delete(jar))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }).attachToRecyclerView(recyclerView);
    }
}
