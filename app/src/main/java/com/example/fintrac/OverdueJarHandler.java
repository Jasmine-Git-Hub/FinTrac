package com.example.fintrac;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OverdueJarHandler {
    private static final String TAG = "OverdueJarHandler";
    private static final String COLLECTION_JARS = "jars";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public OverdueJarHandler() {
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    public interface OverdueJarCallback {
        void onJarArchived(String jarId, String jarName);
        void onError(Exception e);
    }

    public void checkAndArchiveOverdueJars(Context context, OverdueJarCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "User not logged in");
            return;
        }

        String userId = currentUser.getUid();
        Date today = getTodayWithoutTime();

        // Filter archived jars here rather than in the query: new jars store the flag as
        // "archived" (so whereEqualTo("isArchived", false) skipped them), and combining it with
        // array-contains would also need a composite index.
        db.collection(COLLECTION_JARS)
                .whereArrayContains("participants", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (Boolean.TRUE.equals(doc.getBoolean("isArchived"))) continue;
                        String deadlineStr = doc.getString("deadline");
                        if (deadlineStr == null || deadlineStr.isEmpty()) continue;

                        Date deadlineDate = parseJarDate(deadlineStr);
                        if (deadlineDate == null) continue;

                        // Check if overdue (deadline is before today)
                        if (deadlineDate.before(today)) {
                            String jarId = doc.getId();
                            String jarName = doc.getString("name");

                            Log.d(TAG, "Found overdue jar: " + jarName + " (ID: " + jarId + ")");

                            // Archive the jar
                            archiveJar(jarId, jarName, userId, context, callback);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking overdue jars", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    private void archiveJar(String jarId, String jarName, String userId, Context context, OverdueJarCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isArchived", true);
        updates.put("archivedAt", Timestamp.now());
        updates.put("archivedReason", "overdue");

        db.collection(COLLECTION_JARS).document(jarId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Archived overdue jar: " + jarName);

                    // Create in-app notification
                    createInAppNotification(jarId, jarName, userId);

                    // Send system notification
                    sendOverdueNotification(context, jarName, jarId);

                    if (callback != null) {
                        callback.onJarArchived(jarId, jarName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to archive jar: " + jarName, e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    private void createInAppNotification(String jarId, String jarName, String userId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "Jar Moved to Archives");
        notification.put("message", "\"" + jarName + "\" has been moved to Archives because it's overdue. It will be permanently deleted after 30 days.");
        notification.put("type", "JAR_OVERDUE");
        notification.put("jarId", jarId);
        notification.put("jarName", jarName);
        notification.put("isRead", false);
        notification.put("timestamp", Timestamp.now());

        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "In-app notification created for overdue jar");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create in-app notification", e);
                });
    }

    private void sendOverdueNotification(Context context, String jarName, String jarId) {
        // Create intent to open ArchivesActivity directly
        Intent intent = new Intent(context, ArchivesActivity.class);
        intent.putExtra("SHOW_OVERDUE_TOAST", true);
        intent.putExtra("OVERDUE_JAR_NAME", jarName);
        intent.putExtra("OVERDUE_JAR_ID", jarId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Use your existing notification system
        String title = "Jar Overdue: " + jarName;
        String message = "Its deadline has passed, so it was moved to Archives. You can restore it within 30 days.";

        // You can use your existing NotificationHelper or create a simple one
        showNotification(context, title, message, intent, jarId.hashCode());
    }

    private void showNotification(Context context, String title, String message, Intent intent, int notificationId) {
        if (!NotificationPrefs.canNotify(context)) return;
        android.app.NotificationManager notificationManager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "overdue_jars_channel",
                    "Overdue Jars",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for overdue jars");
            notificationManager.createNotificationChannel(channel);
        }

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );

        androidx.core.app.NotificationCompat.Builder builder =
                new androidx.core.app.NotificationCompat.Builder(context, "overdue_jars_channel")
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, builder.build());
    }

    private Date getTodayWithoutTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date parseJarDate(String dateStr) {
        String[] formats = {"yyyy-MM-dd", "MMM dd, yyyy", "MM/dd/yyyy", "dd/MM/yyyy"};
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                sdf.setLenient(false);
                Date date = sdf.parse(dateStr);
                if (date != null) {
                    // Reset time part for accurate comparison
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal.getTime();
                }
            } catch (ParseException ignored) {}
        }
        return null;
    }
}