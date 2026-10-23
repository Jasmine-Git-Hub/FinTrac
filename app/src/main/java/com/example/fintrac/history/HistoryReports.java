package com.example.fintrac.history;

import android.content.Context;

import com.example.fintrac.NumberFormatUtil;
import com.example.fintrac.ReportHelper;
import com.example.fintrac.Transaction;
import com.example.fintrac.core.Money;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds and exports the History reports. Totals use exact centavo math. */
public final class HistoryReports {

    public enum Format { CSV, PDF }

    private HistoryReports() {}

    /** The "Generate report" button: Excel summary with weekly/monthly/yearly periods. */
    public static void exportExcelSummary(Context context, List<Transaction> all) {
        ReportHelper.ReportData data = baseReport(all, "Financial Summary Report",
                "Generated on: " + new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date()));

        Date now = new Date();
        data.periodSummaries = new ArrayList<>();
        data.periodSummaries.add(period(all, "Weekly (Last 7 Days)", HistoryEntries.startOfDaysAgo(7), now));
        data.periodSummaries.add(period(all, "Monthly (Last 30 Days)", HistoryEntries.startOfDaysAgo(30), now));
        data.periodSummaries.add(period(all, "Yearly (Last 365 Days)", HistoryEntries.startOfDaysAgo(365), now));
        data.periodSummaries.add(period(all, "Current Month ("
                + new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now) + ")", HistoryEntries.startOfMonth(), now));
        data.periodSummaries.add(period(all, "Current Year (" + Calendar.getInstance().get(Calendar.YEAR) + ")",
                HistoryEntries.startOfYear(), now));

        data.periodSummariesFormatted = new ArrayList<>();
        for (ReportHelper.PeriodSummary p : data.periodSummaries) {
            ReportHelper.PeriodSummaryFormatted f = new ReportHelper.PeriodSummaryFormatted();
            f.periodLabel = p.periodLabel;
            f.incomeFormatted = NumberFormatUtil.formatCompact(context, p.income);
            f.expenseFormatted = NumberFormatUtil.formatCompact(context, p.expense);
            f.netCashFlowFormatted = NumberFormatUtil.formatCompact(context, p.netCashFlow);
            f.incomeRaw = p.income;
            f.expenseRaw = p.expense;
            f.netCashFlowRaw = p.netCashFlow;
            data.periodSummariesFormatted.add(f);
        }

        ReportHelper.generateExcelReport(context, data, "FinTrac_Financial_Summary_" + stamp() + ".xls");
    }

    /** Custom-range CSV/PDF report. */
    public static void exportRange(Context context, List<Transaction> all, String title,
                                   Date from, Date to, String rangeLabel, Format format) {
        List<Transaction> inRange = new ArrayList<>();
        for (Transaction t : all) {
            Date d = HistoryEntries.dateOf(t);
            if (!d.before(from) && !d.after(to)) inRange.add(t);
        }
        ReportHelper.ReportData data = baseReport(inRange, title, rangeLabel);
        String name = "FinTrac_Report_" + stamp() + (format == Format.CSV ? ".csv" : ".pdf");
        if (format == Format.CSV) {
            ReportHelper.generateCSVReport(context, data, name);
        } else {
            ReportHelper.generatePDFReport(context, data, name);
        }
    }

    private static ReportHelper.ReportData baseReport(List<Transaction> items, String title, String range) {
        ReportHelper.ReportData data = new ReportHelper.ReportData();
        data.title = title;
        data.dateRange = range;
        data.transactions = new ArrayList<>(items);

        Money[] totals = HistoryEntries.totalsBetween(items, new Date(Long.MIN_VALUE), new Date(Long.MAX_VALUE));
        data.totalIncome = data.totalIncomeRaw = totals[0].toPesos();
        data.totalExpense = data.totalExpenseRaw = totals[1].toPesos();
        data.netCashFlow = data.netCashFlowRaw = totals[0].minus(totals[1]).toPesos();

        Map<String, Money> amounts = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Transaction t : items) {
            String category = t.getCategory() != null ? t.getCategory() : "Uncategorized";
            amounts.merge(category, Money.ofPesos(t.getAmount()), Money::plus);
            counts.merge(category, 1, Integer::sum);
        }
        data.categorySummaries = new ArrayList<>();
        for (Map.Entry<String, Money> e : amounts.entrySet()) {
            ReportHelper.CategorySummary s = new ReportHelper.CategorySummary();
            s.category = e.getKey();
            s.amount = e.getValue().toPesos();
            s.count = counts.get(e.getKey());
            data.categorySummaries.add(s);
        }
        return data;
    }

    private static ReportHelper.PeriodSummary period(List<Transaction> all, String label, Date from, Date to) {
        Money[] totals = HistoryEntries.totalsBetween(all, from, to);
        ReportHelper.PeriodSummary s = new ReportHelper.PeriodSummary();
        s.periodLabel = label;
        s.income = totals[0].toPesos();
        s.expense = totals[1].toPesos();
        s.netCashFlow = totals[0].minus(totals[1]).toPesos();
        return s;
    }

    private static String stamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }
}
