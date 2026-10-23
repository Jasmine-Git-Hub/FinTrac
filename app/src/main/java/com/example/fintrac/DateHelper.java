package com.example.fintrac;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateHelper {

    private static final String DISPLAY_DATE_FORMAT = "MMM dd, yyyy";
    private static final String STORAGE_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Calculate days left from a deadline string
     * @param deadlineStr Deadline in format "MMM dd, yyyy" (e.g., "Feb 28, 2026")
     * @return Formatted string like "2 days left", "Due today", "Overdue", etc.
     */
    public static String calculateDaysLeft(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isEmpty()) {
            return "No deadline";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
            Date deadlineDate = sdf.parse(deadlineStr);

            if (deadlineDate == null) {
                return "Invalid date";
            }

            return calculateDaysLeft(deadlineDate);

        } catch (ParseException e) {
            e.printStackTrace();
            return "Invalid date";
        }
    }

    /**
     * Calculate days left from a Date object
     */
    public static String calculateDaysLeft(Date deadlineDate) {
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
    }

    /**
     * Convert display format (MMM dd, yyyy) to storage format (yyyy-MM-dd)
     */
    public static String toStorageFormat(String displayDate) {
        if (displayDate == null || displayDate.isEmpty()) {
            return "";
        }

        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
            Date date = displayFormat.parse(displayDate);

            SimpleDateFormat storageFormat = new SimpleDateFormat(STORAGE_DATE_FORMAT, Locale.getDefault());
            return storageFormat.format(date);

        } catch (ParseException e) {
            e.printStackTrace();
            return displayDate;
        }
    }

    /**
     * Convert storage format (yyyy-MM-dd) to display format (MMM dd, yyyy)
     */
    public static String toDisplayFormat(String storageDate) {
        if (storageDate == null || storageDate.isEmpty()) {
            return "";
        }

        try {
            SimpleDateFormat storageFormat = new SimpleDateFormat(STORAGE_DATE_FORMAT, Locale.getDefault());
            Date date = storageFormat.parse(storageDate);

            SimpleDateFormat displayFormat = new SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault());
            return displayFormat.format(date);

        } catch (ParseException e) {
            e.printStackTrace();
            return storageDate;
        }
    }

    /**
     * Check if a deadline is within the next X days
     */
    public static boolean isDeadlineNear(String deadlineStr, int thresholdDays) {
        String daysLeft = calculateDaysLeft(deadlineStr);

        if (daysLeft.equals("Overdue") || daysLeft.equals("Due today")) {
            return true;
        }

        try {
            if (daysLeft.contains("days left")) {
                int days = Integer.parseInt(daysLeft.split(" ")[0]);
                return days <= thresholdDays;
            } else if (daysLeft.equals("1 day left")) {
                return 1 <= thresholdDays;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return false;
    }
}