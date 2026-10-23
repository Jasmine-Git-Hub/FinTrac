package com.example.fintrac;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DeadlineReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "deadline_reminders_channel";
    private boolean isSilent = false; // FLAG PARA SA SILENT SYNC

    @Override
    public void onReceive(Context context, Intent intent) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Kunin yung flag kung silent sync lang ba mula sa HomeFragment
        isSilent = intent.getBooleanExtra("SILENT_SYNC", false);

        String userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        checkBillDeadlines(context, db, userId, today);
        checkSharedJarDeadlines(context, db, userId, today);
        checkPersonalJarDeadlines(context, db, userId, today);
        checkChallengeStatus(context, db, userId);
    }

    private void checkBillDeadlines(Context context, FirebaseFirestore db, String userId, Calendar today) {
        db.collection("users").document(userId).collection("bills")
                .whereEqualTo("isPaid", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String dueDateStr = doc.getString("dueDate");
                        if (dueDateStr != null) {
                            dueDateStr = dueDateStr.replace("Due: ", "").trim();
                            try {
                                Date dueDate = sdf.parse(dueDateStr);
                                if (dueDate != null) {
                                    processDeadline(context, db, userId, dueDate, today,
                                            "Bill", doc.getString("name"), doc.getId(), "BILL_DUE");
                                }
                            } catch (Exception e) { Log.e("Deadline", "Bill date error", e); }
                        }
                    }
                });
    }

    private void checkSharedJarDeadlines(Context context, FirebaseFirestore db, String userId, Calendar today) {
        db.collection("jars").whereArrayContains("participants", userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        processJarDocument(context, db, userId, today, doc);
                    }
                });
    }

    private void checkPersonalJarDeadlines(Context context, FirebaseFirestore db, String userId, Calendar today) {
        db.collection("users").document(userId).collection("jars").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        processJarDocument(context, db, userId, today, doc);
                    }
                });
    }

    private void processJarDocument(Context context, FirebaseFirestore db, String userId, Calendar today, QueryDocumentSnapshot doc) {
        Boolean isArchived = doc.getBoolean("isArchived");
        if (isArchived != null && isArchived) return;

        double saved = 0.0, goal = 0.0;
        Object savedObj = doc.get("savedAmount");
        Object goalObj = doc.get("goalAmount");

        if (savedObj instanceof Number) saved = ((Number) savedObj).doubleValue();
        else if (savedObj instanceof String) try { saved = Double.parseDouble(((String) savedObj).replaceAll("[^\\d.]", "")); } catch (Exception e){}

        if (goalObj instanceof Number) goal = ((Number) goalObj).doubleValue();
        else if (goalObj instanceof String) try { goal = Double.parseDouble(((String) goalObj).replaceAll("[^\\d.]", "")); } catch (Exception e){}

        if (goal > 0 && saved >= goal) return;

        String deadlineStr = doc.getString("deadline");
        if (deadlineStr != null && !deadlineStr.isEmpty()) {
            Date deadlineDate = parseDate(deadlineStr);
            if (deadlineDate != null) {
                processDeadline(context, db, userId, deadlineDate, today, "Jar", doc.getString("name"), doc.getId(), "JAR_DEADLINE");
            }
        }
    }

    private void checkChallengeStatus(Context context, FirebaseFirestore db, String userId) {
        db.collection("users").document(userId).collection("challenges")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    long now = System.currentTimeMillis();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isArchived = doc.getBoolean("isArchived");
                        if (isArchived != null && isArchived) continue;

                        String name = doc.getString("name");
                        Long currentDay = doc.getLong("currentDay");
                        Long totalDays = doc.getLong("totalDays");
                        Long lastCheckIn = doc.getLong("lastCheckInTime");

                        if (currentDay == null || totalDays == null) continue;

                        // Same 8 AM rule as the Check In button, so we never nudge while it's still locked
                        boolean canCheckIn = ChallengeItem.canCheckIn(lastCheckIn == null ? 0 : lastCheckIn, now);
                        if (canCheckIn && currentDay < totalDays) {
                            String msg = "Consistency is key! Time to check-in for your '" + name + "' challenge.";
                            // PASSED isSilent HERE
                            showSystemNotification(context, "Challenge Check-in", msg, (doc.getId() + "checkin").hashCode(), isSilent);
                            addInAppNotification(db, userId, "Challenge Check-in", msg, "CHALLENGE_REMINDER", doc.getId(), name);
                        }

                        long daysLeft = totalDays - currentDay;
                        if (daysLeft == 1 || daysLeft == 0) {
                            String msg = (daysLeft == 0) ? "TODAY is the last day of your '" + name + "' challenge! Finish strong!"
                                    : "Only 1 day left to complete your '" + name + "' challenge!";
                            // PASSED isSilent HERE
                            showSystemNotification(context, "Challenge Ending Soon", msg, (doc.getId() + "end").hashCode(), isSilent);
                            addInAppNotification(db, userId, "Challenge Ending Soon", msg, "CHALLENGE_DEADLINE", doc.getId(), name);
                        }
                    }
                });
    }

    private void processDeadline(Context context, FirebaseFirestore db, String userId, Date dueDate, Calendar today,
                                 String typeStr, String title, String docId, String notifType) {
        Calendar dueCal = Calendar.getInstance();
        dueCal.setTime(dueDate);
        dueCal.set(Calendar.HOUR_OF_DAY, 0);
        dueCal.set(Calendar.MINUTE, 0);
        dueCal.set(Calendar.SECOND, 0);
        dueCal.set(Calendar.MILLISECOND, 0);

        long diffInMillis = dueCal.getTimeInMillis() - today.getTimeInMillis();
        long daysLeft = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        if (daysLeft <= 3) { // Binago ko konti para pumasok pati yung mga less than 3 days
            String timeMsg;
            if (daysLeft == 0) timeMsg = "TODAY!";
            else if (daysLeft < 0) timeMsg = "OVERDUE by " + Math.abs(daysLeft) + " days!";
            else timeMsg = "in " + daysLeft + " days.";

            String message = "Your " + typeStr + " '" + title + "' is due " + timeMsg;

            // PASSED isSilent HERE
            showSystemNotification(context, typeStr + " Deadline: " + title, message, docId.hashCode(), isSilent);
            addInAppNotification(db, userId, typeStr + " Deadline: " + title, message, notifType, docId, title);
        }
    }

    private Date parseDate(String dateStr) {
        String[] formats = {"yyyy-MM-dd", "MMM dd, yyyy", "MM/dd/yyyy", "dd/MM/yyyy"};
        for (String f : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.getDefault());
                sdf.setLenient(false);
                Date d = sdf.parse(dateStr);
                if (d != null) return d;
            } catch (Exception ignored) {}
        }
        return null;
    }

    // DINAGDAG ANG BOOLEAN silent DITO PARA PIGILAN ANG POPUP
    private void showSystemNotification(Context context, String title, String message, int notificationId, boolean silent) {
        if (silent) return; // MAGIC FIX: Wag mag-popup sa phone kung silent sync!

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "FinTrac Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, builder.build());
    }

    private void addInAppNotification(FirebaseFirestore db, String userId, String title, String message, String type, String relatedId, String relatedName) {
        String dateKey = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String uniqueNotifId = type + "_" + relatedId + "_" + dateKey;

        Map<String, Object> notification = new HashMap<>();
        notification.put("id", uniqueNotifId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("isRead", false);
        notification.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        if (type.contains("JAR")) {
            notification.put("jarId", relatedId);
            notification.put("jarName", relatedName);
        }

        db.collection("users").document(userId).collection("notifications")
                .document(uniqueNotifId)
                .set(notification)
                .addOnSuccessListener(aVoid -> Log.d("Deadline", "Notification safely sent and secured from duplicates."));
    }
}