package com.example.fintrac.data;

import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Archived jars, bills and challenges, with restore / delete and the 30-day clean-up. */
public class ArchiveRepository {

    public enum Kind { JARS, BILLS, CHALLENGES }

    public static final int RETENTION_DAYS = 30;

    /** One archived item as the Archives screen shows it. */
    public static final class Entry {
        public final Kind kind;
        public final String id;
        public final String title;
        /** When it was archived; null for very old items that never recorded it. */
        public final Date archivedAt;
        /** Jars: whether the signed-in user created it (only owners can delete a shared jar). */
        public final boolean ownedByMe;
        /** Challenges: all days done, so there's nothing left to restore it to. */
        public final boolean finished;

        Entry(Kind kind, String id, String title, Date archivedAt, boolean ownedByMe, boolean finished) {
            this.kind = kind;
            this.id = id;
            this.title = title;
            this.archivedAt = archivedAt;
            this.ownedByMe = ownedByMe;
            this.finished = finished;
        }
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final JarRepository jars;

    public ArchiveRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance(), new FirestoreJarRepository());
    }

    public ArchiveRepository(FirebaseFirestore db, FirebaseAuth auth, JarRepository jars) {
        this.db = db;
        this.auth = auth;
        this.jars = jars;
    }

    public Subscription observe(Kind kind, DataListener<List<Entry>> listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new UserFacingException("Please log in again."));
            return () -> {};
        }
        String uid = user.getUid();
        ListenerRegistration registration = query(kind, uid).addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshot == null) return;
            List<Entry> entries = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                if (!Boolean.TRUE.equals(doc.getBoolean("isArchived"))) continue;
                Timestamp archivedAt = doc.getTimestamp("archivedAt");
                String name = doc.getString("name");
                Long current = doc.getLong("currentDay");
                Long total = doc.getLong("totalDays");
                entries.add(new Entry(kind, doc.getId(),
                        name != null && !name.isEmpty() ? name : "Unnamed Item",
                        archivedAt != null ? archivedAt.toDate() : null,
                        kind != Kind.JARS || uid.equals(doc.getString("createdBy")),
                        current != null && total != null && total > 0 && current >= total));
            }
            listener.onData(entries);
        });
        return registration::remove;
    }

    /** Brings an item back. Bills come back unpaid (archiving marks them paid). */
    public Task<Void> restore(Entry entry) {
        switch (entry.kind) {
            case JARS:
                return db.collection("jars").document(entry.id).update("isArchived", false);
            case BILLS:
                return userDoc("bills", entry.id).continueWithTask(ref -> ref.getResult()
                        .update("isArchived", false, "isPaid", false));
            case CHALLENGES:
            default:
                if (entry.finished) {
                    return Tasks.forException(new UserFacingException("This challenge is already complete."));
                }
                return userDoc("challenges", entry.id).continueWithTask(ref -> ref.getResult()
                        .update("isArchived", false));
        }
    }

    /**
     * Permanently removes the item for this user. A shared jar is deleted by its owner;
     * anyone else simply leaves it, which removes it from their archives only.
     */
    public Task<Void> delete(Entry entry) {
        switch (entry.kind) {
            case JARS:
                if (entry.ownedByMe) return jars.delete(entry.id);
                String uid = jars.currentUserId();
                return uid == null ? Tasks.forResult(null) : jars.removeParticipant(entry.id, uid);
            case BILLS:
                return userDoc("bills", entry.id).continueWithTask(ref -> ref.getResult().delete());
            case CHALLENGES:
            default:
                return userDoc("challenges", entry.id).continueWithTask(ref -> ref.getResult().delete());
        }
    }

    public Task<Void> deleteAll(List<Entry> entries) {
        List<Task<Void>> tasks = new ArrayList<>();
        for (Entry entry : entries) tasks.add(delete(entry));
        return Tasks.whenAll(tasks);
    }

    private Query query(Kind kind, String uid) {
        switch (kind) {
            case JARS:
                // Must filter by membership: the security rules refuse unscoped jar queries.
                // Archived ones are picked out in observe() (combining both filters here would
                // need a composite index).
                return db.collection("jars").whereArrayContains("participants", uid);
            case BILLS:
                return db.collection("users").document(uid).collection("bills").whereEqualTo("isArchived", true);
            case CHALLENGES:
            default:
                return db.collection("users").document(uid).collection("challenges").whereEqualTo("isArchived", true);
        }
    }

    private Task<com.google.firebase.firestore.DocumentReference> userDoc(String collection, String id) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forException(new UserFacingException("Please log in again."));
        return Tasks.forResult(db.collection("users").document(user.getUid()).collection(collection).document(id));
    }
}
