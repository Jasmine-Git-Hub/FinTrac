// File: JarFirestoreModel.java
package com.example.fintrac;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JarFirestoreModel implements Serializable {
    private String id;
    private String name;
    private String type;
    private double savedAmount;
    private double goalAmount;
    private String deadline; // Store actual deadline date (e.g., "2026-02-28")
    private String weekly;
    private String iconName;
    private int jarColor;
    private String createdBy;
    private Timestamp createdAt;
    private List<String> participants;
    private Map<String, Double> participantContributions;
    private List<JarTransaction> transactions;
    private boolean isArchived;
    private Timestamp archivedAt;

    public JarFirestoreModel() {
        // Required empty constructor for Firestore
    }

    // FIXED CONSTRUCTOR - Now uses deadline parameter instead of daysLeft
    public JarFirestoreModel(String id, String name, String type, double savedAmount, double goalAmount,
                             String deadline, String weekly, String iconName, int jarColor,
                             String createdBy) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.savedAmount = savedAmount;
        this.goalAmount = goalAmount;
        this.deadline = deadline;
        this.weekly = weekly != null ? weekly : "";
        this.iconName = iconName != null ? iconName : "hat";
        this.jarColor = jarColor;
        this.createdBy = createdBy;
        this.createdAt = Timestamp.now();
        this.isArchived = false;

        // Initialize collections
        this.participants = new ArrayList<>();
        this.participantContributions = new HashMap<>();
        this.transactions = new ArrayList<>();

        // Add creator as participant if createdBy is not null
        if (createdBy != null && !createdBy.isEmpty()) {
            this.participants.add(createdBy);
            this.participantContributions.put(createdBy, savedAmount);
        }

        // Debug log
        Log.d("JarFirestoreModel", "Jar created with deadline: '" + deadline + "'");
    }


    // Method to calculate days left dynamically
    // Replace the calculateDaysLeft() method with this improved version:

    @Exclude
    public String calculateDaysLeft() {
        if (deadline == null || deadline.isEmpty()) {
            return "No deadline";
        }

        try {
            Date deadlineDate = null;

            // Try different date formats
            String[] possibleFormats = {
                    "yyyy-MM-dd",        // Storage format
                    "MMM dd, yyyy",      // Display format
                    "MM/dd/yyyy",        // Alternative format
                    "dd/MM/yyyy"         // Another alternative
            };

            for (String format : possibleFormats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                    sdf.setLenient(false);
                    deadlineDate = sdf.parse(deadline);
                    if (deadlineDate != null) {
                        break;
                    }
                } catch (ParseException e) {
                    // Try next format
                }
            }

            if (deadlineDate == null) {
                Log.e("JarFirestoreModel", "Could not parse date: " + deadline);
                return "Invalid date";
            }

            // Get current date without time
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar deadlineCal = Calendar.getInstance();
            deadlineCal.setTime(deadlineDate);
            deadlineCal.set(Calendar.HOUR_OF_DAY, 0);
            deadlineCal.set(Calendar.MINUTE, 0);
            deadlineCal.set(Calendar.SECOND, 0);
            deadlineCal.set(Calendar.MILLISECOND, 0);

            long diffInMillis = deadlineCal.getTimeInMillis() - today.getTimeInMillis();
            long daysLeft = TimeUnit.MILLISECONDS.toDays(diffInMillis);

            if (daysLeft < 0) {
                return "Overdue";
            } else if (daysLeft == 0) {
                return "Due today";
            } else if (daysLeft == 1) {
                return "1 day left";
            } else {
                return daysLeft + " days left";
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("JarFirestoreModel", "Error calculating days left", e);
            return "Error";
        }
    }

    @Exclude
    public JarItem toJarItem() {
        JarItem item = new JarItem(
                this.id != null ? this.id : "",
                this.name != null ? this.name : "",
                this.type != null ? this.type : "Flexible",
                this.savedAmount,
                this.goalAmount,
                calculateDaysLeft(),
                this.weekly != null ? this.weekly : "",
                this.iconName != null ? this.iconName : "hat",
                this.jarColor
        );
        item.setCreatedBy(this.createdBy);
        return item;
    }
    // Getters and Setters with null safety
    public String getId() { return id != null ? id : ""; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type != null ? type : "Flexible"; }
    public void setType(String type) { this.type = type; }

    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }

    public double getGoalAmount() { return goalAmount; }
    public void setGoalAmount(double goalAmount) { this.goalAmount = goalAmount; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    // Deprecated - kept for backward compatibility
    @Deprecated
    public String getDaysLeft() { return calculateDaysLeft(); }

    @Deprecated
    public void setDaysLeft(String daysLeft) {
        // Do nothing - this field is no longer used
    }

    public String getWeekly() { return weekly != null ? weekly : ""; }
    public void setWeekly(String weekly) { this.weekly = weekly; }

    public String getIconName() { return iconName != null ? iconName : "hat"; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public int getJarColor() { return jarColor; }
    public void setJarColor(int jarColor) { this.jarColor = jarColor; }

    public String getCreatedBy() { return createdBy != null ? createdBy : ""; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public Timestamp getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Timestamp archivedAt) { this.archivedAt = archivedAt; }

    public List<String> getParticipants() {
        if (participants == null) participants = new ArrayList<>();
        return participants;
    }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public Map<String, Double> getParticipantContributions() {
        if (participantContributions == null) participantContributions = new HashMap<>();
        return participantContributions;
    }
    public void setParticipantContributions(Map<String, Double> participantContributions) {
        this.participantContributions = participantContributions;
    }

    public List<JarTransaction> getTransactions() {
        if (transactions == null) transactions = new ArrayList<>();
        return transactions;
    }
    public void setTransactions(List<JarTransaction> transactions) {
        this.transactions = transactions;
    }
}