package com.example.fintrac;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserFirestoreManager {
    private static final String TAG = "UserFirestoreManager";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public UserFirestoreManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public interface UserCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    // Save current user to Firestore
    public void saveCurrentUser(UserCallback<Void> callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", currentUser.getUid());
        userMap.put("email", currentUser.getEmail());
        userMap.put("displayName", currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() :
                currentUser.getEmail().split("@")[0]);
        userMap.put("photoUrl", currentUser.getPhotoUrl() != null ?
                currentUser.getPhotoUrl().toString() : "");

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .set(userMap)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(callback::onFailure);
    }
}
