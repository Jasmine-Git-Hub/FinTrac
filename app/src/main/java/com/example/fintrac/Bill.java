package com.example.fintrac;

import com.example.fintrac.core.Money;
import com.google.firebase.firestore.Exclude;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import com.google.firebase.firestore.IgnoreExtraProperties;

// Optional: @IgnoreExtraProperties prevents crashes if there are fields in DB not in this class
@IgnoreExtraProperties
public class Bill {

    // Ginawang PUBLIC lahat para mabasa ni Firestore at ma-save sa database
    public String id;
    public String name;
    public String category;
    public String amount;
    public String dueDate;
    public String frequency;
    public int logoResId;
    public boolean isPaid;

    // Empty constructor (Required ni Firestore para sa pag-read ng data)
    public Bill() {
    }

    // Constructor without ID (Ginagamit kapag mag-aadd ng bagong bill)
    public Bill(String name, String category, String frequency, String amount,
                String dueDate, int logoResId, boolean isPaid) {
        this.name = name;
        this.category = category;
        this.frequency = frequency;
        this.amount = amount;
        this.dueDate = dueDate;
        this.logoResId = logoResId;
        this.isPaid = isPaid;
    }

    // Constructor with ID (Ginagamit kapag galing sa Firestore yung data)
    public Bill(String id, String name, String category, String frequency,
                String amount, String dueDate, int logoResId, boolean isPaid) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.frequency = frequency;
        this.amount = amount;
        this.dueDate = dueDate;
        this.logoResId = logoResId;
        this.isPaid = isPaid;
    }

    // Exact amount in centavos (PHP). Written alongside the legacy string "amount" so older
    // screens that read "amount" keep working; new code reads this first.
    public Long amountCents;

    /** Amount as exact money, preferring amountCents and falling back to the legacy string. */
    @Exclude
    public Money money() {
        return amountCents != null ? Money.ofCents(amountCents) : Money.parse(amount);
    }

    @Exclude
    public void setMoney(Money money) {
        this.amountCents = money.cents();
        this.amount = money.toPlainString();
    }

    /** Due date is stored as "Due: M/d/yyyy". Returns null if missing or unreadable. */
    @Exclude
    public Date dueDateValue() {
        if (dueDate == null) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy", Locale.US);
            sdf.setLenient(false);
            return sdf.parse(dueDate.replace("Due: ", "").trim());
        } catch (ParseException e) {
            return null;
        }
    }

    @Exclude
    public boolean isOverdue(Date now) {
        Date due = dueDateValue();
        return due != null && due.before(now);
    }

    /** Field-by-field comparison for list diffing (Bill has no equals/hashCode by design). */
    @Exclude
    public boolean sameContentAs(Bill other) {
        return other != null
                && Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(category, other.category)
                && Objects.equals(frequency, other.frequency)
                && Objects.equals(dueDate, other.dueDate)
                && money().equals(other.money())
                && logoResId == other.logoResId
                && isPaid == other.isPaid;
    }

    // Optional: Helper method para makuha ang ID kahit private (kung gusto mo ibalik sa private later)
    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }
}