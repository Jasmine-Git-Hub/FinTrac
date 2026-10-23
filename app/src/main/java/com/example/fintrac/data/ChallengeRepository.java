package com.example.fintrac.data;

import com.example.fintrac.ChallengeIconHelper;
import com.example.fintrac.ChallengeItem;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/** The signed-in user's savings challenges (users/{uid}/challenges). */
public class ChallengeRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ChallengeRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance());
    }

    public ChallengeRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    /** Live list of non-archived challenges (finished ones included, so they can be archived). */
    public Subscription observeActive(DataListener<List<ChallengeItem>> listener) {
        CollectionReference challenges = challenges();
        if (challenges == null) {
            listener.onError(new UserFacingException("Please log in again."));
            return () -> {};
        }
        ListenerRegistration registration = challenges.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshot == null) return;
            List<ChallengeItem> items = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                if (Boolean.TRUE.equals(doc.getBoolean("isArchived"))) continue;
                ChallengeItem item = fromDocument(doc);
                if (item != null) items.add(item);
            }
            listener.onData(items);
        });
        return registration::remove;
    }

    public Task<Void> add(ChallengeItem item) {
        CollectionReference challenges = challenges();
        if (challenges == null) return Tasks.forException(new UserFacingException("Please log in again."));
        return challenges.add(item).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return null;
        });
    }

    /**
     * Records today's check-in. Done as a transaction on the saved values, so double taps or
     * two devices can't count the same day twice. Returns the updated challenge.
     */
    public Task<ChallengeItem> checkIn(String challengeId) {
        CollectionReference challenges = challenges();
        if (challenges == null) return Tasks.forException(new UserFacingException("Please log in again."));
        DocumentReference ref = challenges.document(challengeId);

        return db.runTransaction(t -> {
            DocumentSnapshot doc = t.get(ref);
            ChallengeItem item = doc.exists() ? fromDocument(doc) : null;
            if (item == null || Boolean.TRUE.equals(doc.getBoolean("isArchived"))) {
                throw new UserFacingException("This challenge no longer exists.");
            }
            if (item.isFinished()) {
                throw new UserFacingException("This challenge is already complete.");
            }
            long now = System.currentTimeMillis();
            if (!ChallengeItem.canCheckIn(item.getLastCheckInTime(), now)) {
                throw new UserFacingException("Already checked in today! Come back after 8:00 AM.");
            }

            int newDay = item.getCurrentDay() + 1;
            item.setCurrentDay(newDay);
            item.setLastCheckInTime(now);
            if (item.isFinished()) {
                t.update(ref, "currentDay", newDay, "lastCheckInTime", now, "isCompleted", true);
            } else {
                t.update(ref, "currentDay", newDay, "lastCheckInTime", now);
            }
            return item;
        });
    }

    public Task<Void> archive(String challengeId) {
        CollectionReference challenges = challenges();
        if (challenges == null) return Tasks.forResult(null);
        return challenges.document(challengeId).update("isArchived", true, "archivedAt", Timestamp.now());
    }

    public Task<Void> delete(String challengeId) {
        CollectionReference challenges = challenges();
        if (challenges == null) return Tasks.forResult(null);
        return challenges.document(challengeId).delete();
    }

    /** Marks unread reminder notifications for this challenge as read. Best effort. */
    public void clearReminders(String challengeId) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String id = doc.getString("id");
                        if (id != null && id.contains(challengeId)) {
                            doc.getReference().update("isRead", true, "read", true);
                        }
                    }
                });
    }

    private static ChallengeItem fromDocument(DocumentSnapshot doc) {
        try {
            ChallengeItem item = doc.toObject(ChallengeItem.class);
            if (item == null) return null;
            item.setDocumentId(doc.getId());
            // Stored icon/colour ids are from whatever build wrote them; always re-resolve
            item.setIconResId(ChallengeIconHelper.getIcon(item.getName()));
            item.setColorResId(ChallengeIconHelper.getColor(item.getName()));
            item.updateCalculatedFields();
            return item;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private CollectionReference challenges() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : db.collection("users").document(user.getUid()).collection("challenges");
    }
}
