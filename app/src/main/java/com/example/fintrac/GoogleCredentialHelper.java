package com.example.fintrac;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;

import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/**
 * Google sign-in via Credential Manager (replaces the deprecated GoogleSignInClient).
 * Returns a Google ID token to exchange for a Firebase credential.
 */
public final class GoogleCredentialHelper {

    public interface Callback {
        void onIdToken(String idToken);
        void onCancelled();
        void onError(String message);
    }

    private GoogleCredentialHelper() {}

    public static void signIn(Activity activity, Callback callback) {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                activity.getString(R.string.default_web_client_id)).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        CredentialManager.create(activity).getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(credential.getData());
                            callback.onIdToken(google.getIdToken());
                        } else {
                            callback.onError("Unexpected credential type");
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) {
                            callback.onCancelled();
                        } else {
                            callback.onError(e.getMessage());
                        }
                    }
                });
    }

    /** Call on logout so the account picker shows again next time. */
    public static void clearState(Context context) {
        CredentialManager.create(context).clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                ContextCompat.getMainExecutor(context),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(Void result) {}

                    @Override
                    public void onError(ClearCredentialException e) {}
                });
    }
}
