package com.example.fintrac.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.fintrac.Transaction;
import com.example.fintrac.core.Money;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HistoryEntriesTest {

    private static Transaction tx(String id, String title, String category, double amount, boolean expense, long time) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setTitle(title);
        t.setCategory(category);
        t.setAmount(amount);
        t.setExpense(expense);
        t.setDate(new Date(time));
        return t;
    }

    @Test
    public void paidBillIsNotCountedTwice() {
        List<Transaction> personal = Collections.singletonList(
                tx("p1", "Paid Bill: Internet", "Utilities", 1500, true, 2000));
        List<Transaction> bills = Collections.singletonList(
                tx("bill_b1", "Paid Bill: Internet", "Bills", 1500, true, 1000));

        List<Transaction> merged = HistoryEntries.merge(personal, bills, Collections.emptyList());

        assertEquals(1, merged.size());
        assertEquals(Money.ofPesos(-1500), HistoryEntries.netSince(merged, new Date(0)));
    }

    @Test
    public void oldPaidBillWithoutRecordIsKept() {
        List<Transaction> bills = Collections.singletonList(
                tx("bill_b1", "Paid Bill: Rent", "Bills", 8000, true, 1000));

        List<Transaction> merged = HistoryEntries.merge(Collections.emptyList(), bills, Collections.emptyList());

        assertEquals(1, merged.size());
    }

    @Test
    public void jarDepositIsNotCountedAsIncome() {
        List<Transaction> personal = Collections.singletonList(
                tx("p1", "Saved to Jar: Trip", "Savings", 500, true, 2000));
        List<Transaction> jars = Arrays.asList(
                tx("root_jar_1", "Jar Activity: Trip", "Jars", 500, false, 2000),
                tx("root_jar_2", "Jar Activity: Old", "Jars", 300, false, 1500));

        List<Transaction> merged = HistoryEntries.merge(personal, Collections.emptyList(), jars);

        // The matching jar entry is dropped; the unmatched one stays listed but isn't income
        assertEquals(2, merged.size());
        assertEquals(Money.ofPesos(-500), HistoryEntries.netSince(merged, new Date(0)));
    }

    @Test
    public void netUsesExactCentavos() {
        List<Transaction> personal = Arrays.asList(
                tx("a", "Salary", "Income", 0.10, false, 1000),
                tx("b", "Bonus", "Income", 0.20, false, 1000));
        assertEquals(30, HistoryEntries.netSince(personal, new Date(0)).cents());
    }

    @Test
    public void newestFirst() {
        List<Transaction> personal = Arrays.asList(
                tx("old", "A", "x", 1, false, 1000),
                tx("new", "B", "x", 1, false, 5000));
        assertEquals("new", HistoryEntries.merge(personal, Collections.emptyList(), Collections.emptyList()).get(0).getId());
    }

    @Test
    public void filters() {
        Transaction manualIncome = tx("m", "Salary", "Income", 100, false, 1);
        Transaction bill = tx("bill_1", "Paid Bill: Net", "Bills", 50, true, 1);
        Transaction jar = tx("root_jar_1", "Jar Activity: Trip", "Jars", 20, false, 1);

        assertTrue(HistoryEntries.matches(manualIncome, HistoryEntries.Filter.INCOME));
        assertFalse(HistoryEntries.matches(bill, HistoryEntries.Filter.EXPENSE));
        assertTrue(HistoryEntries.matches(bill, HistoryEntries.Filter.BILLS));
        assertTrue(HistoryEntries.matches(jar, HistoryEntries.Filter.JARS));
        assertFalse(HistoryEntries.matches(jar, HistoryEntries.Filter.INCOME));
    }
}
