package com.example.fintrac;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyHelper {
    private static final String PREF_NAME = "AppPreferences";
    private static final String KEY_CURRENCY = "selected_currency";
    private static final String TAG = "CurrencyHelper";

    // Save Currency Choice (e.g., "PHP", "USD")
    public static void setCurrency(Context context, String currencyCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENCY, currencyCode).apply();
        Log.d(TAG, "Currency saved: " + currencyCode);
    }

    // Get Current Currency Code
    public static String getCurrencyCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String currency = prefs.getString(KEY_CURRENCY, "PHP");
        Log.d(TAG, "Retrieved currency: " + currency);
        return currency;
    }

    // Format Amount (e.g., converts 500.0 to "₱500.00" or "$500.00")
    public static String formatAmount(Context context, double amount) {
        String code = getCurrencyCode(context);
        Log.d(TAG, "Formatting amount " + amount + " with currency: " + code);

        NumberFormat format;

        switch (code) {
            case "USD":
                format = NumberFormat.getCurrencyInstance(Locale.US);
                break;
            case "EUR":
                format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                break;
            case "GBP":
                format = NumberFormat.getCurrencyInstance(Locale.UK);
                break;
            case "JPY":
                format = NumberFormat.getCurrencyInstance(Locale.JAPAN);
                break;
            case "INR":
                format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                break;
            case "THB":
                format = NumberFormat.getCurrencyInstance(new Locale("th", "TH"));
                break;
            case "KRW":
                format = NumberFormat.getCurrencyInstance(Locale.KOREA);
                break;
            case "SGD":
                format = NumberFormat.getCurrencyInstance(new Locale("en", "SG"));
                break;
            case "AUD":
                format = NumberFormat.getCurrencyInstance(new Locale("en", "AU"));
                break;
            case "CAD":
                format = NumberFormat.getCurrencyInstance(Locale.CANADA);
                break;
            case "CNY":
                format = NumberFormat.getCurrencyInstance(Locale.CHINA);
                break;
            case "CHF":
                format = NumberFormat.getCurrencyInstance(new Locale("de", "CH"));
                break;
            case "NZD":
                format = NumberFormat.getCurrencyInstance(new Locale("en", "NZ"));
                break;
            case "VND":
                format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                break;
            default:
                format = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
                break;
        }

        String formatted = format.format(amount);
        Log.d(TAG, "Formatted result: " + formatted);
        return formatted;
    }

    // Get currency symbol only (without the amount)
    public static String getCurrencySymbol(Context context) {
        String code = getCurrencyCode(context);
        switch (code) {
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
            default: return "₱";
        }
    }
}