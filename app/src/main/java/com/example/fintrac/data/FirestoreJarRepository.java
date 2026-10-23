package com.example.fintrac.data;

import android.util.Log;

import com.example.fintrac.FirestoreHelper;
import com.example.fintrac.JarFirestoreManager;
import com.example.fintrac.Transaction;
import com.example.fintrac.JarFirestoreModel;
import com.example.fintrac.JarTransaction;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/** Jars stored in the top-level "jars" collection, shared via the "participants" array. */
public class FirestoreJarRepository implements JarRepository {

    private static final String TAG = "JarRepository";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final JarFirestoreManager manager;

    public FirestoreJarRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance(), new JarFirestoreManager());
    }

    public FirestoreJarRepository(FirebaseFirestore db, FirebaseAuth auth, JarFirestoreManager manager) {
        this.db = db;
        this.auth = auth;
        this.manager = manager;
    }

    @Override
    public Subscription observeActiveJars(Listener listener) {
        String uid = currentUserId();
        if (uid == null) {
            listener.onError(new IllegalStateException("Not signed in"));
            return () -> {};
        }

        ListenerRegistration registration = db.collection("jars")
                .whereArrayContains("participants", uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) return;

                    List<JarFirestoreModel> jars = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        if (Boolean.TRUE.equals(doc.getBoolean("isArchived"))) continue;
                        try {
                            JarFirestoreModel jar = doc.toObject(JarFirestoreModel.class);
                            if (jar == null) continue;
                            if (jar.getId().isEmpty()) jar.setId(doc.getId());
                            jars.add(jar);
                        } catch (RuntimeException e) {
                            Log.w(TAG, "Skipping unreadable jar " + doc.getId(), e);
                        }
                    }
                    jars.sort((a, b) -> {
                        if (a.getCreatedAt() == null) return b.getCreatedAt() == null ? 0 : 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });
                    listener.onJars(jars);
                });
        return registration::remove;
    }

    @Override
    public Task<Void> create(JarFirestoreModel jar) {
        // JarFirestoreManager also writes the "Created jar" activity record
        TaskCompletionSource<Void> result = new TaskCompletionSource<>();
        manager.createJar(jar, new JarFirestoreManager.FirestoreCallback<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference ref) {
                result.setResult(null);
            }

            @Override
            public void onFailure(Exception e) {
                result.setException(e);
            }
        });
        return result.getTask();
    }

    @Override
    public Task<Void> delete(String jarId) {
        if (currentUserId() == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }
        DocumentReference jarRef = db.collection("jars").document(jarId);

        // Activity records must go first: the rules check jar membership, which needs the jar
        return db.collection("transactions").whereEqualTo("jarId", jarId).get()
                .continueWithTask(task -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.delete(jarRef);
                    return batch.commit();
                });
    }

    @Override
    public String currentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // ---- Single jar ----

    @Override
    public Subscription observeJar(String jarId, DataListener<JarFirestoreModel> listener) {
        ListenerRegistration registration = db.collection("jars").document(jarId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (doc == null || !doc.exists()) {
                        listener.onError(new UserFacingException("This jar no longer exists."));
                        return;
                    }
                    JarFirestoreModel jar = doc.toObject(JarFirestoreModel.class);
                    if (jar == null) return;
                    if (jar.getId().isEmpty()) jar.setId(doc.getId());
                    listener.onData(jar);
                });
        return registration::remove;
    }

    @Override
    public Subscription observeActivity(String jarId, DataListener<List<JarTransaction>> listener) {
        ListenerRegistration registration = db.collection("transactions")
                .whereEqualTo("jarId", jarId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) return;
                    List<JarTransaction> items = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        JarTransaction item = doc.toObject(JarTransaction.class);
                        if (item != null) items.add(item);
                    }
                    listener.onData(items);
                });
        return registration::remove;
    }

    @Override
    public Task<UserProfile> loadProfile(String uid) {
        return db.collection("users").document(uid).get()
                .continueWith(task -> {
                    DocumentSnapshot doc = task.isSuccessful() ? task.getResult() : null;
                    return doc != null && doc.exists() ? UserProfile.from(doc) : UserProfile.unknown(uid);
                });
    }

    @Override
    public Task<Boolean> addMoney(String jarId, Money amount) {
        String uid = currentUserId();
        if (uid == null) return Tasks.forException(new UserFacingException("Please log in again."));
        if (!amount.isPositive()) return Tasks.forException(new UserFacingException("Enter an amount greater than zero."));

        DocumentReference jarRef = db.collection("jars").document(jarId);
        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference activityRef = db.collection("transactions").document();
        String[] jarNameHolder = {"Shared Jar"};

        Task<Boolean> deposit = db.runTransaction(t -> {
            DocumentSnapshot jarDoc = t.get(jarRef);
            DocumentSnapshot userDoc = t.get(userRef);
            JarFirestoreModel jar = jarDoc.exists() ? jarDoc.toObject(JarFirestoreModel.class) : null;
            if (jar == null) throw new UserFacingException("This jar no longer exists.");
            if (!jar.getParticipants().contains(uid)) throw new UserFacingException("You're no longer part of this jar.");

            Money saved = Money.ofPesos(jar.getSavedAmount());
            Money goal = Money.ofPesos(jar.getGoalAmount());
            Double mineRaw = jar.getParticipantContributions().get(uid);
            Money mine = Money.ofPesos(mineRaw != null ? mineRaw : 0.0);

            if (!goal.isGreaterThan(saved)) {
                throw new JarLimitException(JarLimitException.Kind.GOAL_ALREADY_REACHED, Money.ZERO);
            }
            if (saved.plus(amount).isGreaterThan(goal)) {
                throw new JarLimitException(JarLimitException.Kind.OVER_GOAL, goal.minus(saved));
            }

            int people = jar.getParticipants().size();
            if ("Shared".equalsIgnoreCase(jar.getType()) && people > 0) {
                Money share = Money.ofCents(goal.cents() / people);
                if (!share.isGreaterThan(mine)) {
                    throw new JarLimitException(JarLimitException.Kind.SHARE_ALREADY_PAID, share);
                }
                if (mine.plus(amount).isGreaterThan(share)) {
                    throw new JarLimitException(JarLimitException.Kind.OVER_SHARE, share.minus(mine));
                }
            }

            Money newSaved = saved.plus(amount);
            // Field-path update touches only this user's entry (required by the security rules)
            t.update(jarRef,
                    "savedAmount", newSaved.toPesos(),
                    "participantContributions." + uid, mine.plus(amount).toPesos());

            String name = userDoc.exists() ? UserProfile.displayNameOf(userDoc) : "User";
            JarTransaction activity = new JarTransaction(jarId, uid, name, amount.toPesos(),
                    "ADD_MONEY", "Added money to jar");
            activity.setId(activityRef.getId());
            activity.setJarName(jar.getName());
            activity.setTimestamp(Timestamp.now());
            t.set(activityRef, activity);

            jarNameHolder[0] = jar.getName();
            return !goal.isGreaterThan(newSaved);
        });

        // Mirror the deposit in the user's personal history (best effort, as before)
        deposit.addOnSuccessListener(reached -> FirestoreHelper.getInstance().addTransaction(
                new Transaction("Saved to Jar: " + jarNameHolder[0], "Auto-recorded jar savings",
                        "Savings", amount.toPesos(), true, uid),
                new FirestoreHelper.OnTransactionAddedListener() {
                    @Override
                    public void onSuccess(Transaction transaction) {}

                    @Override
                    public void onFailure(String error) {
                        Log.w(TAG, "Failed to auto-record jar savings: " + error);
                    }
                }));
        return deposit;
    }

    @Override
    public Task<Void> invite(String jarId, String email) {
        TaskCompletionSource<Void> result = new TaskCompletionSource<>();
        manager.addParticipant(jarId, email, new JarFirestoreManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                result.setResult(null);
            }

            @Override
            public void onFailure(Exception e) {
                result.setException(e);
            }
        });
        return result.getTask();
    }

    @Override
    public Task<Void> removeParticipant(String jarId, String uid) {
        return db.collection("jars").document(jarId).update(
                "participants", FieldValue.arrayRemove(uid),
                "participantContributions." + uid, FieldValue.delete());
    }

    @Override
    public Task<Void> archive(String jarId) {
        return db.collection("jars").document(jarId).update(
                "isArchived", true,
                "archivedAt", FieldValue.serverTimestamp());
    }
}
