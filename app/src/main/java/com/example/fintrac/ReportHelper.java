package com.example.fintrac;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportHelper {

    public static class ReportData {
        public String title;
        public String dateRange;
        public double totalIncome;
        public double totalExpense;
        public double netCashFlow;
        public double totalIncomeRaw;
        public double totalExpenseRaw;
        public double netCashFlowRaw;
        public List<PeriodSummary> periodSummaries;
        public List<PeriodSummaryFormatted> periodSummariesFormatted;
        public List<CategorySummary> categorySummaries;
        public List<Transaction> transactions;
    }

    public static class CategorySummary {
        public String category;
        public double amount;
        public int count;
    }

    public static class PeriodSummary {
        public String periodLabel;
        public double income;
        public double expense;
        public double netCashFlow;
    }

    public static class PeriodSummaryFormatted {
        public String periodLabel;
        public String incomeFormatted;
        public String expenseFormatted;
        public String netCashFlowFormatted;
        public double incomeRaw;
        public double expenseRaw;
        public double netCashFlowRaw;
    }

    public static void generateCSVReport(Context context, ReportData data, String filename) {
        try {
            // Get current currency AT THE MOMENT OF GENERATION
            String currencyCode = CurrencyHelper.getCurrencyCode(context);
            String currencySymbol = CurrencyHelper.getCurrencySymbol(context);

            File file = reportFile(context, filename);
            FileWriter writer = new FileWriter(file);

            // Write header
            writer.append("Report Title:,").append(data.title).append("\n");
            writer.append("Date Range:,").append(data.dateRange).append("\n");
            writer.append("Generated:,").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
            writer.append("Currency:,").append(currencyCode).append(" (").append(currencySymbol).append(")\n");
            writer.append("\n");

            writer.append("SUMMARY\n");
            writer.append("Total Income (").append(currencyCode).append("),,").append(NumberFormatUtil.formatExactNumber(data.totalIncomeRaw > 0 ? data.totalIncomeRaw : data.totalIncome)).append("\n");
            writer.append("Total Expense (").append(currencyCode).append("),,").append(NumberFormatUtil.formatExactNumber(data.totalExpenseRaw > 0 ? data.totalExpenseRaw : data.totalExpense)).append("\n");
            writer.append("Net Cash Flow (").append(currencyCode).append("),,").append(NumberFormatUtil.formatExactNumber(data.netCashFlowRaw != 0 ? data.netCashFlowRaw : data.netCashFlow)).append("\n");
            writer.append("\n");

            if (data.periodSummaries != null && !data.periodSummaries.isEmpty()) {
                writer.append("PERIOD SUMMARY\n");
                writer.append("Period,Income (").append(currencyCode).append("),Expense (").append(currencyCode).append("),Net Cash Flow (").append(currencyCode).append(")\n");
                for (PeriodSummary summary : data.periodSummaries) {
                    writer.append(escapeCSV(summary.periodLabel)).append(",")
                            .append(NumberFormatUtil.formatExactNumber(summary.income)).append(",")
                            .append(NumberFormatUtil.formatExactNumber(summary.expense)).append(",")
                            .append(NumberFormatUtil.formatExactNumber(summary.netCashFlow)).append("\n");
                }
                writer.append("\n");
            }

            if (data.categorySummaries != null && !data.categorySummaries.isEmpty()) {
                writer.append("CATEGORY BREAKDOWN\n");
                writer.append("Category,Amount (").append(currencyCode).append("),Transaction Count\n");
                for (CategorySummary summary : data.categorySummaries) {
                    writer.append(escapeCSV(summary.category)).append(",")
                            .append(NumberFormatUtil.formatExactNumber(summary.amount)).append(",")
                            .append(String.valueOf(summary.count)).append("\n");
                }
                writer.append("\n");
            }

            if (data.transactions != null && !data.transactions.isEmpty()) {
                writer.append("TRANSACTION DETAILS\n");
                writer.append("Date,Title,Category,Amount (").append(currencyCode).append("),Type\n");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                for (Transaction t : data.transactions) {
                    String date = t.getDate() != null ? sdf.format(t.getDate()) : "N/A";
                    String type = t.isExpense() ? "Expense" : "Income";
                    writer.append(date).append(",")
                            .append(escapeCSV(t.getTitle())).append(",")
                            .append(escapeCSV(t.getCategory())).append(",")
                            .append(NumberFormatUtil.formatExactNumber(t.getAmount())).append(",")
                            .append(type).append("\n");
                }
            }

            writer.flush();
            writer.close();
            shareFile(context, file, "text/csv");
            Toast.makeText(context, "Report ready: " + filename, Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void generatePDFReport(Context context, ReportData data, String filename) {
        try {
            // Get current currency AT THE MOMENT OF GENERATION
            String currencyCode = CurrencyHelper.getCurrencyCode(context);

            File file = reportFile(context, filename);
            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            Paragraph titlePara = new Paragraph(data.title);
            titlePara.setTextAlignment(TextAlignment.CENTER);
            titlePara.setFontSize(18);
            titlePara.setBold();
            document.add(titlePara);

            Paragraph datePara = new Paragraph("Date Range: " + data.dateRange);
            datePara.setTextAlignment(TextAlignment.CENTER);
            datePara.setFontSize(12);
            document.add(datePara);

            Paragraph generatedPara = new Paragraph("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            generatedPara.setTextAlignment(TextAlignment.CENTER);
            generatedPara.setFontSize(10);
            document.add(generatedPara);

            Paragraph currencyPara = new Paragraph("Currency: " + currencyCode);
            currencyPara.setTextAlignment(TextAlignment.CENTER);
            currencyPara.setFontSize(10);
            document.add(currencyPara);

            document.add(new Paragraph("\n"));

            Paragraph summaryTitle = new Paragraph("SUMMARY");
            summaryTitle.setFontSize(14);
            summaryTitle.setBold();
            document.add(summaryTitle);

            float[] columnWidths = {2, 3};
            Table summaryTable = new Table(UnitValue.createPercentArray(columnWidths));
            summaryTable.setWidth(UnitValue.createPercentValue(100));

            Cell incomeLabelCell = new Cell();
            incomeLabelCell.add(new Paragraph("Total Income:"));
            summaryTable.addCell(incomeLabelCell);

            Cell incomeValueCell = new Cell();
            double actualIncome = data.totalIncomeRaw > 0 ? data.totalIncomeRaw : data.totalIncome;
            incomeValueCell.add(new Paragraph(CurrencyHelper.formatAmount(context, actualIncome)));
            summaryTable.addCell(incomeValueCell);

            Cell expenseLabelCell = new Cell();
            expenseLabelCell.add(new Paragraph("Total Expense:"));
            summaryTable.addCell(expenseLabelCell);

            Cell expenseValueCell = new Cell();
            double actualExpense = data.totalExpenseRaw > 0 ? data.totalExpenseRaw : data.totalExpense;
            expenseValueCell.add(new Paragraph(CurrencyHelper.formatAmount(context, actualExpense)));
            summaryTable.addCell(expenseValueCell);

            Cell netLabelCell = new Cell();
            netLabelCell.add(new Paragraph("Net Cash Flow:"));
            summaryTable.addCell(netLabelCell);

            Cell netValueCell = new Cell();
            double actualNet = data.netCashFlowRaw != 0 ? data.netCashFlowRaw : data.netCashFlow;
            String netColor = actualNet >= 0 ? "Positive" : "Negative";
            netValueCell.add(new Paragraph(CurrencyHelper.formatAmount(context, actualNet) + " (" + netColor + ")"));
            summaryTable.addCell(netValueCell);

            document.add(summaryTable);
            document.add(new Paragraph("\n"));

            if (data.periodSummaries != null && !data.periodSummaries.isEmpty()) {
                Paragraph periodTitle = new Paragraph("PERIOD SUMMARY");
                periodTitle.setFontSize(14);
                periodTitle.setBold();
                document.add(periodTitle);

                float[] periodWidths = {3, 2, 2, 2};
                Table periodTable = new Table(UnitValue.createPercentArray(periodWidths));
                periodTable.setWidth(UnitValue.createPercentValue(100));

                Cell periodHeader1 = new Cell();
                periodHeader1.add(new Paragraph("Period"));
                periodTable.addHeaderCell(periodHeader1);

                Cell periodHeader2 = new Cell();
                periodHeader2.add(new Paragraph("Income"));
                periodTable.addHeaderCell(periodHeader2);

                Cell periodHeader3 = new Cell();
                periodHeader3.add(new Paragraph("Expense"));
                periodTable.addHeaderCell(periodHeader3);

                Cell periodHeader4 = new Cell();
                periodHeader4.add(new Paragraph("Net Cash Flow"));
                periodTable.addHeaderCell(periodHeader4);

                for (PeriodSummary summary : data.periodSummaries) {
                    Cell periodCell1 = new Cell();
                    periodCell1.add(new Paragraph(summary.periodLabel));
                    periodTable.addCell(periodCell1);

                    Cell periodCell2 = new Cell();
                    periodCell2.add(new Paragraph(CurrencyHelper.formatAmount(context, summary.income)));
                    periodTable.addCell(periodCell2);

                    Cell periodCell3 = new Cell();
                    periodCell3.add(new Paragraph(CurrencyHelper.formatAmount(context, summary.expense)));
                    periodTable.addCell(periodCell3);

                    Cell periodCell4 = new Cell();
                    periodCell4.add(new Paragraph(CurrencyHelper.formatAmount(context, summary.netCashFlow)));
                    periodTable.addCell(periodCell4);
                }

                document.add(periodTable);
                document.add(new Paragraph("\n"));
            }

            if (data.categorySummaries != null && !data.categorySummaries.isEmpty()) {
                Paragraph categoryTitle = new Paragraph("CATEGORY BREAKDOWN");
                categoryTitle.setFontSize(14);
                categoryTitle.setBold();
                document.add(categoryTitle);

                float[] catWidths = {3, 2, 2};
                Table catTable = new Table(UnitValue.createPercentArray(catWidths));
                catTable.setWidth(UnitValue.createPercentValue(100));

                Cell catHeader1 = new Cell();
                catHeader1.add(new Paragraph("Category"));
                catTable.addHeaderCell(catHeader1);

                Cell catHeader2 = new Cell();
                catHeader2.add(new Paragraph("Amount"));
                catTable.addHeaderCell(catHeader2);

                Cell catHeader3 = new Cell();
                catHeader3.add(new Paragraph("Count"));
                catTable.addHeaderCell(catHeader3);

                for (CategorySummary summary : data.categorySummaries) {
                    Cell catCell1 = new Cell();
                    catCell1.add(new Paragraph(summary.category));
                    catTable.addCell(catCell1);

                    Cell catCell2 = new Cell();
                    catCell2.add(new Paragraph(CurrencyHelper.formatAmount(context, summary.amount)));
                    catTable.addCell(catCell2);

                    Cell catCell3 = new Cell();
                    catCell3.add(new Paragraph(String.valueOf(summary.count)));
                    catTable.addCell(catCell3);
                }

                document.add(catTable);
                document.add(new Paragraph("\n"));
            }

            if (data.transactions != null && !data.transactions.isEmpty()) {
                Paragraph txTitle = new Paragraph("TRANSACTION DETAILS");
                txTitle.setFontSize(14);
                txTitle.setBold();
                document.add(txTitle);

                float[] txWidths = {2, 3, 2, 2, 2};
                Table txTable = new Table(UnitValue.createPercentArray(txWidths));
                txTable.setWidth(UnitValue.createPercentValue(100));

                Cell txHeader1 = new Cell();
                txHeader1.add(new Paragraph("Date"));
                txTable.addHeaderCell(txHeader1);

                Cell txHeader2 = new Cell();
                txHeader2.add(new Paragraph("Title"));
                txTable.addHeaderCell(txHeader2);

                Cell txHeader3 = new Cell();
                txHeader3.add(new Paragraph("Category"));
                txTable.addHeaderCell(txHeader3);

                Cell txHeader4 = new Cell();
                txHeader4.add(new Paragraph("Amount"));
                txTable.addHeaderCell(txHeader4);

                Cell txHeader5 = new Cell();
                txHeader5.add(new Paragraph("Type"));
                txTable.addHeaderCell(txHeader5);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                for (Transaction t : data.transactions) {
                    String date = t.getDate() != null ? sdf.format(t.getDate()) : "N/A";
                    String type = t.isExpense() ? "Expense" : "Income";

                    Cell dateCell = new Cell();
                    dateCell.add(new Paragraph(date));
                    txTable.addCell(dateCell);

                    Cell titleCell = new Cell();
                    titleCell.add(new Paragraph(t.getTitle() != null ? t.getTitle() : "N/A"));
                    txTable.addCell(titleCell);

                    Cell categoryCell = new Cell();
                    categoryCell.add(new Paragraph(t.getCategory() != null ? t.getCategory() : "Uncategorized"));
                    txTable.addCell(categoryCell);

                    Cell amountCell = new Cell();
                    amountCell.add(new Paragraph(CurrencyHelper.formatAmount(context, t.getAmount())));
                    txTable.addCell(amountCell);

                    Cell typeCell = new Cell();
                    typeCell.add(new Paragraph(type));
                    txTable.addCell(typeCell);
                }

                document.add(txTable);
            }

            document.close();
            shareFile(context, file, "application/pdf");
            Toast.makeText(context, "Report ready: " + filename, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void generateExcelReport(Context context, ReportData data, String filename) {
        try {
            // Get current currency AT THE MOMENT OF GENERATION
            String currencyCode = CurrencyHelper.getCurrencyCode(context);
            String currencySymbol = CurrencyHelper.getCurrencySymbol(context);

            File file = reportFile(context, filename);
            FileWriter writer = new FileWriter(file);

            writer.append("<?xml version=\"1.0\"?>\n");
            writer.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" ")
                    .append("xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n");

            writer.append("<Worksheet ss:Name=\"Summary\"><Table>\n");
            appendExcelRow(writer, "Report Title", data.title);
            appendExcelRow(writer, "Date Range", data.dateRange);
            appendExcelRow(writer, "Generated", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            appendExcelRow(writer, "Currency", currencyCode + " (" + currencySymbol + ")");
            writer.append("<Row/>\n");

            appendExcelRow(writer, "Period", "Income (" + currencyCode + ")", "Expense (" + currencyCode + ")", "Net Cash Flow (" + currencyCode + ")");

            if (data.periodSummaries != null) {
                for (PeriodSummary summary : data.periodSummaries) {
                    appendExcelRow(writer,
                            summary.periodLabel,
                            NumberFormatUtil.formatExactNumber(summary.income),
                            NumberFormatUtil.formatExactNumber(summary.expense),
                            NumberFormatUtil.formatExactNumber(summary.netCashFlow));
                }
            }

            writer.append("<Row/>\n");
            double actualIncome = data.totalIncomeRaw > 0 ? data.totalIncomeRaw : data.totalIncome;
            double actualExpense = data.totalExpenseRaw > 0 ? data.totalExpenseRaw : data.totalExpense;
            double actualNet = data.netCashFlowRaw != 0 ? data.netCashFlowRaw : data.netCashFlow;
            appendExcelRow(writer, "TOTAL",
                    NumberFormatUtil.formatExactNumber(actualIncome),
                    NumberFormatUtil.formatExactNumber(actualExpense),
                    NumberFormatUtil.formatExactNumber(actualNet));

            writer.append("</Table></Worksheet>\n");

            if (data.categorySummaries != null && !data.categorySummaries.isEmpty()) {
                writer.append("<Worksheet ss:Name=\"Category Breakdown\"><Table>\n");
                appendExcelRow(writer, "Category", "Amount (" + currencyCode + ")", "Transaction Count");
                for (CategorySummary summary : data.categorySummaries) {
                    appendExcelRow(writer,
                            summary.category,
                            NumberFormatUtil.formatExactNumber(summary.amount),
                            String.valueOf(summary.count));
                }
                writer.append("</Table></Worksheet>\n");
            }

            writer.append("<Worksheet ss:Name=\"Transactions\"><Table>\n");
            appendExcelRow(writer, "Date", "Title", "Category", "Amount (" + currencyCode + ")", "Type");
            if (data.transactions != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                for (Transaction transaction : data.transactions) {
                    String date = transaction.getDate() != null ? sdf.format(transaction.getDate()) : "N/A";
                    appendExcelRow(writer,
                            date,
                            transaction.getTitle() != null ? transaction.getTitle() : "",
                            transaction.getCategory() != null ? transaction.getCategory() : "Uncategorized",
                            NumberFormatUtil.formatExactNumber(transaction.getAmount()),
                            transaction.isExpense() ? "Expense" : "Income");
                }
            }
            writer.append("</Table></Worksheet>\n");

            writer.append("</Workbook>");

            writer.flush();
            writer.close();

            shareFile(context, file, "application/vnd.ms-excel");
            Toast.makeText(context, "Excel report ready: " + filename, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error generating Excel report: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

    private static void appendExcelRow(FileWriter writer, String... values) throws IOException {
        writer.append("<Row>");
        for (String value : values) {
            writer.append("<Cell><Data ss:Type=\"String\">")
                    .append(escapeXml(value))
                    .append("</Data></Cell>");
        }
        writer.append("</Row>\n");
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // Reports go to app-private cache (not public Downloads); the user decides where to save
    // or send them from the share sheet. Old reports are cleared so financial data doesn't pile up.
    private static File reportFile(Context context, String filename) throws IOException {
        File dir = new File(context.getCacheDir(), "reports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create reports directory");
        }
        File[] old = dir.listFiles();
        if (old != null) {
            for (File f : old) {
                f.delete();
            }
        }
        return new File(dir, new File(filename).getName());
    }

    private static void shareFile(Context context, File file, String type) {
        try {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(type);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(shareIntent, "Share Report"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error sharing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}