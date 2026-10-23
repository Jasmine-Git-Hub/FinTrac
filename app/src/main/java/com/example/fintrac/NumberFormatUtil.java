package com.example.fintrac;

import android.content.Context;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberFormatUtil {

    /**
     * Format a double number to a compact, readable format with selected currency
     * @param context The application context
     * @param value The number to format
     * @return Compact formatted string with currency symbol
     */
    public static String formatCompact(Context context, double value) {
        if (context == null) {
            return formatCompactDefault(value);
        }

        if (value == 0) {
            return CurrencyHelper.formatAmount(context, 0);
        }

        boolean isNegative = value < 0;
        double absoluteValue = Math.abs(value);

        String compactNumber;

        if (absoluteValue >= 1_000_000_000) {
            compactNumber = String.format(Locale.US, "%.1fB", absoluteValue / 1_000_000_000);
        } else if (absoluteValue >= 1_000_000) {
            compactNumber = String.format(Locale.US, "%.1fM", absoluteValue / 1_000_000);
        } else if (absoluteValue >= 1_000) {
            compactNumber = String.format(Locale.US, "%.1fK", absoluteValue / 1_000);
        } else {
            // For values less than 1000, use regular currency format
            return CurrencyHelper.formatAmount(context, value);
        }

        // Remove .0 from numbers like "100.0K" to make it "100K"
        if (compactNumber.endsWith(".0K")) {
            compactNumber = compactNumber.replace(".0K", "K");
        } else if (compactNumber.endsWith(".0M")) {
            compactNumber = compactNumber.replace(".0M", "M");
        } else if (compactNumber.endsWith(".0B")) {
            compactNumber = compactNumber.replace(".0B", "B");
        }

        // Get the currency symbol
        String currencySymbol = getCurrencySymbol(CurrencyHelper.getCurrencyCode(context));

        return (isNegative ? "-" : "") + currencySymbol + compactNumber;
    }

    private static String formatCompactDefault(double value) {
        if (value == 0) return "₱0";

        boolean isNegative = value < 0;
        double absoluteValue = Math.abs(value);

        String formattedValue;

        if (absoluteValue >= 1_000_000_000) {
            formattedValue = String.format(Locale.US, "%.1fB", absoluteValue / 1_000_000_000);
        } else if (absoluteValue >= 1_000_000) {
            formattedValue = String.format(Locale.US, "%.1fM", absoluteValue / 1_000_000);
        } else if (absoluteValue >= 1_000) {
            formattedValue = String.format(Locale.US, "%.1fK", absoluteValue / 1_000);
        } else {
            if (absoluteValue == Math.floor(absoluteValue)) {
                formattedValue = String.format(Locale.US, "%.0f", absoluteValue);
            } else {
                formattedValue = String.format(Locale.US, "%.2f", absoluteValue);
            }
        }

        if (formattedValue.endsWith(".0K")) {
            formattedValue = formattedValue.replace(".0K", "K");
        } else if (formattedValue.endsWith(".0M")) {
            formattedValue = formattedValue.replace(".0M", "M");
        } else if (formattedValue.endsWith(".0B")) {
            formattedValue = formattedValue.replace(".0B", "B");
        }

        return (isNegative ? "-₱" : "₱") + formattedValue;
    }

    /**
     * Format a double number to exact value with currency symbol (for PDF exports)
     * @param context The application context
     * @param value The number to format
     * @return Exact formatted string with currency symbol
     */
    public static String formatExactWithCurrency(Context context, double value) {
        if (context == null) {
            return String.format(Locale.US, "%.2f", value);
        }
        // This will use the selected currency from settings
        return CurrencyHelper.formatAmount(context, value);
    }

    /**
     * Format a double number to exact value without currency symbol (for CSV/Excel)
     * @param value The number to format
     * @return Exact number string
     */
    public static String formatExactNumber(double value) {
        if (value == Math.floor(value)) {
            return String.format(Locale.US, "%.0f", value);
        } else {
            return String.format(Locale.US, "%.2f", value);
        }
    }

    /**
     * Get currency symbol from currency code
     * @param currencyCode The currency code (PHP, USD, EUR, etc.)
     * @return Currency symbol
     */
    private static String getCurrencySymbol(String currencyCode) {
        switch (currencyCode) {
            case "USD": return "$";
            case "EUR": return "€";
            case "GBP": return "£";
            case "JPY": return "¥";
            case "INR": return "₹";
            case "KRW": return "₩";
            case "SGD": return "S$";
            case "AUD": return "A$";
            case "CAD": return "C$";
            case "CNY": return "¥";
            case "CHF": return "Fr";
            case "NZD": return "NZ$";
            case "THB": return "฿";
            case "VND": return "₫";
            default: return "₱"; // PHP default
        }
    }
}