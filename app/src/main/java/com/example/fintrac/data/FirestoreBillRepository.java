package com.example.fintrac.data;

import android.util.Log;

import com.example.fintrac.Bill;
import com.example.fintrac.FirestoreHelper;
import com.example.fintrac.R;
import com.example.fintrac.Transaction;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bills stored at users/{uid}/bills. */
public class FirestoreBillRepository implements BillRepository {

    private static final String TAG = "BillRepository";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FirestoreBillRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance());
    }

    public FirestoreBillRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    @Override
    public Subscription observeActiveBills(Listener listener) {
        CollectionReference bills = billsRef();
        if (bills == null) {
            listener.onError(new IllegalStateException("Not signed in"));
            return () -> {};
        }

        ListenerRegistration registration = bills.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshot == null) return;

            List<Bill> result = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                if (Boolean.TRUE.equals(doc.getBoolean("isArchived"))) continue;
                result.add(fromDocument(doc));
            }
            listener.onBills(result);
        });
        return registration::remove;
    }

    @Override
    public Task<Void> save(Bill bill) {
        CollectionReference bills = billsRef();
        if (bills == null) return notSignedIn();

        if (bill.id == null) {
            bill.id = bills.document().getId();
        }
        return bills.document(bill.id).set(bill);
    }

    @Override
    public Task<Void> setPaid(String billId, boolean paid) {
        CollectionReference bills = billsRef();
        if (bills == null) return notSignedIn();
        return bills.document(billId).update("isPaid", paid);
    }

    @Override
    public Task<Void> archive(String billId) {
        CollectionReference bills = billsRef();
        if (bills == null) return notSignedIn();

        Map<String, Object> updates = new HashMap<>();
        updates.put("isPaid", true);
        updates.put("isArchived", true);
        updates.put("archivedAt", FieldValue.serverTimestamp());
        return bills.document(billId).update(updates);
    }

    @Override
    public void recordPayment(Bill bill) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", "Bill Paid Successfully");
        notification.put("message", "You have successfully marked your bill '" + bill.name + "' as paid.");
        notification.put("type", "BILL_PAID");
        notification.put("isRead", false);
        notification.put("timestamp", FieldValue.serverTimestamp());
        db.collection("users").document(user.getUid()).collection("notifications")
                .add(notification)
                .addOnFailureListener(e -> Log.w(TAG, "Failed to create paid notification", e));

        Money amount = bill.money();
        if (!amount.isPositive()) return;

        Transaction expense = new Transaction(
                "Paid Bill: " + bill.name,
                "Auto-recorded payment",
                bill.category != null ? bill.category : "Bills",
                amount.toPesos(),
                true,
                user.getUid());
        FirestoreHelper.getInstance().addTransaction(expense, new FirestoreHelper.OnTransactionAddedListener() {
            @Override
            public void onSuccess(Transaction transaction) {}

            @Override
            public void onFailure(String error) {
                Log.w(TAG, "Failed to auto-record bill payment: " + error);
            }
        });
    }

    @Override
    public void clearReminders(String billId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || billId == null) return;

        db.collection("users").document(user.getUid()).collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String notifId = doc.getString("id");
                        if (notifId != null && notifId.contains(billId)) {
                            doc.getReference().update("isRead", true, "read", true);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Failed to clear reminders", e));
    }

    private CollectionReference billsRef() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return null;
        return db.collection("users").document(user.getUid()).collection("bills");
    }

    private static Task<Void> notSignedIn() {
        return Tasks.forException(new IllegalStateException("Not signed in"));
    }

    private static Bill fromDocument(DocumentSnapshot doc) {
        // The stored "logoResId" is a raw resource id from whichever build wrote it; ids change
        // between builds (especially with resource shrinking), so it's ignored to avoid crashes.
        // Every bill has always been saved with this icon anyway.
        Bill bill = new Bill(
                doc.getId(),
                doc.getString("name"),
                doc.getString("category"),
                doc.getString("frequency"),
                doc.getString("amount"),
                doc.getString("dueDate"),
                R.drawable.ic_visual_reports,
                Boolean.TRUE.equals(doc.getBoolean("isPaid")));
        bill.amountCents = doc.getLong("amountCents");
        return bill;
    }
}
