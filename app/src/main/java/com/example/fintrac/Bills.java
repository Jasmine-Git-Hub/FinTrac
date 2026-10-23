package com.example.fintrac;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fintrac.bills.BillsUiState;
import com.example.fintrac.bills.BillsViewModel;
import com.example.fintrac.core.Money;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

/**
 * Bills screen. Draws {@link BillsUiState} from {@link BillsViewModel} and forwards user actions;
 * all data access and business rules live in the ViewModel and repository.
 */
public class Bills extends Fragment implements BillsAdapter.BillActionListener {

    private static final String[] CATEGORIES =
            {"Utilities", "Subscriptions", "Loans", "Rent", "Groceries", "Transportation", "Others"};

    private BillsViewModel viewModel;
    private BillsAdapter adapter;

    private RecyclerView recyclerView;
    private TextView tvTotalDue, tvTotalPaid, tvMessage;
    private View progress, messageContainer;
    private MaterialButton btnRetry;
    private MenuItem togglePaidItem;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bills, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BillsViewModel.class);

        tvTotalDue = view.findViewById(R.id.tvTotalDue);
        tvTotalPaid = view.findViewById(R.id.tvTotalPaid);
        progress = view.findViewById(R.id.progressBills);
        messageContainer = view.findViewById(R.id.billsMessageContainer);
        tvMessage = view.findViewById(R.id.tvBillsMessage);
        btnRetry = view.findViewById(R.id.btnBillsRetry);
        btnRetry.setOnClickListener(v -> viewModel.retry());

        adapter = new BillsAdapter(this);
        recyclerView = view.findViewById(R.id.rvBillsList);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        setupCategoryChips(view);
        setupSwipeActions();
        setupMenu();

        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddBill);
        fabAdd.setOnClickListener(v -> showAddOrEditBillDialog(null));

        viewModel.state().observe(getViewLifecycleOwner(), this::render);
        viewModel.messages().observe(getViewLifecycleOwner(), event -> {
            String text = event.consume();
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
            }
        });

        scheduleBillReminders();
    }

    private void render(BillsUiState state) {
        adapter.submitList(state.visibleBills);
        tvTotalDue.setText(SettingHelper.formatAmount(requireContext(), state.totalDue.toPesos()));
        tvTotalPaid.setText(SettingHelper.formatAmount(requireContext(), state.totalPaid.toPesos()));

        progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);

        if (state.error != null) {
            tvMessage.setText(state.error);
            btnRetry.setVisibility(View.VISIBLE);
            messageContainer.setVisibility(View.VISIBLE);
        } else if (state.isEmpty()) {
            tvMessage.setText(state.totalDue.isPositive() || state.totalPaid.isPositive()
                    ? R.string.bills_empty_filtered : R.string.bills_empty);
            btnRetry.setVisibility(View.GONE);
            messageContainer.setVisibility(View.VISIBLE);
        } else {
            messageContainer.setVisibility(View.GONE);
        }

        if (togglePaidItem != null) {
            togglePaidItem.setTitle(state.showPaid ? R.string.bills_hide_paid : R.string.bills_show_paid);
        }
    }

    @Override
    public void onBillPaidToggled(Bill bill) {
        viewModel.togglePaid(bill);
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.bills_menu, menu);
                togglePaidItem = menu.findItem(R.id.action_toggle_paid);
                BillsUiState state = viewModel.state().getValue();
                if (togglePaidItem != null && state != null) {
                    togglePaidItem.setTitle(state.showPaid ? R.string.bills_hide_paid : R.string.bills_show_paid);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_toggle_paid) {
                    viewModel.toggleShowPaid();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void setupCategoryChips(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupCategory);
        Chip chipAll = view.findViewById(R.id.chipAll);
        chipAll.setChecked(true);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setCategory(BillsViewModel.ALL);
                return;
            }
            Chip chip = group.findViewById(checkedIds.get(0));
            viewModel.setCategory(chip != null ? chip.getText().toString() : BillsViewModel.ALL);
        });
    }

    /** Swipe left to archive, right to edit. */
    private void setupSwipeActions() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                // Snap the row back; the list updates by itself if the action goes through
                adapter.notifyItemChanged(position);
                Bill bill = adapter.getBillAt(position);
                if (direction == ItemTouchHelper.LEFT) {
                    confirmArchive(bill);
                } else {
                    showAddOrEditBillDialog(bill);
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void confirmArchive(Bill bill) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Archive Bill")
                .setMessage("Are you sure you want to archive '" + bill.name + "'? You can find it in Archives.")
                .setPositiveButton("Archive", (dialog, which) -> viewModel.archive(bill))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddOrEditBillDialog(@Nullable Bill existing) {
        Context context = requireContext();
        ContextThemeWrapper themed = new ContextThemeWrapper(context,
                com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar);
        View dialogView = LayoutInflater.from(themed).inflate(R.layout.dialog_add_bill, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextInputEditText etName = dialogView.findViewById(R.id.etBillName);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDueDate = dialogView.findViewById(R.id.etDueDate);
        AutoCompleteTextView actCategory = dialogView.findViewById(R.id.actCategory);
        TextInputLayout tilFrequency = dialogView.findViewById(R.id.tilFrequency);
        AutoCompleteTextView actFrequency = dialogView.findViewById(R.id.actFrequency);
        MaterialCheckBox cbRecurring = dialogView.findViewById(R.id.cbRecurring);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnAddBill);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        cbRecurring.setOnCheckedChangeListener((button, checked) ->
                tilFrequency.setVisibility(checked ? View.VISIBLE : View.GONE));
        actFrequency.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.frequency_options)));
        actCategory.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, CATEGORIES));

        etDueDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(context,
                    (picker, year, month, day) -> etDueDate.setText((month + 1) + "/" + day + "/" + year),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        if (existing != null) {
            etName.setText(existing.name);
            // Stored in base PHP; show it in the user's currency
            double display = existing.money().toPesos()
                    * SettingHelper.getExchangeRate(context, SettingHelper.getCurrency(context));
            etAmount.setText(String.format(Locale.US, "%.2f", display));
            if (existing.dueDate != null) etDueDate.setText(existing.dueDate.replace("Due: ", ""));
            actCategory.setText(existing.category, false);
            if (existing.frequency != null && !existing.frequency.equalsIgnoreCase("One-time")) {
                cbRecurring.setChecked(true);
                actFrequency.setText(existing.frequency, false);
            }
            btnSave.setText(R.string.save_changes);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String category = text(actCategory);
            String frequency = "One-time";
            if (cbRecurring.isChecked()) {
                frequency = text(actFrequency).isEmpty() ? "Monthly" : text(actFrequency);
            }

            String error = viewModel.save(existing, text(etName), parseToBase(text(etAmount)),
                    text(etDueDate), category.isEmpty() ? "Utilities" : category, frequency);
            if (error != null) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    /** Converts what the user typed (in their display currency) to base PHP, or null if invalid. */
    @Nullable
    private Money parseToBase(String typed) {
        Money entered = Money.parse(typed);
        if (!entered.isPositive()) return null;
        return Money.ofPesos(SettingHelper.convertToBasePHP(requireContext(), entered.toPesos()));
    }

    private static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    /** Daily 8 AM check for due bills (see DeadlineReminderReceiver). Re-scheduling is idempotent. */
    private void scheduleBillReminders() {
        Context context = requireContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, DeadlineReminderReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent);
    }
}
