package com.example.fintrac.jars;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;

import com.example.fintrac.JarFirestoreModel;
import com.example.fintrac.JarTransaction;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Event;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.example.fintrac.data.FirestoreJarRepository;
import com.example.fintrac.data.JarLimitException;
import com.example.fintrac.data.JarRepository;
import com.example.fintrac.data.UserProfile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One jar's details: the live jar document, its activity feed, participant profiles
 * (each fetched once and cached), and add-money / invite / remove / archive actions.
 */
public class JarDetailsViewModel extends ViewModel {

    /** Result of a refused deposit, for the screen to phrase in the user's currency. */
    public static final class LimitMessage {
        public final JarLimitException.Kind kind;
        public final Money amount;

        LimitMessage(JarLimitException.Kind kind, Money amount) {
            this.kind = kind;
            this.amount = amount;
        }
    }

    private final JarRepository repository;
    private final String jarId;
    private final Subscription jarSubscription;
    private final Subscription activitySubscription;

    private final MutableLiveData<JarFirestoreModel> jar = new MutableLiveData<>();
    private final MutableLiveData<List<JarTransaction>> activity = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Map<String, UserProfile>> profiles = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();
    private final MutableLiveData<Event<LimitMessage>> limitMessages = new MutableLiveData<>();
    private final MutableLiveData<Event<Money>> deposited = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> goalReached = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> closeScreen = new MutableLiveData<>();

    private final Set<String> requestedProfiles = new HashSet<>();

    public static ViewModelProvider.Factory factory(String jarId) {
        return new ViewModelProvider.Factory() {
            @SuppressWarnings("unchecked")
            @Override
            public <T extends ViewModel> T create(Class<T> modelClass, CreationExtras extras) {
                return (T) new JarDetailsViewModel(new FirestoreJarRepository(), jarId);
            }
        };
    }

    public JarDetailsViewModel(JarRepository repository, String jarId) {
        this.repository = repository;
        this.jarId = jarId;

        jarSubscription = repository.observeJar(jarId, new DataListener<JarFirestoreModel>() {
            @Override
            public void onData(JarFirestoreModel data) {
                jar.setValue(data);
                requestProfiles(data.getParticipants());
            }

            @Override
            public void onError(Exception e) {
                // Deleted, archived by someone else, or we were removed from it
                messages.setValue(new Event<>(e instanceof UserFacingException
                        ? e.getMessage() : "You no longer have access to this jar."));
                closeScreen.setValue(new Event<>(true));
            }
        });

        activitySubscription = repository.observeActivity(jarId, new DataListener<List<JarTransaction>>() {
            @Override
            public void onData(List<JarTransaction> data) {
                activity.setValue(data);
                Set<String> uids = new HashSet<>();
                for (JarTransaction item : data) {
                    if (item.getUserId() != null) uids.add(item.getUserId());
                }
                requestProfiles(uids);
            }

            @Override
            public void onError(Exception e) {
                // The feed is secondary; keep showing the jar
                activity.setValue(Collections.emptyList());
            }
        });
    }

    public LiveData<JarFirestoreModel> jar() { return jar; }
    public LiveData<List<JarTransaction>> activity() { return activity; }
    public LiveData<Map<String, UserProfile>> profiles() { return profiles; }
    public LiveData<Event<String>> messages() { return messages; }
    public LiveData<Event<LimitMessage>> limitMessages() { return limitMessages; }
    public LiveData<Event<Money>> deposited() { return deposited; }
    public LiveData<Event<Boolean>> goalReached() { return goalReached; }
    public LiveData<Event<Boolean>> closeScreen() { return closeScreen; }

    public String currentUserId() {
        return repository.currentUserId();
    }

    public boolean isOwner() {
        JarFirestoreModel current = jar.getValue();
        String uid = repository.currentUserId();
        return current != null && uid != null && uid.equals(current.getCreatedBy());
    }

    /** Fair share per person for Shared jars (base PHP), or ZERO if not applicable. */
    public Money suggestedShare() {
        JarFirestoreModel current = jar.getValue();
        if (current == null || current.getParticipants().isEmpty()) return Money.ZERO;
        return Money.ofCents(Money.ofPesos(current.getGoalAmount()).cents() / current.getParticipants().size());
    }

    /** @param amountInBase already converted to base PHP */
    public void addMoney(Money amountInBase) {
        repository.addMoney(jarId, amountInBase)
                .addOnSuccessListener(reached -> {
                    deposited.setValue(new Event<>(amountInBase));
                    if (Boolean.TRUE.equals(reached)) goalReached.setValue(new Event<>(true));
                })
                .addOnFailureListener(e -> {
                    Throwable cause = rootCause(e);
                    if (cause instanceof JarLimitException) {
                        JarLimitException limit = (JarLimitException) cause;
                        limitMessages.setValue(new Event<>(new LimitMessage(limit.kind, limit.remaining)));
                    } else if (cause instanceof UserFacingException) {
                        messages.setValue(new Event<>(cause.getMessage()));
                    } else {
                        messages.setValue(new Event<>("Couldn't add money. Check your connection and try again."));
                    }
                });
    }

    public void invite(String email) {
        repository.invite(jarId, email)
                .addOnSuccessListener(v -> messages.setValue(
                        new Event<>("If " + email + " has a FinTrac account, they've been invited.")))
                .addOnFailureListener(e -> messages.setValue(new Event<>(
                        e.getMessage() != null ? e.getMessage() : "Couldn't send the invite.")));
    }

    public void removeParticipant(String uid, String name) {
        if (!isOwner()) return;
        repository.removeParticipant(jarId, uid)
                .addOnSuccessListener(v -> messages.setValue(new Event<>(name + " removed from jar")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Failed to remove " + name)));
    }

    /** After the "Goal reached" dialog: move the jar to Archives and leave the screen. */
    public void archiveAfterGoal() {
        repository.archive(jarId)
                .addOnSuccessListener(v -> messages.setValue(new Event<>("Goal Reached! Moved to Archives.")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Failed to archive jar")))
                .addOnCompleteListener(t -> closeScreen.setValue(new Event<>(true)));
    }

    private void requestProfiles(Iterable<String> uids) {
        for (String uid : uids) {
            if (!requestedProfiles.add(uid)) continue;
            repository.loadProfile(uid).addOnSuccessListener(profile -> {
                Map<String, UserProfile> updated = new HashMap<>(profiles.getValue());
                updated.put(uid, profile);
                profiles.setValue(updated);
            });
        }
    }

    private static Throwable rootCause(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null && current.getCause() != current
                && !(current instanceof JarLimitException) && !(current instanceof UserFacingException)) {
            current = current.getCause();
        }
        return current;
    }

    @Override
    protected void onCleared() {
        jarSubscription.cancel();
        activitySubscription.cancel();
    }
}
