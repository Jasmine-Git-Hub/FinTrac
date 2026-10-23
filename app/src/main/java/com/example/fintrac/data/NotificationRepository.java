package com.example.fintrac.data;

import com.example.fintrac.FirestoreHelper;
import com.example.fintrac.NotificationItem;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** The signed-in user's notification inbox (users/{uid}/notifications). */
public class NotificationRepository {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public NotificationRepository() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance());
    }

    public NotificationRepository(FirebaseFirestore db, FirebaseAuth auth) {
        this.db = db;
        this.auth = auth;
    }

    /** Live inbox, newest first. */
    public Subscription observe(DataListener<List<NotificationItem>> listener) {
        CollectionReference inbox = inbox();
        if (inbox == null) {
            listener.onError(new UserFacingException("Please log in again."));
            return () -> {};
        }
        ListenerRegistration registration = inbox
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) return;
                    List<NotificationItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            NotificationItem item = doc.toObject(NotificationItem.class);
                            if (item == null) continue;
                            item.setId(doc.getId());
                            items.add(item);
                        } catch (RuntimeException ignored) {
                            // Skip malformed notifications rather than breaking the inbox
                        }
                    }
                    listener.onData(items);
                });
        return registration::remove;
    }

    public Task<Void> markRead(Collection<String> ids) {
        CollectionReference inbox = inbox();
        if (inbox == null || ids.isEmpty()) return Tasks.forResult(null);
        WriteBatch batch = db.batch();
        for (String id : ids) {
            batch.update(inbox.document(id), "isRead", true, "read", true);
        }
        return batch.commit();
    }

    public Task<Void> delete(String id) {
        CollectionReference inbox = inbox();
        if (inbox == null) return Tasks.forResult(null);
        return inbox.document(id).delete();
    }

    /**
     * The owner's invite already added this user to the jar; accepting confirms it, logs
     * "Joined the jar" in the jar's activity, and clears the invite.
     */
    public Task<Void> acceptJarInvite(NotificationItem invite) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || invite.getJarId() == null) {
            return Tasks.forException(new UserFacingException("This invitation is no longer valid."));
        }
        return db.collection("jars").document(invite.getJarId()).get()
                .continueWithTask(task -> {
                    DocumentSnapshot jar = task.isSuccessful() ? task.getResult() : null;
                    List<?> participants = jar != null && jar.exists() ? (List<?>) jar.get("participants") : null;
                    if (participants == null || !participants.contains(user.getUid())) {
                        throw new UserFacingException("This invitation is no longer valid.");
                    }
                    String name = user.getDisplayName();
                    if (name == null || name.isEmpty()) {
                        name = user.getEmail() != null ? user.getEmail().split("@")[0] : "User";
                    }
                    FirestoreHelper.getInstance().addJarTransaction(invite.getJarId(), invite.getJarName(),
                            user.getUid(), name, "ADD_PARTICIPANT", 0.0, "Joined the jar");
                    return delete(invite.getId());
                });
    }

    /**
     * The invite already added this user to the jar, so declining must also take them back
     * out (allowed by the rules as "leaving"), not just delete the notification.
     */
    public Task<Void> declineJarInvite(NotificationItem invite) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || invite.getId() == null) return Tasks.forResult(null);
        if (invite.getJarId() == null) return delete(invite.getId());

        return db.collection("jars").document(invite.getJarId())
                .update("participants", FieldValue.arrayRemove(user.getUid()),
                        "participantContributions." + user.getUid(), FieldValue.delete())
                // Jar gone or already left: still clear the invite
                .continueWithTask(task -> delete(invite.getId()));
    }

    private CollectionReference inbox() {
        FirebaseUser user = auth.getCurrentUser();
        return user == null ? null : db.collection("users").document(user.getUid()).collection("notifications");
    }
}
