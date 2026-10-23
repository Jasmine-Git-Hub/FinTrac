package com.example.fintrac;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.util.Date;

public class TransactionItem {
    @Exclude
    private String documentId;

    private String title;
    private double amount;
    private String type;        // "income" or "expense"
    private String category;
    private String note;        // Added this para sa text_note mo
    private Timestamp timestamp;

    public TransactionItem() {} // Required for Firebase

    public TransactionItem(String title, double amount, String type, String category, String note) {
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.note = note;
        this.timestamp = new Timestamp(new Date());
    }

    // Getters
    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getNote() { return note; }
    public Timestamp getTimestamp() { return timestamp; }
}