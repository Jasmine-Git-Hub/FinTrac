package com.example.fintrac.data;

import android.content.Context;

import com.example.fintrac.JarFirestoreModel;
import com.example.fintrac.core.UserFacingException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Deletes the signed-in user's account and data (Google Play account-deletion requirement).
 *
 * Order matters: data first (the security rules need the user signed in), then the auth
 * account last. If anything fails, the account is left intact so the user can retry.
 */
public final class AccountDeletion {

    private static final String[] USER_SUBCOLLECTIONS =
            {"bills", "challenges", "transactions", "notifications", "jars"};
    private static final String[] LOCAL_PREFS =
            {"OtpSession", "LoginPrefs", "SettingsPrefs", "OtpPrefs"};

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final JarRepository jars;

    public AccountDeletion() {
        this(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance(), new FirestoreJarRepository());
    }

    public AccountDeletion(FirebaseFirestore db, FirebaseAuth auth, JarRepository jars) {
        this.db = db;
        this.auth = auth;
        this.jars = jars;
    }

    /** Whether the user signs in with email + password (vs. Google only). */
    public boolean usesPassword() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return false;
        for (com.google.firebase.auth.UserInfo info : user.getProviderData()) {
            if ("password".equals(info.getProviderId())) return true;
        }
        return false;
    }

    /**
     * @param credential fresh credential for re-authentication (Firebase only allows deleting
     *                   an account that signed in recently)
     */
    public Task<Void> deleteEverything(Context context, AuthCredential credential) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return Tasks.forException(new UserFacingException("Please log in again."));
        String uid = user.getUid();

        return user.reauthenticate(credential)
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) {
                        throw new UserFacingException("That password isn't correct.");
                    }
                    // Activity first: the rules only allow it while still a member of the jar
                    return deleteOwnActivity(uid);
                })
                .continueWithTask(t -> {
                    t.getResult();
                    return removeFromJars(uid);
                })
                .continueWithTask(t -> {
                    t.getResult();
                    List<Task<Void>> deletes = new ArrayList<>();
                    for (String sub : USER_SUBCOLLECTIONS) {
                        deletes.add(deleteCollection(db.collection("users").document(uid).collection(sub).get()));
                    }
                    return Tasks.whenAll(deletes);
                })
                .continueWithTask(t -> {
                    t.getResult();
                    return db.collection("users").document(uid).delete();
                })
                .continueWithTask(t -> {
                    t.getResult();
                    return user.delete();
                })
                .continueWith(t -> {
                    t.getResult();
                    clearLocalData(context);
                    return null;
                });
    }

    /** Owned jars are deleted (with their activity); others' jars are left. */
    private Task<Void> removeFromJars(String uid) {
        return db.collection("jars").whereArrayContains("participants", uid).get()
                .continueWithTask(t -> {
                    List<Task<Void>> ops = new ArrayList<>();
                    for (DocumentSnapshot doc : t.getResult().getDocuments()) {
                        JarFirestoreModel jar = doc.toObject(JarFirestoreModel.class);
                        if (jar != null && uid.equals(jar.getCreatedBy())) {
                            ops.add(jars.delete(doc.getId()));
                        } else {
                            ops.add(jars.removeParticipant(doc.getId(), uid));
                        }
                    }
                    return Tasks.whenAll(ops);
                });
    }

    /** Best effort: the user's own entries in shared-jar activity logs. */
    private Task<Void> deleteOwnActivity(String uid) {
        Task<Void> transactions = deleteCollection(db.collection("transactions").whereEqualTo("userId", uid).get());
        Task<Void> jarTransactions = deleteCollection(db.collection("jarTransactions").whereEqualTo("userId", uid).get());
        // Entries in jars the user just left may no longer be deletable; don't block on them
        return Tasks.whenAllComplete(transactions, jarTransactions).continueWith(t -> null);
    }

    private Task<Void> deleteCollection(Task<QuerySnapshot> query) {
        return query.continueWithTask(t -> {
            List<DocumentReference> refs = new ArrayList<>();
            for (DocumentSnapshot doc : t.getResult().getDocuments()) refs.add(doc.getReference());
            List<Task<Void>> commits = new ArrayList<>();
            for (int i = 0; i < refs.size(); i += 450) { // Firestore batches allow up to 500 writes
                WriteBatch batch = db.batch();
                for (DocumentReference ref : refs.subList(i, Math.min(i + 450, refs.size()))) batch.delete(ref);
                commits.add(batch.commit());
            }
            return Tasks.whenAll(commits);
        });
    }

    private static void clearLocalData(Context context) {
        for (String prefs : LOCAL_PREFS) {
            context.getSharedPreferences(prefs, Context.MODE_PRIVATE).edit().clear().apply();
        }
        deleteRecursively(new File(context.getFilesDir(), "profile"));
        deleteRecursively(new File(context.getCacheDir(), "reports"));
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursively(child);
        }
        file.delete();
    }
}
