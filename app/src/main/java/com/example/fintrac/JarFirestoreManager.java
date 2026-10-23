package com.example.fintrac;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JarFirestoreManager {
    private static final String TAG = "JarFirestoreManager";
    private static final String COLLECTION_JARS = "jars";
    private static final String COLLECTION_TRANSACTIONS = "transactions";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;
    private final CollectionReference jarsCollection;
    private final CollectionReference transactionsCollection;
    private final CollectionReference usersCollection;

    public JarFirestoreManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        jarsCollection = db.collection(COLLECTION_JARS);
        transactionsCollection = db.collection(COLLECTION_TRANSACTIONS);
        usersCollection = db.collection(COLLECTION_USERS);
    }

    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    // Create new jar - Properly handles user ID
    // In createJar method, update the jar creation part:

    // Create new jar - Properly handles user ID
    public void createJar(JarFirestoreModel jarModel, FirestoreCallback<DocumentReference> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        String userId = currentUser.getUid();

        // Generate ID if not set
        if (jarModel.getId() == null || jarModel.getId().isEmpty()) {
            jarModel.setId(jarsCollection.document().getId());
        }

        // Set createdBy
        jarModel.setCreatedBy(userId);
        jarModel.setCreatedAt(com.google.firebase.Timestamp.now());

        // Initialize participants list
        List<String> participants = jarModel.getParticipants();
        if (participants == null || participants.isEmpty()) {
            participants = new ArrayList<>();
            participants.add(userId);
            jarModel.setParticipants(participants);
        } else if (!participants.contains(userId)) {
            participants.add(userId);
        }

        // Initialize contributions
        Map<String, Double> contributions = jarModel.getParticipantContributions();
        if (contributions == null || contributions.isEmpty()) {
            contributions = new HashMap<>();
            contributions.put(userId, jarModel.getSavedAmount());
            jarModel.setParticipantContributions(contributions);
        } else if (!contributions.containsKey(userId)) {
            contributions.put(userId, jarModel.getSavedAmount());
        }

        // Initialize transactions list
        if (jarModel.getTransactions() == null) {
            jarModel.setTransactions(new ArrayList<>());
        }

        // REMOVED: The daysLeft setting - now calculated dynamically

        DocumentReference jarRef = jarsCollection.document(jarModel.getId());

        jarRef.set(jarModel)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Jar created with ID: " + jarModel.getId());

                    // Get user's full name from Firestore
                    usersCollection.document(userId).get()
                            .addOnSuccessListener(userDoc -> {
                                String userName = getUserDisplayName(userDoc, currentUser);

                                // Create transaction record with proper name
                                JarTransaction transaction = new JarTransaction(
                                        jarModel.getId(),
                                        userId,
                                        userName,
                                        jarModel.getSavedAmount(),
                                        "CREATE",
                                        "Created jar: " + jarModel.getName()
                                );
                                transaction.setJarName(jarModel.getName());

                                // Add transaction to separate collection
                                addTransactionToCollection(jarModel.getId(), jarModel.getName(), transaction);
                            })
                            .addOnFailureListener(e -> {
                                // Fallback to basic name
                                JarTransaction transaction = new JarTransaction(
                                        jarModel.getId(),
                                        userId,
                                        getUserName(currentUser),
                                        jarModel.getSavedAmount(),
                                        "CREATE",
                                        "Created jar: " + jarModel.getName()
                                );
                                transaction.setJarName(jarModel.getName());
                                addTransactionToCollection(jarModel.getId(), jarModel.getName(), transaction);
                            });

                    callback.onSuccess(jarRef);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating jar", e);
                    callback.onFailure(e);
                });
    }

    // Helper method to get user display name from Firestore document
    private String getUserDisplayName(DocumentSnapshot userDoc, FirebaseUser firebaseUser) {
        if (userDoc.exists()) {
            String fullName = userDoc.getString("fullName");
            if (fullName != null && !fullName.trim().isEmpty()) {
                return fullName;
            }

            String userName = userDoc.getString("userName");
            if (userName != null && !userName.trim().isEmpty()) {
                return userName;
            }

            String displayName = userDoc.getString("displayName");
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName;
            }

            String name = userDoc.getString("name");
            if (name != null && !name.trim().isEmpty()) {
                return name;
            }
        }

        return getUserName(firebaseUser);
    }

    // Add transaction to separate collection
    private void addTransactionToCollection(String jarId, String jarName, JarTransaction transaction) {
        if (transaction.getId() == null || transaction.getId().isEmpty()) {
            transaction.setId(transactionsCollection.document().getId());
        }

        transaction.setJarId(jarId);
        transaction.setJarName(jarName);
        transaction.setTimestamp(com.google.firebase.Timestamp.now());

        transactionsCollection.document(transaction.getId())
                .set(transaction)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Transaction added to collection: " + transaction.getId() +
                                " with userName: " + transaction.getUserName())
                )
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error adding transaction to collection", e)
                );
    }

    /**
     * Invite a user to a shared jar (owner only, enforced by Firestore rules).
     * The participant, their contribution entry, their invite notification and the activity
     * record are written in one batch so a partial failure can't leave the jar half-updated.
     * "No such account" reports success like a real invite, so the screen can't be used to
     * check who has a FinTrac account.
     */
    public void addParticipant(String jarId, String participantEmail,
                               FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        DocumentReference jarRef = jarsCollection.document(jarId);

        jarRef.get().continueWithTask(jarTask -> {
            DocumentSnapshot jarDoc = jarTask.getResult();
            if (jarDoc == null || !currentUser.getUid().equals(jarDoc.getString("createdBy"))) {
                throw new Exception("Only the jar owner can invite people.");
            }
            return usersCollection.whereEqualTo("email", participantEmail).limit(1).get()
                    .continueWithTask(userTask -> {
                        if (userTask.getResult() == null || userTask.getResult().isEmpty()) {
                            return com.google.android.gms.tasks.Tasks.forResult(null);
                        }
                        DocumentSnapshot invitee = userTask.getResult().getDocuments().get(0);
                        String inviteeId = invitee.getId();
                        List<?> participants = (List<?>) jarDoc.get("participants");
                        if (inviteeId.equals(currentUser.getUid())
                                || (participants != null && participants.contains(inviteeId))) {
                            throw new Exception("This person is already in the jar.");
                        }
                        return usersCollection.document(currentUser.getUid()).get()
                                .continueWithTask(meTask -> writeInvite(jarRef, jarDoc, invitee,
                                        getUserDisplayName(meTask.getResult(), currentUser),
                                        participantEmail, currentUser.getUid()));
                    });
        }).addOnSuccessListener(ignored -> callback.onSuccess(null))
          .addOnFailureListener(e -> {
              Log.e(TAG, "Error inviting participant", e);
              callback.onFailure(e);
          });
    }

    private com.google.android.gms.tasks.Task<Void> writeInvite(DocumentReference jarRef, DocumentSnapshot jarDoc,
                                                               DocumentSnapshot invitee, String inviterName,
                                                               String inviteeEmail, String inviterId) {
        String jarId = jarRef.getId();
        String jarName = jarDoc.getString("name") != null ? jarDoc.getString("name") : "Shared Jar";
        String inviteeName = getUserDisplayName(invitee, null);
        if (inviteeName == null || inviteeName.isEmpty()) {
            inviteeName = inviteeEmail.split("@")[0];
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "JAR_INVITE");
        notification.put("title", "Jar Invitation");
        notification.put("message", inviterName + " invited you to join '" + jarName + "'");
        notification.put("jarId", jarId);
        notification.put("jarName", jarName);
        notification.put("inviterId", inviterId);
        notification.put("inviterName", inviterName);
        notification.put("inviteeName", inviteeName);
        notification.put("isRead", false);
        notification.put("timestamp", new Date());
        notification.put("actionType", "ACCEPT/DECLINE");

        DocumentReference txRef = transactionsCollection.document();
        JarTransaction activity = new JarTransaction(jarId, inviterId, inviterName, 0.0,
                "ADD_PARTICIPANT", "invited " + inviteeName + " to the jar");
        activity.setId(txRef.getId());
        activity.setJarName(jarName);
        activity.setTimestamp(com.google.firebase.Timestamp.now());

        WriteBatch batch = db.batch();
        batch.update(jarRef,
                "participants", FieldValue.arrayUnion(invitee.getId()),
                "participantContributions." + invitee.getId(), 0.0);
        batch.set(usersCollection.document(invitee.getId()).collection("notifications").document(), notification);
        batch.set(txRef, activity);
        return batch.commit();
    }

    private String getUserName(FirebaseUser user) {
        if (user == null) return "User";

        if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        }
        String email = user.getEmail();
        if (email != null) {
            return email.split("@")[0];
        }
        return "User";
    }

}