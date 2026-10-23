package com.example.fintrac.data;

import com.example.fintrac.FirestoreHelper;
import com.example.fintrac.Transaction;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** The signed-in user's personal transactions (users/{uid}/transactions). */
public class TransactionRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public TransactionRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance());
    }

    public TransactionRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    /** Live list of all personal transactions (unsorted). */
    public Subscription observeAll(DataListener<List<Transaction>> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new UserFacingException("Please log in again."));
            return () -> {};
        }
        ListenerRegistration registration = db.collection("users").document(user.getUid())
                .collection("transactions")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) return;
                    List<Transaction> items = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        items.add(fromDocument(doc));
                    }
                    listener.onData(items);
                });
        return registration::remove;
    }

    public Task<Void> add(Transaction transaction) {
        TaskCompletionSource<Void> result = new TaskCompletionSource<>();
        FirestoreHelper.getInstance().addTransaction(transaction, new FirestoreHelper.OnTransactionAddedListener() {
            @Override
            public void onSuccess(Transaction saved) {
                result.setResult(null);
            }

            @Override
            public void onFailure(String error) {
                result.setException(new Exception(error));
            }
        });
        return result.getTask();
    }

    /**
     * Read field by field rather than with toObject(): older entries stored "amount" as text
     * or used "expense" instead of "isExpense", which would make automatic mapping fail.
     */
    static Transaction fromDocument(DocumentSnapshot doc) {
        Transaction t = new Transaction();
        t.setId(doc.getId());
        t.setTitle(doc.getString("title"));
        t.setDescription(doc.getString("description"));
        t.setCategory(doc.getString("category"));
        t.setUserId(doc.getString("userId"));

        Object amount = doc.get("amount");
        if (amount instanceof Number) {
            t.setAmount(((Number) amount).doubleValue());
        } else if (amount instanceof String) {
            t.setAmount(Money.parse((String) amount).toPesos());
        }

        Boolean expense = doc.getBoolean("isExpense");
        if (expense == null) expense = doc.getBoolean("expense");
        t.setExpense(Boolean.TRUE.equals(expense));

        Timestamp timestamp = doc.getTimestamp("timestamp");
        Date date = doc.getDate("date");
        if (timestamp != null) t.setTimestamp(timestamp);
        else if (date != null) t.setDate(date);
        return t;
    }
}
