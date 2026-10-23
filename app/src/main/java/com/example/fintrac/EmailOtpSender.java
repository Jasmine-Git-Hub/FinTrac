package com.example.fintrac;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends the OTP email through EmailJS (free tier, no server needed).
 * Only EmailJS's public key ships in the app; no email account password is involved.
 *
 * EmailJS setup: Account > Security > enable "Allow EmailJS API for non-browser applications",
 * and create a template whose "To Email" is {{to_email}} and whose body contains {{otp_code}}.
 */
public final class EmailOtpSender {

    public interface Callback {
        void onSuccess();
        void onFailure(String error);
    }

    private static final String TAG = "EmailOtpSender";
    private static final String ENDPOINT = "https://api.emailjs.com/api/v1.0/email/send";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private EmailOtpSender() {}

    /** Turns a send failure into something the user (or developer) can act on. */
    public static String describeFailure(String error) {
        if (error == null) return "Failed to send the code. Check your internet connection.";
        if (error.contains("not configured")) {
            return "Email codes aren't set up in this build (EmailJS keys missing from local.properties).";
        }
        if (error.startsWith("HTTP 4")) {
            return "The email service rejected the request (" + error + ")";
        }
        return "Failed to send the code. Check your internet connection.";
    }

    /** Callback runs on the main thread. */
    public static void send(String recipientEmail, String otpCode, Callback callback) {
        if (BuildConfig.EMAILJS_PUBLIC_KEY.isEmpty()) {
            MAIN.post(() -> callback.onFailure("EmailJS is not configured"));
            return;
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection conn = null;
            try {
                JSONObject params = new JSONObject()
                        .put("to_email", recipientEmail)
                        .put("otp_code", otpCode)
                        .put("expires_minutes", OtpManager.VALIDITY_MINUTES);
                JSONObject body = new JSONObject()
                        .put("service_id", BuildConfig.EMAILJS_SERVICE_ID)
                        .put("template_id", BuildConfig.EMAILJS_TEMPLATE_ID)
                        .put("user_id", BuildConfig.EMAILJS_PUBLIC_KEY)
                        .put("template_params", params);

                conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    MAIN.post(callback::onSuccess);
                } else {
                    // EmailJS explains failures in plain text, e.g. "The recipients address is empty"
                    String reason = readBody(conn.getErrorStream());
                    Log.w(TAG, "EmailJS returned HTTP " + code + ": " + reason);
                    MAIN.post(() -> callback.onFailure("HTTP " + code + (reason.isEmpty() ? "" : ": " + reason)));
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to send OTP email", e);
                MAIN.post(() -> callback.onFailure(e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String readBody(java.io.InputStream stream) {
        if (stream == null) return "";
        try (java.io.InputStream in = stream) {
            byte[] bytes = new byte[300];
            int n = in.read(bytes);
            return n > 0 ? new String(bytes, 0, n, StandardCharsets.UTF_8).trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
