package com.example.fintrac.history;

import com.example.fintrac.Transaction;
import com.example.fintrac.core.Money;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure rules for the History screen: merging the three sources, removing duplicates,
 * filtering, and period totals. No Android or Firebase calls, so it's unit-tested.
 *
 * Sources:
 *  - personal: users/{uid}/transactions (manual entries plus auto-recorded
 *    "Paid Bill: …" and "Saved to Jar: …" records)
 *  - paid bills, synthesized as entries with id "bill_…"
 *  - shared-jar activity by this user, synthesized with id "root_jar_…"
 */
public final class HistoryEntries {

    public enum Filter { ALL, INCOME, EXPENSE, BILLS, JARS }

    static final String BILL_PREFIX = "bill_";
    static final String JAR_PREFIX = "root_jar_";
    static final String PAID_BILL_TITLE = "Paid Bill: ";
    static final String SAVED_TO_JAR_TITLE = "Saved to Jar: ";
    static final String JAR_ACTIVITY_TITLE = "Jar Activity: ";

    private HistoryEntries() {}

    /**
     * Combines the sources, newest first. A synthesized bill or jar entry is dropped when the
     * personal history already has the matching auto-recorded payment (same name and amount),
     * since that's the same money movement listed twice.
     */
    public static List<Transaction> merge(List<Transaction> personal, List<Transaction> paidBills,
                                          List<Transaction> jarActivity) {
        Map<String, Integer> recordedBills = new HashMap<>();
        Map<String, Integer> recordedJarSavings = new HashMap<>();
        List<Transaction> result = new ArrayList<>();

        for (Transaction t : personal) {
            if (isHiddenZero(t)) continue;
            String title = t.getTitle() != null ? t.getTitle() : "";
            if (title.startsWith(PAID_BILL_TITLE)) {
                increment(recordedBills, key(title.substring(PAID_BILL_TITLE.length()), t));
            } else if (title.startsWith(SAVED_TO_JAR_TITLE)) {
                increment(recordedJarSavings, key(title.substring(SAVED_TO_JAR_TITLE.length()), t));
            }
            result.add(t);
        }
        for (Transaction t : paidBills) {
            if (Money.ofPesos(t.getAmount()).cents() == 0) continue;
            String name = stripPrefix(t.getTitle(), PAID_BILL_TITLE);
            if (!consume(recordedBills, key(name, t))) result.add(t);
        }
        for (Transaction t : jarActivity) {
            if (isHiddenZero(t)) continue;
            String name = stripPrefix(t.getTitle(), JAR_ACTIVITY_TITLE);
            if (!consume(recordedJarSavings, key(name, t))) result.add(t);
        }

        result.sort((a, b) -> dateOf(b).compareTo(dateOf(a)));
        return result;
    }

    public static boolean matches(Transaction t, Filter filter) {
        String id = t.getId() != null ? t.getId() : "";
        String title = t.getTitle() != null ? t.getTitle() : "";
        String category = t.getCategory() != null ? t.getCategory().toLowerCase(Locale.US) : "";

        boolean isBill = id.startsWith(BILL_PREFIX) || title.startsWith(PAID_BILL_TITLE)
                || title.startsWith("Paid: ") || category.contains("bills");
        boolean isJar = id.startsWith(JAR_PREFIX) || category.contains("jar");
        boolean isManual = !id.startsWith(BILL_PREFIX) && !id.startsWith(JAR_PREFIX)
                && !title.startsWith(PAID_BILL_TITLE) && !title.startsWith("Paid: ");

        switch (filter) {
            case INCOME: return isManual && !t.isExpense();
            case EXPENSE: return isManual && t.isExpense();
            case BILLS: return isBill;
            case JARS: return isJar && t.getAmount() > 0;
            case ALL:
            default: return true;
        }
    }

    public static List<Transaction> filter(List<Transaction> all, Filter filter) {
        List<Transaction> out = new ArrayList<>();
        for (Transaction t : all) {
            if (matches(t, filter)) out.add(t);
        }
        return out;
    }

    /**
     * Income minus expense for entries on or after {@code from}. Shared-jar activity is
     * left out: it's money moved into savings (already counted via "Saved to Jar"), not income.
     */
    public static Money netSince(List<Transaction> all, Date from) {
        Money net = Money.ZERO;
        for (Transaction t : all) {
            if (isJarActivity(t) || dateOf(t).before(from)) continue;
            Money amount = Money.ofPesos(t.getAmount());
            net = t.isExpense() ? net.minus(amount) : net.plus(amount);
        }
        return net;
    }

    /** Income and expense totals for entries in [from, to]. Index 0 = income, 1 = expense. */
    public static Money[] totalsBetween(List<Transaction> all, Date from, Date to) {
        Money income = Money.ZERO;
        Money expense = Money.ZERO;
        for (Transaction t : all) {
            Date d = dateOf(t);
            if (isJarActivity(t) || d.before(from) || d.after(to)) continue;
            Money amount = Money.ofPesos(t.getAmount());
            if (t.isExpense()) expense = expense.plus(amount);
            else income = income.plus(amount);
        }
        return new Money[]{income, expense};
    }

    public static boolean isJarActivity(Transaction t) {
        return t.getId() != null && t.getId().startsWith(JAR_PREFIX);
    }

    public static Date dateOf(Transaction t) {
        if (t.getTimestamp() != null) return t.getTimestamp().toDate();
        if (t.getDate() != null) return t.getDate();
        return new Date(0);
    }

    // ---- Period starts ----

    public static Date startOfDaysAgo(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -days);
        return startOfDay(c);
    }

    public static Date startOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        return startOfDay(c);
    }

    public static Date startOfYear() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_YEAR, 1);
        return startOfDay(c);
    }

    private static Date startOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    // ---- Helpers ----

    /** Zero-amount jar/join events are noise in a money history. */
    private static boolean isHiddenZero(Transaction t) {
        if (Money.ofPesos(t.getAmount()).cents() != 0) return false;
        String text = ((t.getTitle() != null ? t.getTitle() : "") + " "
                + (t.getDescription() != null ? t.getDescription() : "")).toLowerCase(Locale.US);
        return text.contains("jar") || text.contains("joined");
    }

    private static String key(String name, Transaction t) {
        return (name == null ? "" : name.trim().toLowerCase(Locale.US)) + "|" + Money.ofPesos(t.getAmount()).cents();
    }

    private static String stripPrefix(String title, String prefix) {
        if (title == null) return "";
        return title.startsWith(prefix) ? title.substring(prefix.length()) : title;
    }

    private static void increment(Map<String, Integer> counts, String key) {
        counts.merge(key, 1, Integer::sum);
    }

    private static boolean consume(Map<String, Integer> counts, String key) {
        Integer n = counts.get(key);
        if (n == null || n == 0) return false;
        counts.put(key, n - 1);
        return true;
    }
}
