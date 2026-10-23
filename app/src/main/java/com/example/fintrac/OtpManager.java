package com.example.fintrac;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Issues and checks email OTP codes.
 *
 * Only a SHA-256 hash of the pending code is kept, in memory (never in an Intent or on disk).
 * Codes expire after {@link #VALIDITY_MINUTES} minutes and lock after {@link #MAX_ATTEMPTS}
 * wrong guesses. If the app process dies, the user simply requests a new code.
 *
 * Note: without a server this is a client-side check, so it adds friction rather than
 * true two-factor security; the password is still the real gate.
 */
public final class OtpManager {

    public static final int VALIDITY_MINUTES = 10;
    public static final int MAX_ATTEMPTS = 5;
    public static final long RESEND_COOLDOWN_MS = 60_000;

    public enum Result { OK, WRONG, EXPIRED, LOCKED }

    private static final String PREFS = "OtpSession";
    private static final String KEY_VERIFIED_UID = "verified_uid";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static String pendingEmail;
    private static String pendingHash;
    private static long expiresAt;
    private static int attemptsLeft;
    private static long lastSentAt;

    private OtpManager() {}

    /** Creates a fresh 6-digit code for this email, replacing any pending one. */
    public static synchronized String issue(String email) {
        String code = String.format(Locale.US, "%06d", RANDOM.nextInt(1_000_000));
        pendingEmail = normalize(email);
        pendingHash = hash(pendingEmail, code);
        expiresAt = System.currentTimeMillis() + VALIDITY_MINUTES * 60_000L;
        attemptsLeft = MAX_ATTEMPTS;
        lastSentAt = System.currentTimeMillis();
        return code;
    }

    public static synchronized Result verify(String email, String enteredCode) {
        if (pendingHash == null || !normalize(email).equals(pendingEmail)
                || System.currentTimeMillis() > expiresAt) {
            return Result.EXPIRED;
        }
        if (attemptsLeft <= 0) {
            return Result.LOCKED;
        }
        if (MessageDigest.isEqual(
                pendingHash.getBytes(StandardCharsets.UTF_8),
                hash(pendingEmail, enteredCode.trim()).getBytes(StandardCharsets.UTF_8))) {
            pendingHash = null;
            pendingEmail = null;
            return Result.OK;
        }
        attemptsLeft--;
        return attemptsLeft <= 0 ? Result.LOCKED : Result.WRONG;
    }

    public static synchronized int attemptsLeft() {
        return attemptsLeft;
    }

    public static synchronized long cooldownRemainingMs() {
        return Math.max(0, lastSentAt + RESEND_COOLDOWN_MS - System.currentTimeMillis());
    }

    // ---- Session gate: has this signed-in user passed the OTP since they logged in? ----

    public static void markSessionVerified(Context context, String uid) {
        prefs(context).edit().putString(KEY_VERIFIED_UID, uid).apply();
    }

    public static boolean isSessionVerified(Context context, String uid) {
        return uid != null && uid.equals(prefs(context).getString(KEY_VERIFIED_UID, null));
    }

    public static void clearSession(Context context) {
        prefs(context).edit().remove(KEY_VERIFIED_UID).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private static String hash(String email, String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((email + ":" + code).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
