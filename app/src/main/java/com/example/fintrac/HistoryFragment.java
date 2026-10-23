package com.example.fintrac;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fintrac.history.HistoryEntries;
import com.example.fintrac.history.HistoryReports;
import com.example.fintrac.history.HistoryViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * History screen. Lists personal transactions, paid bills and jar activity (merged and
 * de-duplicated by {@link HistoryEntries}) and exports the Excel summary.
 */
public class HistoryFragment extends Fragment {

    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;

    private TextView tvCount, tvWeek, tvMonth, tvYear, tvMessage;
    private View progress, messageContainer;
    private MaterialButton btnRetry;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        tvCount = view.findViewById(R.id.tvTransactionCount);
        tvWeek = view.findViewById(R.id.tvWeeklyExpense);
        tvMonth = view.findViewById(R.id.tvMonthlyExpense);
        tvYear = view.findViewById(R.id.tvYearlyExpense);
        progress = view.findViewById(R.id.progressHistory);
        messageContainer = view.findViewById(R.id.historyMessageContainer);
        tvMessage = view.findViewById(R.id.tvHistoryMessage);
        btnRetry = view.findViewById(R.id.btnHistoryRetry);
        btnRetry.setOnClickListener(v -> viewModel.refresh());

        RecyclerView list = view.findViewById(R.id.rvHistory);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(new java.util.ArrayList<>(), requireContext());
        list.setAdapter(adapter);

        MaterialButton btnReport = view.findViewById(R.id.btnGenerateReport);
        btnReport.setContentDescription("Generate report");
        btnReport.setOnClickListener(v -> {
            HistoryViewModel.State state = viewModel.state().getValue();
            if (state == null || state.all.isEmpty()) {
                Toast.makeText(requireContext(), "Nothing to report yet", Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Generate Report")
                    .setItems(new String[]{"Quick summary (Excel)", "Custom report (CSV or PDF)"}, (d, which) -> {
                        if (which == 0) {
                            HistoryReports.exportExcelSummary(requireContext(), state.all);
                        } else {
                            showCustomReportDialog(state.all);
                        }
                    })
                    .show();
        });

        ChipGroup chips = view.findViewById(R.id.chipGroupFilter);
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            HistoryEntries.Filter filter = HistoryEntries.Filter.ALL;
            if (id == R.id.chipIncome) filter = HistoryEntries.Filter.INCOME;
            else if (id == R.id.chipExpense) filter = HistoryEntries.Filter.EXPENSE;
            else if (id == R.id.chipBills) filter = HistoryEntries.Filter.BILLS;
            else if (id == R.id.chipJars) filter = HistoryEntries.Filter.JARS;
            viewModel.setFilter(filter);
        });

        viewModel.state().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Other screens add transactions; pick them up when returning here
        viewModel.refresh();
    }

    /** Report type, date range (incl. custom) and CSV/PDF format. */
    private void showCustomReportDialog(List<Transaction> all) {
        // This layout uses Material 3 widget styles; the app theme is MaterialComponents, so inflate
        // it with a Material 3 theme (as Bills does for its dialog) or the chips crash on creation.
        android.view.ContextThemeWrapper themed = new android.view.ContextThemeWrapper(requireContext(),
                com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar);
        View view = LayoutInflater.from(themed).inflate(R.layout.dialog_report_options, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();

        ChipGroup typeChips = view.findViewById(R.id.chipGroupReportType);
        ChipGroup rangeChips = view.findViewById(R.id.chipGroupDateRange);
        ChipGroup formatChips = view.findViewById(R.id.chipGroupFormat);
        View customRange = view.findViewById(R.id.layoutCustomDate);
        MaterialButton btnStart = view.findViewById(R.id.btnStartDate);
        MaterialButton btnEnd = view.findViewById(R.id.btnEndDate);

        // Custom range defaults to the last month, ending today
        Calendar start = Calendar.getInstance();
        start.add(Calendar.MONTH, -1);
        Calendar end = Calendar.getInstance();
        SimpleDateFormat label = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        btnStart.setText(label.format(start.getTime()));
        btnEnd.setText(label.format(end.getTime()));

        rangeChips.setOnCheckedStateChangeListener((group, ids) ->
                customRange.setVisibility(!ids.isEmpty() && ids.get(0) == R.id.chipCustom ? View.VISIBLE : View.GONE));
        btnStart.setOnClickListener(v -> pickDate(start, btnStart, label));
        btnEnd.setOnClickListener(v -> pickDate(end, btnEnd, label));

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btnGenerate).setOnClickListener(v -> {
            int type = typeChips.getCheckedChipId();
            String title = type == R.id.chipCashFlow ? "Cash Flow Report"
                    : type == R.id.chipCategory ? "Category Breakdown Report" : "Income vs Expense Report";

            Calendar from = Calendar.getInstance();
            Calendar to = Calendar.getInstance();
            String rangeText;
            int range = rangeChips.getCheckedChipId();
            if (range == R.id.chipWeekly) {
                from.add(Calendar.DAY_OF_YEAR, -7);
                rangeText = "Last 7 Days";
            } else if (range == R.id.chipYearly) {
                from.add(Calendar.YEAR, -1);
                rangeText = "Last 12 Months";
            } else if (range == R.id.chipCustom) {
                from.setTime(start.getTime());
                to.setTime(end.getTime());
                if (from.after(to)) {
                    Toast.makeText(requireContext(), "Start date must be before end date", Toast.LENGTH_SHORT).show();
                    return;
                }
                rangeText = label.format(from.getTime()) + " to " + label.format(to.getTime());
            } else {
                from.add(Calendar.MONTH, -1);
                rangeText = "Last 30 Days";
            }
            // Include the whole first and last day
            from.set(Calendar.HOUR_OF_DAY, 0);
            from.set(Calendar.MINUTE, 0);
            from.set(Calendar.SECOND, 0);
            to.set(Calendar.HOUR_OF_DAY, 23);
            to.set(Calendar.MINUTE, 59);
            to.set(Calendar.SECOND, 59);

            HistoryReports.Format format = formatChips.getCheckedChipId() == R.id.chipPDF
                    ? HistoryReports.Format.PDF : HistoryReports.Format.CSV;
            dialog.dismiss();
            HistoryReports.exportRange(requireContext(), all, title, from.getTime(), to.getTime(), rangeText, format);
        });
        dialog.show();
    }

    private void pickDate(Calendar target, MaterialButton button, SimpleDateFormat label) {
        new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
            target.set(year, month, day);
            button.setText(label.format(target.getTime()));
        }, target.get(Calendar.YEAR), target.get(Calendar.MONTH), target.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void render(HistoryViewModel.State state) {
        adapter.setTransactions(state.visible);
        int n = state.visible.size();
        tvCount.setText(state.loading ? "Loading..." : n == 1 ? "1 transaction" : n + " transactions");

        tvWeek.setText(NumberFormatUtil.formatCompact(requireContext(), state.weekNet.toPesos()));
        tvMonth.setText(NumberFormatUtil.formatCompact(requireContext(), state.monthNet.toPesos()));
        tvYear.setText(NumberFormatUtil.formatCompact(requireContext(), state.yearNet.toPesos()));

        progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
        if (state.error != null) {
            tvMessage.setText(state.error);
            btnRetry.setVisibility(View.VISIBLE);
            messageContainer.setVisibility(View.VISIBLE);
        } else if (state.isEmpty()) {
            tvMessage.setText(state.all.isEmpty() ? R.string.history_empty : R.string.history_empty_filtered);
            btnRetry.setVisibility(View.GONE);
            messageContainer.setVisibility(View.VISIBLE);
        } else {
            messageContainer.setVisibility(View.GONE);
        }
    }
}
