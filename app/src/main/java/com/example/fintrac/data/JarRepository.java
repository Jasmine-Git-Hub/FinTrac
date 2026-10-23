package com.example.fintrac.data;

import com.example.fintrac.JarFirestoreModel;
import com.example.fintrac.JarTransaction;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.google.android.gms.tasks.Task;

import java.util.List;

/** Jar data access for the Jars list. Screens and ViewModels never touch Firestore directly. */
public interface JarRepository {

    interface Listener {
        void onJars(List<JarFirestoreModel> jars);
        void onError(Exception e);
    }

    /** Live list of non-archived jars the signed-in user participates in, newest first. */
    Subscription observeActiveJars(Listener listener);

    Task<Void> create(JarFirestoreModel jar);

    /** Deletes the jar and its activity records. Owner only (enforced by Firestore rules). */
    Task<Void> delete(String jarId);

    /** uid of the signed-in user, or null. */
    String currentUserId();

    // ---- Single jar (details screen) ----

    /** Live updates for one jar. Delivers an error if the jar is deleted or access is lost. */
    Subscription observeJar(String jarId, DataListener<JarFirestoreModel> listener);

    /** Live activity feed (newest 50) for one jar. */
    Subscription observeActivity(String jarId, DataListener<List<JarTransaction>> listener);

    Task<UserProfile> loadProfile(String uid);

    /**
     * Adds money for the signed-in user. Goal and fair-share limits are checked inside the
     * same Firestore transaction as the write, so two people saving at once can't overshoot.
     * Fails with {@link com.example.fintrac.core.UserFacingException} when a limit is hit.
     *
     * @return true if this deposit reached the jar's goal
     */
    Task<Boolean> addMoney(String jarId, Money amount);

    Task<Void> invite(String jarId, String email);

    Task<Void> removeParticipant(String jarId, String uid);

    Task<Void> archive(String jarId);
}
