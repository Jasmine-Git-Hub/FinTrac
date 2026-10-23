package com.example.fintrac;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.util.Log;

public class FirestoreHelper {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRANSACTIONS = "transactions";
    private static final String COLLECTION_JAR_TRANSACTIONS = "jarTransactions";

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private static FirestoreHelper instance;

    private FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirestoreHelper getInstance() {
        if (instance == null) {
            instance = new FirestoreHelper();
        }
        return instance;
    }

    private String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ============ USER OPERATIONS ============
    public void saveUserData(String userId, String name, String email, final OnUserSavedListener listener) {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("name", name);
        user.put("email", email);
        user.put("createdAt", new Date());

        db.collection(COLLECTION_USERS)
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Check if a user exists in Firestore by their user ID
     * @param userId The Firebase user ID to check
     * @param listener Callback to handle the result
     */
    public void checkUserExists(String userId, final OnUserExistsListener listener) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (listener != null) {
                        listener.onResult(documentSnapshot.exists());
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Update user name
     * @param userId The Firebase user ID
     * @param newName The new name to set
     * @param listener Callback for success/failure
     */
    public void updateUserName(String userId, String newName, final OnUserUpdatedListener listener) {
        db.collection(COLLECTION_USERS)
                .document(userId)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    // ============ TRANSACTION OPERATIONS ============
    public void addTransaction(Transaction transaction, final OnTransactionAddedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }

        if (transaction.getTimestamp() == null) {
            transaction.setTimestamp(new Timestamp(new Date()));
        }

        transaction.setUserId(userId);

        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_TRANSACTIONS)
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    transaction.setId(documentReference.getId());
                    documentReference.update("id", documentReference.getId());
                    if (listener != null) {
                        listener.onSuccess(transaction);
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    public void clearFirestoreCache() {
        try {
            FirebaseFirestore.getInstance().clearPersistence();
            Log.d("FirestoreHelper", "Firestore cache cleared");
        } catch (Exception e) {
            Log.e("FirestoreHelper", "Error clearing cache", e);
        }
    }

    public void getTotalBalance(final OnBalanceLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_TRANSACTIONS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double totalBalance = 0;
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        if (transaction != null) {
                            String description = transaction.getDescription();
                            String title = transaction.getTitle();

                            if ((description != null && (description.toLowerCase().contains("jar:") || description.toLowerCase().contains("joined"))) ||
                                    (title != null && (title.toLowerCase().contains("jar:") || title.toLowerCase().contains("joined")))) {
                                continue;
                            }

                            if (transaction.isExpense()) {
                                totalBalance -= transaction.getAmount();
                            } else {
                                totalBalance += transaction.getAmount();
                            }
                        }
                    }

                    if (listener != null) {
                        listener.onSuccess(totalBalance);
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    public void getWeeklyExpenses(final OnWeeklyExpensesLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfWeek = calendar.getTime();

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfWeek = calendar.getTime();

        db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo("isExpense", true)
                .whereGreaterThanOrEqualTo("timestamp", new Timestamp(startOfWeek))
                .whereLessThanOrEqualTo("timestamp", new Timestamp(endOfWeek))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    float[] weeklyExpenses = new float[7];

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = document.toObject(Transaction.class);
                        if (transaction != null && transaction.getTimestamp() != null) {
                            String description = transaction.getDescription();
                            String title = transaction.getTitle();

                            if ((description != null && (description.toLowerCase().contains("jar:") || description.toLowerCase().contains("joined"))) ||
                                    (title != null && (title.toLowerCase().contains("jar:") || title.toLowerCase().contains("joined")))) {
                                continue;
                            }

                            Calendar cal = Calendar.getInstance();
                            cal.setTime(transaction.getTimestamp().toDate());
                            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                            int index = (dayOfWeek == Calendar.SUNDAY) ? 0 : dayOfWeek - 1;

                            weeklyExpenses[index] += (float) transaction.getAmount();
                        }
                    }

                    if (listener != null) {
                        listener.onSuccess(weeklyExpenses);
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    private void sortBillsByDueDate(List<Bill> bills) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

        bills.sort((bill1, bill2) -> {
            try {
                String date1 = bill1.dueDate.replace("Due: ", "");
                String date2 = bill2.dueDate.replace("Due: ", "");

                Date d1 = sdf.parse(date1);
                Date d2 = sdf.parse(date2);

                if (d1 == null) return 1;
                if (d2 == null) return -1;

                return d1.compareTo(d2);
            } catch (Exception e) {
                return 0;
            }
        });
    }

    // ============ JAR TRANSACTIONS OPERATIONS ============
    public void addJarTransaction(String jarId, String jarName, String userId, String userName,
                                  String type, double amount, String note) {
        Map<String, Object> jarTransaction = new HashMap<>();
        jarTransaction.put("jarId", jarId);
        jarTransaction.put("jarName", jarName);
        jarTransaction.put("userId", userId);
        jarTransaction.put("userName", userName);
        jarTransaction.put("type", type);
        jarTransaction.put("amount", amount);
        jarTransaction.put("note", note);
        jarTransaction.put("timestamp", new Date());

        db.collection(COLLECTION_JAR_TRANSACTIONS)
                .add(jarTransaction)
                .addOnSuccessListener(doc -> {
                    Log.d("FirestoreHelper", "Jar transaction stored successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreHelper", "Error storing jar transaction", e);
                });
    }

    // ============ LISTENERS ============
    public interface OnUserSavedListener {
        void onSuccess();
        void onFailure(String error);
    }

    /**
     * Listener for checking if a user exists
     */
    public interface OnUserExistsListener {
        void onResult(boolean exists);
        void onFailure(String error);
    }

    /**
     * Listener for loading user data
     */
    public interface OnUserDataLoadedListener {
        void onSuccess(String name, String email, Date createdAt);
        void onFailure(String error);
    }

    /**
     * Listener for user update operations
     */
    public interface OnUserUpdatedListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnTransactionAddedListener {
        void onSuccess(Transaction transaction);
        void onFailure(String error);
    }

    public interface OnTransactionsLoadedListener {
        void onSuccess(List<Transaction> transactions);
        void onFailure(String error);
    }

    public interface OnBalanceLoadedListener {
        void onSuccess(double balance);
        void onFailure(String error);
    }

    public interface OnWeeklyExpensesLoadedListener {
        void onSuccess(float[] weeklyExpenses);
        void onFailure(String error);
    }

    public interface OnUnpaidBillsLoadedListener {
        void onSuccess(List<Bill> bills);
        void onFailure(String error);
    }
}