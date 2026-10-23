package com.example.fintrac.data;

import com.google.firebase.firestore.DocumentSnapshot;

/** The public bits of another user's profile shown in shared jars. */
public final class UserProfile {

    public final String uid;
    public final String displayName;
    /** Local content:// URI of a custom photo (only resolvable on the owner's device). */
    public final String avatarUri;
    /** Name of a built-in avatar drawable, e.g. "avatar_3". */
    public final String avatarName;

    public UserProfile(String uid, String displayName, String avatarUri, String avatarName) {
        this.uid = uid;
        this.displayName = displayName;
        this.avatarUri = avatarUri;
        this.avatarName = avatarName;
    }

    static UserProfile from(DocumentSnapshot doc) {
        return new UserProfile(doc.getId(), displayNameOf(doc),
                doc.getString("profileImageLocalUri"), doc.getString("profileAvatar"));
    }

    static UserProfile unknown(String uid) {
        return new UserProfile(uid, "User", null, null);
    }

    static String displayNameOf(DocumentSnapshot doc) {
        for (String field : new String[]{"fullName", "userName", "displayName", "name"}) {
            String value = doc.getString(field);
            if (value != null && !value.trim().isEmpty()) return value;
        }
        String email = doc.getString("email");
        return email != null && email.contains("@") ? email.split("@")[0] : "User";
    }
}
