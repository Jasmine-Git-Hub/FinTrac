package com.example.fintrac;

import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Transaction {
    private String id;
    private String title;
    private String description;
    private String category;
    private double amount;
    private boolean isExpense;
    private Date date;
    private String userId;
    private Timestamp timestamp;

    // Empty constructor required for Firestore
    public Transaction() {}

    // Constructor for creating new transactions
    public Transaction(String title, String description, String category,
                       double amount, boolean isExpense, String userId) {
        this.title = title;
        this.description = description != null ? description : "";
        this.category = category != null ? category : "";
        this.amount = amount;
        this.isExpense = isExpense;
        this.userId = userId;
        this.date = new Date();
        this.timestamp = new Timestamp(this.date);
    }

    // Getters and Setters with PropertyName annotations
    @PropertyName("id")
    public String getId() { return id; }

    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @PropertyName("title")
    public String getTitle() { return title; }

    @PropertyName("title")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("description")
    public String getDescription() { return description; }

    @PropertyName("description")
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    @PropertyName("category")
    public String getCategory() { return category; }

    @PropertyName("category")
    public void setCategory(String category) {
        this.category = category != null ? category : "";
    }

    @PropertyName("amount")
    public double getAmount() { return amount; }

    @PropertyName("amount")
    public void setAmount(double amount) { this.amount = amount; }

    @PropertyName("isExpense")
    public boolean isExpense() { return isExpense; }

    @PropertyName("isExpense")
    public void setExpense(boolean expense) { this.isExpense = expense; }

    @PropertyName("userId")
    public String getUserId() { return userId; }

    @PropertyName("userId")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("date")
    public Date getDate() { return date; }

    @PropertyName("date")
    public void setDate(Date date) {
        this.date = date;
        // Also update timestamp when date is set
        if (date != null) {
            this.timestamp = new Timestamp(date);
        }
    }

    @PropertyName("timestamp")
    public Timestamp getTimestamp() { return timestamp; }

    @PropertyName("timestamp")
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
        // Also update date when timestamp is set
        if (timestamp != null) {
            this.date = timestamp.toDate();
        }
    }

    // Helper method to get formatted date string
    public String getFormattedDate() {
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        } else if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            return sdf.format(date);
        }
        return "";
    }

    // Helper method to get formatted time string
    public String getFormattedTime() {
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf.format(timestamp.toDate());
        } else if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf.format(date);
        }
        return "";
    }

    @NonNull
    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", amount=" + amount +
                ", isExpense=" + isExpense +
                ", timestamp=" + timestamp +
                '}';
    }
}