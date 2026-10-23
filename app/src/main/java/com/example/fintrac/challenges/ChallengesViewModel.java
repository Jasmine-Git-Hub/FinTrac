package com.example.fintrac.challenges;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fintrac.ChallengeIconHelper;
import com.example.fintrac.ChallengeItem;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Event;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.example.fintrac.data.ChallengeRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Challenges screen: live active challenges, totals, start / check in / archive / delete. */
public class ChallengesViewModel extends ViewModel {

    public static final class State {
        public final boolean loading;
        public final String error;
        public final List<ChallengeItem> active;
        public final Money savedTotal;

        State(boolean loading, String error, List<ChallengeItem> active, Money savedTotal) {
            this.loading = loading;
            this.error = error;
            this.active = Collections.unmodifiableList(active);
            this.savedTotal = savedTotal;
        }
    }

    private final ChallengeRepository repository;
    private Subscription subscription;

    private final MutableLiveData<State> state =
            new MutableLiveData<>(new State(true, null, Collections.emptyList(), Money.ZERO));
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();
    /** A check-in that finished a challenge; the screen celebrates, then calls {@link #archive}. */
    private final MutableLiveData<Event<ChallengeItem>> completed = new MutableLiveData<>();
    /** A normal check-in: the amount saved today (base currency). */
    private final MutableLiveData<Event<Money>> checkedIn = new MutableLiveData<>();

    public ChallengesViewModel() {
        this(new ChallengeRepository());
    }

    public ChallengesViewModel(ChallengeRepository repository) {
        this.repository = repository;
        subscribe();
    }

    public LiveData<State> state() { return state; }
    public LiveData<Event<String>> messages() { return messages; }
    public LiveData<Event<ChallengeItem>> completed() { return completed; }
    public LiveData<Event<Money>> checkedIn() { return checkedIn; }

    public void retry() {
        subscription.cancel();
        subscribe();
    }

    public void start(String name, String description, int days, double target) {
        if (name == null || name.trim().isEmpty() || days <= 0 || target <= 0) {
            messages.setValue(new Event<>("Enter a name, a number of days and a target above zero."));
            return;
        }
        ChallengeItem item = new ChallengeItem(name.trim(), description,
                ChallengeIconHelper.getIcon(name), ChallengeIconHelper.getColor(name), 0, days, target);
        repository.add(item)
                .addOnSuccessListener(v -> messages.setValue(new Event<>("Challenge Started!")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Couldn't start the challenge. Try again.")));
    }

    public void checkIn(ChallengeItem item) {
        if (item.getDocumentId() == null) return;
        repository.checkIn(item.getDocumentId())
                .addOnSuccessListener(updated -> {
                    repository.clearReminders(updated.getDocumentId());
                    if (updated.isFinished()) {
                        completed.setValue(new Event<>(updated));
                    } else {
                        checkedIn.setValue(new Event<>(updated.dailyAmount()));
                    }
                })
                .addOnFailureListener(e -> messages.setValue(new Event<>(userMessage(e, "Check-in failed. Try again."))));
    }

    public void archive(ChallengeItem item) {
        if (item.getDocumentId() == null) return;
        repository.archive(item.getDocumentId())
                .addOnSuccessListener(v -> messages.setValue(new Event<>("Challenge moved to Archives")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Failed to archive the challenge")));
    }

    public void delete(ChallengeItem item) {
        if (item.getDocumentId() == null) return;
        repository.delete(item.getDocumentId())
                .addOnSuccessListener(v -> messages.setValue(new Event<>("Challenge deleted permanently")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Failed to delete the challenge")));
    }

    private void subscribe() {
        subscription = repository.observeActive(new DataListener<List<ChallengeItem>>() {
            @Override
            public void onData(List<ChallengeItem> items) {
                List<ChallengeItem> active = new ArrayList<>();
                Money saved = Money.ZERO;
                for (ChallengeItem item : items) {
                    if (item.isFinished()) {
                        // Finished but never archived (e.g. the app closed during the celebration)
                        repository.archive(item.getDocumentId());
                        continue;
                    }
                    active.add(item);
                    saved = saved.plus(item.savedSoFar());
                }
                state.setValue(new State(false, null, active, saved));
            }

            @Override
            public void onError(Exception e) {
                State current = state.getValue();
                state.setValue(new State(false, "Couldn't load your challenges. Check your connection.",
                        current != null ? current.active : Collections.emptyList(),
                        current != null ? current.savedTotal : Money.ZERO));
            }
        });
    }

    private static String userMessage(Exception e, String fallback) {
        Throwable cause = e;
        while (cause != null && !(cause instanceof UserFacingException)) cause = cause.getCause();
        return cause != null ? cause.getMessage() : fallback;
    }

    @Override
    protected void onCleared() {
        subscription.cancel();
    }
}
