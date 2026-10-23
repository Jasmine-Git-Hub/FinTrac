package com.example.fintrac;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FinTracWorker extends Worker {

    private static final String CHANNEL_ID = "fintrac_notifications";
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public FinTracWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        // I-check kung in-allow ba ng user ang notifications sa Settings nila
        if (!NotificationPrefs.canNotify(getApplicationContext())) {
            Log.d("FinTracWorker", "Notifications are disabled in settings. Skipping work.");
            return Result.success(); // Wag mag-send kung naka-off sa settings
        }

        FirebaseUser user = mAuth.getCurrentUser();

        // Kung walang naka-login, i-stop na ang background task
        if (user == null) {
            return Result.failure();
        }

        String userId = user.getUid();

        try {
            checkUnpaidBills(userId);
            checkJars(userId);
            checkChallenges(userId);

        } catch (Exception e) {
            Log.e("FinTracWorker", "Error fetching data for notifications: ", e);
            return Result.retry();
        }

        return Result.success();
    }

    private void checkUnpaidBills(String userId) throws Exception {
        QuerySnapshot billsSnapshot = Tasks.await(db.collection("users")
                .document(userId)
                .collection("bills")
                .whereEqualTo("isPaid", false)
                .get());

        if (!billsSnapshot.isEmpty()) {
            int unpaidCount = billsSnapshot.size();
            sendNotification("Unpaid Bills Reminder \uD83D\uDCDD",
                    "You have " + unpaidCount + " unpaid bill(s) left. Don't forget to settle them before the deadline!");
        }
    }

    private void checkJars(String userId) throws Exception {
        QuerySnapshot jarsSnapshot = Tasks.await(db.collection("jars")
                .whereArrayContains("participants", userId)
                .get());

        for (DocumentSnapshot doc : jarsSnapshot) {

            // 🔥 KAILANGAN ITO PARA DI MAG-NOTIFY ANG ARCHIVED JARS 🔥
            Boolean isArchived = doc.getBoolean("isArchived");
            if (isArchived != null && isArchived) {
                continue;
            }

            String jarName = doc.getString("name");
            Long savedAmount = doc.getLong("savedAmount");
            Long goalAmount = doc.getLong("goalAmount");
            String daysLeft = doc.getString("daysLeft");

            if (jarName == null || savedAmount == null || goalAmount == null) continue;

            if (savedAmount >= goalAmount && goalAmount > 0) {
                sendNotification("Goal Reached! \uD83C\uDF89",
                        "Congratulations! You've reached your goal amount of " + goalAmount + " for your '" + jarName + "' jar.");
            }

            if (daysLeft != null && (daysLeft.contains("1 days") || daysLeft.contains("2 days"))) {
                sendNotification("Jar Deadline Approaching! \u23F0",
                        "Your jar '" + jarName + "' is ending very soon (" + daysLeft + "). Keep saving!");
            }
        }
    }

    private void checkChallenges(String userId) throws Exception {
        QuerySnapshot challengesSnapshot = Tasks.await(db.collection("users")
                .document(userId)
                .collection("challenges")
                .whereEqualTo("isActive", true)
                .get());

        String todayDate = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(new Date());
        int uncheckedChallengesCount = 0;

        for (DocumentSnapshot doc : challengesSnapshot) {
            String lastCheckInDate = doc.getString("lastCheckInDate");

            if (lastCheckInDate == null || !lastCheckInDate.equals(todayDate)) {
                uncheckedChallengesCount++;
            }
        }

        if (uncheckedChallengesCount > 0) {
            sendNotification("Daily Challenge Pending! \uD83D\uDCAA",
                    "You have " + uncheckedChallengesCount + " active challenge(s) waiting for check-in today. Keep your streak alive!");
        }
    }

    private void sendNotification(String title, String message) {
        Context context = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FinTrac Background Sync",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, LogIn.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}