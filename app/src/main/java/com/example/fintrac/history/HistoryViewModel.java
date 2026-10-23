package com.example.fintrac.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fintrac.Transaction;
import com.example.fintrac.core.Money;
import com.example.fintrac.data.HistoryRepository;

import java.util.Collections;
import java.util.List;

/** History screen: loads the merged history, applies the chip filter, and computes the period cards. */
public class HistoryViewModel extends ViewModel {

    /** Immutable snapshot of what the screen shows. */
    public static final class State {
        public final boolean loading;
        public final String error;
        /** Everything (unfiltered); reports are built from this. */
        public final List<Transaction> all;
        public final List<Transaction> visible;
        public final Money weekNet, monthNet, yearNet;

        State(boolean loading, String error, List<Transaction> all, List<Transaction> visible,
              Money weekNet, Money monthNet, Money yearNet) {
            this.loading = loading;
            this.error = error;
            this.all = Collections.unmodifiableList(all);
            this.visible = Collections.unmodifiableList(visible);
            this.weekNet = weekNet;
            this.monthNet = monthNet;
            this.yearNet = yearNet;
        }

        public boolean isEmpty() {
            return !loading && error == null && visible.isEmpty();
        }
    }

    private final HistoryRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>();

    private List<Transaction> all = Collections.emptyList();
    private HistoryEntries.Filter filter = HistoryEntries.Filter.ALL;
    private boolean loading;
    private String error;
    private boolean inFlight;

    public HistoryViewModel() {
        this(new HistoryRepository());
    }

    public HistoryViewModel(HistoryRepository repository) {
        this.repository = repository;
        refresh();
    }

    public LiveData<State> state() {
        return state;
    }

    /** Reloads from Firestore (served from the offline cache when there's no connection). */
    public void refresh() {
        if (inFlight) return;
        inFlight = true;
        loading = all.isEmpty();
        publish();

        repository.loadAll()
                .addOnSuccessListener(items -> {
                    all = items;
                    error = null;
                })
                .addOnFailureListener(e -> {
                    if (all.isEmpty()) error = "Couldn't load your history. Check your connection.";
                })
                .addOnCompleteListener(t -> {
                    inFlight = false;
                    loading = false;
                    publish();
                });
    }

    public void setFilter(HistoryEntries.Filter newFilter) {
        filter = newFilter;
        publish();
    }

    private void publish() {
        state.setValue(new State(loading, error, all, HistoryEntries.filter(all, filter),
                HistoryEntries.netSince(all, HistoryEntries.startOfDaysAgo(6)),
                HistoryEntries.netSince(all, HistoryEntries.startOfMonth()),
                HistoryEntries.netSince(all, HistoryEntries.startOfYear())));
    }
}
