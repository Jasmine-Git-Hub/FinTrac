package com.example.fintrac.data;

import android.util.Log;

import com.example.fintrac.Bill;
import com.example.fintrac.JarTransaction;
import com.example.fintrac.Transaction;
import com.example.fintrac.core.UserFacingException;
import com.example.fintrac.history.HistoryEntries;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Loads everything the History screen lists, merged and de-duplicated. */
public class HistoryRepository {

    private static final String TAG = "HistoryRepository";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public HistoryRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance());
    }

    public HistoryRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    /**
     * Personal history must load; paid bills and jar activity are best effort, so a failure
     * there still shows the rest.
     */
    public Task<List<Transaction>> loadAll() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forException(new UserFacingException("Please log in again."));
        String uid = user.getUid();

        Task<QuerySnapshot> personal = db.collection("users").document(uid).collection("transactions").get();
        Task<QuerySnapshot> bills = db.collection("users").document(uid).collection("bills")
                .whereEqualTo("isPaid", true).get();
        Task<QuerySnapshot> jars = db.collection("transactions").whereEqualTo("userId", uid).get();

        return Tasks.whenAllComplete(personal, bills, jars).continueWith(done -> {
            if (!personal.isSuccessful()) {
                throw personal.getException() != null ? personal.getException()
                        : new UserFacingException("Couldn't load your history.");
            }
            return HistoryEntries.merge(
                    personalEntries(personal.getResult()),
                    bills.isSuccessful() ? billEntries(bills.getResult(), uid) : new ArrayList<>(),
                    jars.isSuccessful() ? jarEntries(jars.getResult(), uid) : new ArrayList<>());
        });
    }

    private static List<Transaction> personalEntries(QuerySnapshot snapshot) {
        List<Transaction> out = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Transaction t = doc.toObject(Transaction.class);
                if (t == null) continue;
                t.setId(doc.getId());
                out.add(t);
            } catch (RuntimeException e) {
                Log.w(TAG, "Skipping unreadable transaction " + doc.getId(), e);
            }
        }
        return out;
    }

    private static List<Transaction> billEntries(QuerySnapshot snapshot, String uid) {
        List<Transaction> out = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Bill bill = new Bill();
            bill.amount = doc.getString("amount");
            bill.amountCents = doc.getLong("amountCents");
            bill.dueDate = doc.getString("dueDate");

            String category = doc.getString("category");
            String frequency = doc.getString("frequency");

            Transaction t = new Transaction();
            t.setId("bill_" + doc.getId());
            t.setTitle("Paid Bill: " + doc.getString("name"));
            t.setDescription((category != null ? category : "Bill") + " • " + (frequency != null ? frequency : "One-time"));
            t.setCategory("Bills");
            t.setAmount(bill.money().toPesos());
            t.setExpense(true);
            t.setUserId(uid);

            Timestamp paidAt = doc.getTimestamp("timestamp");
            Date due = bill.dueDateValue();
            if (paidAt != null) t.setTimestamp(paidAt);
            else if (due != null) t.setDate(due);
            out.add(t);
        }
        return out;
    }

    private static List<Transaction> jarEntries(QuerySnapshot snapshot, String uid) {
        List<Transaction> out = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                JarTransaction jarTx = doc.toObject(JarTransaction.class);
                if (jarTx == null) continue;

                Transaction t = new Transaction();
                t.setId("root_jar_" + doc.getId());
                t.setTitle("Jar Activity: " + (jarTx.getJarName() != null ? jarTx.getJarName() : "Shared Jar"));
                String note = jarTx.getNote();
                t.setDescription(note != null && !note.isEmpty() ? note : "Savings contribution");
                t.setCategory("Jars");
                t.setAmount(jarTx.getAmount());
                String type = jarTx.getType();
                t.setExpense("WITHDRAW".equals(type) || "WITHDRAWAL".equals(type));
                t.setUserId(uid);
                if (jarTx.getTimestamp() != null) t.setTimestamp(jarTx.getTimestamp());
                out.add(t);
            } catch (RuntimeException e) {
                Log.w(TAG, "Skipping unreadable jar activity " + doc.getId(), e);
            }
        }
        return out;
    }
}
