package com.example.fintrac;

import com.example.fintrac.core.Money;
import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.Calendar;

public class ChallengeItem implements Serializable {
    // Firebase Document ID (Exclude so it doesn't save inside the document)
    @Exclude
    private String documentId;

    private String name;
    private String description;
    private int progress;
    private String daysLeft;
    private String percentDone;
    private int iconResId;
    private int colorResId;
    private int currentDay;
    private int totalDays;
    private String progressStart;
    private String progressEnd;
    private double targetAmount;
    private long lastCheckInTime;

    // NEW: Added isArchived to fix the error
    private boolean isArchived;

    // 1. REQUIRED: Empty Constructor for Firestore
    public ChallengeItem() {
        this.currentDay = 0;
        this.totalDays = 7;
        this.progress = 0;
        this.lastCheckInTime = 0;
        this.isArchived = false;
    }

    // 2. Main Constructor
    public ChallengeItem(String name, String description, int iconResId, int colorResId,
                         int currentDay, int totalDays, double targetAmount) {
        this.name = name;
        this.description = description;
        this.iconResId = iconResId;
        this.colorResId = colorResId;
        this.currentDay = currentDay;
        this.totalDays = totalDays;
        this.targetAmount = targetAmount;
        this.lastCheckInTime = 0;
        this.isArchived = false;
        updateCalculatedFields();
    }

    public void updateCalculatedFields() {
        this.progress = (totalDays == 0) ? 0 : (int) ((currentDay * 100.0) / totalDays);
        this.daysLeft = (totalDays - currentDay) + " days left";
        this.percentDone = this.progress + "% done";
        this.progressStart = "Day " + currentDay;
        this.progressEnd = totalDays + " days";
    }

    /** Hour of the day when a new check-in day starts. */
    public static final int CHECK_IN_RESET_HOUR = 8;

    /**
     * The one check-in rule used by the screen, the save, and the reminders:
     * a new check-in day starts at 8:00 AM (not midnight).
     */
    public static boolean canCheckIn(long lastCheckInTime, long now) {
        if (lastCheckInTime == 0) return true;
        Calendar boundary = Calendar.getInstance();
        boundary.setTimeInMillis(now);
        boundary.set(Calendar.HOUR_OF_DAY, CHECK_IN_RESET_HOUR);
        boundary.set(Calendar.MINUTE, 0);
        boundary.set(Calendar.SECOND, 0);
        boundary.set(Calendar.MILLISECOND, 0);
        if (now < boundary.getTimeInMillis()) {
            boundary.add(Calendar.DAY_OF_YEAR, -1);
        }
        return lastCheckInTime < boundary.getTimeInMillis();
    }

    @Exclude
    public boolean canCheckInNow() {
        return !isFinished() && canCheckIn(lastCheckInTime, System.currentTimeMillis());
    }

    @Exclude
    public boolean isFinished() {
        return totalDays > 0 && currentDay >= totalDays;
    }

    /** Amount saved per check-in (base currency). Zero for malformed challenges. */
    @Exclude
    public Money dailyAmount() {
        if (totalDays <= 0) return Money.ZERO;
        return Money.ofCents(Money.ofPesos(targetAmount).cents() / totalDays);
    }

    /** Saved so far; the final day gets any rounding remainder so a finished challenge equals its target. */
    @Exclude
    public Money savedSoFar() {
        if (isFinished()) return Money.ofPesos(targetAmount);
        return Money.ofCents(dailyAmount().cents() * Math.max(currentDay, 0));
    }

    /** Field comparison for list diffing. */
    @Exclude
    public boolean sameContentAs(ChallengeItem other) {
        return other != null
                && java.util.Objects.equals(documentId, other.documentId)
                && java.util.Objects.equals(name, other.name)
                && java.util.Objects.equals(description, other.description)
                && currentDay == other.currentDay
                && totalDays == other.totalDays
                && targetAmount == other.targetAmount
                && lastCheckInTime == other.lastCheckInTime;
    }

    // --- Getters & Setters ---

    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public String getDaysLeft() { return daysLeft; }
    public void setDaysLeft(String daysLeft) { this.daysLeft = daysLeft; }

    public String getPercentDone() { return percentDone; }
    public void setPercentDone(String percentDone) { this.percentDone = percentDone; }

    public int getIconResId() { return iconResId; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }

    public int getColorResId() { return colorResId; }
    public void setColorResId(int colorResId) { this.colorResId = colorResId; }

    public int getCurrentDay() { return currentDay; }
    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
        updateCalculatedFields();
    }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public String getProgressStart() { return progressStart; }
    public void setProgressStart(String progressStart) { this.progressStart = progressStart; }

    public String getProgressEnd() { return progressEnd; }
    public void setProgressEnd(String progressEnd) { this.progressEnd = progressEnd; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public long getLastCheckInTime() { return lastCheckInTime; }
    public void setLastCheckInTime(long lastCheckInTime) { this.lastCheckInTime = lastCheckInTime; }

    // NEW: Getters and Setters for Archive
    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }
}