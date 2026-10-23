package com.example.fintrac.home;

import static org.junit.Assert.assertEquals;

import com.example.fintrac.Bill;
import com.example.fintrac.JarFirestoreModel;
import com.example.fintrac.Transaction;
import com.example.fintrac.core.Money;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeMathTest {

    private static Transaction tx(double amount, boolean expense, Date date) {
        Transaction t = new Transaction();
        t.setTitle("t");
        t.setAmount(amount);
        t.setExpense(expense);
        t.setDate(date);
        return t;
    }

    private static JarFirestoreModel jar(double saved, Map<String, Double> contributions, List<String> people, boolean archived) {
        JarFirestoreModel jar = new JarFirestoreModel();
        jar.setSavedAmount(saved);
        jar.setParticipantContributions(contributions);
        jar.setParticipants(people);
        jar.setArchived(archived);
        return jar;
    }

    @Test
    public void inJarsCountsOnlyMyShareOfActiveJars() {
        Map<String, Double> shared = new HashMap<>();
        shared.put("me", 300.0);
        shared.put("friend", 700.0);
        Map<String, Double> archivedMine = new HashMap<>();
        archivedMine.put("me", 5000.0);

        List<JarFirestoreModel> jars = Arrays.asList(
                jar(1000, shared, Arrays.asList("me", "friend"), false),
                jar(5000, archivedMine, Collections.singletonList("me"), true),
                jar(250, new HashMap<>(), Collections.singletonList("me"), false)); // old solo jar

        assertEquals(Money.ofPesos(550), HomeMath.myJarSavings(jars, "me"));
    }

    @Test
    public void incomeAndExpenseAreExact() {
        Date now = new Date();
        Money[] totals = HomeMath.incomeAndExpense(Arrays.asList(
                tx(0.10, false, now), tx(0.20, false, now), tx(0.05, true, now)));
        assertEquals(30, totals[0].cents());
        assertEquals(5, totals[1].cents());
    }

    @Test
    public void recentIsNewestFirstWithinAWeekAndLimited() {
        Calendar c = Calendar.getInstance();
        Date now = c.getTime();
        c.add(Calendar.DAY_OF_YEAR, -1);
        Date yesterday = c.getTime();
        c.add(Calendar.DAY_OF_YEAR, -30);
        Date old = c.getTime();

        List<Transaction> all = new ArrayList<>(Arrays.asList(
                tx(1, true, yesterday), tx(2, true, now), tx(3, true, old)));
        List<Transaction> recent = HomeMath.recent(all, 5, now);

        assertEquals(2, recent.size());
        assertEquals(2.0, recent.get(0).getAmount(), 0);

        assertEquals(1, HomeMath.recent(all, 1, now).size());
    }

    @Test
    public void monthlyBucketsOnlyCountExpensesThisYear() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.MARCH, 10);
        Date march = c.getTime();
        c.set(2025, Calendar.MARCH, 10);
        Date lastYear = c.getTime();
        c.set(2026, Calendar.OCTOBER, 1);
        Date now = c.getTime();

        Money[] buckets = HomeMath.expenseBuckets(Arrays.asList(
                tx(100, true, march), tx(50, false, march), tx(999, true, lastYear)),
                HomeMath.ChartRange.MONTHLY, now);

        assertEquals(12, buckets.length);
        assertEquals(Money.ofPesos(100), buckets[Calendar.MARCH]);
    }

    @Test
    public void upcomingBillsSortedByDueDate() {
        Bill later = new Bill("B", "x", "One-time", "10", "Due: 12/1/2026", 0, false);
        Bill sooner = new Bill("A", "x", "One-time", "20", "Due: 11/1/2026", 0, false);
        Bill paid = new Bill("C", "x", "One-time", "30", "Due: 10/1/2026", 0, true);

        List<Bill> upcoming = HomeMath.upcomingUnpaid(Arrays.asList(later, sooner, paid));

        assertEquals(2, upcoming.size());
        assertEquals("A", upcoming.get(0).name);
        assertEquals(Money.ofPesos(30), HomeMath.total(upcoming));
    }
}
