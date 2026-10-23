package com.example.fintrac.home;

import com.example.fintrac.Bill;
import com.example.fintrac.JarFirestoreModel;
import com.example.fintrac.Transaction;
import com.example.fintrac.core.Money;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pure calculations behind the Home dashboard. No Android/Firebase calls, so it's unit-tested. */
public final class HomeMath {

    public enum ChartRange { WEEKLY, MONTHLY, QUARTERLY, YEARLY }

    private HomeMath() {}

    /** Index 0 = total income, 1 = total expense (zero-amount jar bookkeeping entries ignored). */
    public static Money[] incomeAndExpense(List<Transaction> all) {
        Money income = Money.ZERO;
        Money expense = Money.ZERO;
        for (Transaction t : all) {
            Money amount = Money.ofPesos(t.getAmount());
            if (amount.cents() == 0) continue;
            if (t.isExpense()) expense = expense.plus(amount);
            else income = income.plus(amount);
        }
        return new Money[]{income, expense};
    }

    /**
     * What the signed-in user personally has saved in active jars: their own contribution,
     * not the whole balance of shared jars, and archived jars excluded.
     */
    public static Money myJarSavings(List<JarFirestoreModel> jars, String uid) {
        Money total = Money.ZERO;
        for (JarFirestoreModel jar : jars) {
            if (jar.isArchived()) continue;
            Map<String, Double> contributions = jar.getParticipantContributions();
            Double mine = contributions.get(uid);
            if (mine == null && jar.getParticipants().size() <= 1) {
                // Older solo jars may lack a contributions entry; the whole jar is theirs
                mine = jar.getSavedAmount();
            }
            if (mine != null) total = total.plus(Money.ofPesos(mine));
        }
        return total;
    }

    /** Newest first, only from the last 7 days, at most {@code limit}. */
    public static List<Transaction> recent(List<Transaction> all, int limit, Date now) {
        Calendar from = Calendar.getInstance();
        from.setTime(now);
        from.add(Calendar.DAY_OF_YEAR, -7);
        from.set(Calendar.HOUR_OF_DAY, 0);
        from.set(Calendar.MINUTE, 0);
        from.set(Calendar.SECOND, 0);
        from.set(Calendar.MILLISECOND, 0);

        List<Transaction> out = new ArrayList<>();
        for (Transaction t : all) {
            if (Money.ofPesos(t.getAmount()).cents() == 0 && mentionsJar(t)) continue;
            if (!dateOf(t).before(from.getTime())) out.add(t);
        }
        out.sort((a, b) -> dateOf(b).compareTo(dateOf(a)));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    /** Unpaid bills, soonest due first (undated last). */
    public static List<Bill> upcomingUnpaid(List<Bill> bills) {
        List<Bill> out = new ArrayList<>();
        for (Bill b : bills) {
            if (!b.isPaid) out.add(b);
        }
        out.sort((a, b) -> {
            Date da = a.dueDateValue();
            Date db = b.dueDateValue();
            if (da == null) return db == null ? 0 : 1;
            if (db == null) return -1;
            return da.compareTo(db);
        });
        return out;
    }

    public static Money total(List<Bill> bills) {
        Money total = Money.ZERO;
        for (Bill b : bills) total = total.plus(b.money());
        return total;
    }

    /**
     * Expense totals per chart bar (base currency):
     * WEEKLY = Sun..Sat of the current week, MONTHLY = Jan..Dec, QUARTERLY = Q1..Q4 of this year,
     * YEARLY = the last five years, oldest first.
     */
    public static Money[] expenseBuckets(List<Transaction> all, ChartRange range, Date now) {
        Calendar today = Calendar.getInstance();
        today.setTime(now);
        int year = today.get(Calendar.YEAR);

        Calendar weekStart = (Calendar) today.clone();
        weekStart.set(Calendar.DAY_OF_WEEK, weekStart.getFirstDayOfWeek());
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 7);

        Money[] buckets = new Money[bucketCount(range)];
        java.util.Arrays.fill(buckets, Money.ZERO);

        for (Transaction t : all) {
            if (!t.isExpense()) continue;
            Calendar c = Calendar.getInstance();
            c.setTime(dateOf(t));
            int index = -1;
            switch (range) {
                case WEEKLY:
                    if (!c.before(weekStart) && c.before(weekEnd)) index = c.get(Calendar.DAY_OF_WEEK) - 1;
                    break;
                case MONTHLY:
                    if (c.get(Calendar.YEAR) == year) index = c.get(Calendar.MONTH);
                    break;
                case QUARTERLY:
                    if (c.get(Calendar.YEAR) == year) index = c.get(Calendar.MONTH) / 3;
                    break;
                case YEARLY:
                    int diff = year - c.get(Calendar.YEAR);
                    if (diff >= 0 && diff < 5) index = 4 - diff;
                    break;
            }
            if (index >= 0) buckets[index] = buckets[index].plus(Money.ofPesos(t.getAmount()));
        }
        return buckets;
    }

    public static int bucketCount(ChartRange range) {
        switch (range) {
            case MONTHLY: return 12;
            case QUARTERLY: return 4;
            case YEARLY: return 5;
            case WEEKLY:
            default: return 7;
        }
    }

    static Date dateOf(Transaction t) {
        if (t.getTimestamp() != null) return t.getTimestamp().toDate();
        if (t.getDate() != null) return t.getDate();
        return new Date(0);
    }

    private static boolean mentionsJar(Transaction t) {
        String text = ((t.getTitle() != null ? t.getTitle() : "") + " "
                + (t.getDescription() != null ? t.getDescription() : "")).toLowerCase(Locale.US);
        return text.contains("jar");
    }
}
