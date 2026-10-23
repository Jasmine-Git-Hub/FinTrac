package com.example.fintrac.archives;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Event;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.example.fintrac.data.ArchiveRepository;
import com.example.fintrac.data.ArchiveRepository.Entry;
import com.example.fintrac.data.ArchiveRepository.Kind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Archives screen: one tab at a time, live, with the 30-day automatic clean-up. */
public class ArchivesViewModel extends ViewModel {

    public static final class State {
        public final boolean loading;
        public final String error;
        public final Kind kind;
        public final List<Entry> entries;

        State(boolean loading, String error, Kind kind, List<Entry> entries) {
            this.loading = loading;
            this.error = error;
            this.kind = kind;
            this.entries = Collections.unmodifiableList(entries);
        }
    }

    private final ArchiveRepository repository;
    private Subscription subscription = () -> {};
    private Kind kind = Kind.JARS;
    /** Items we've already asked to purge, so a slow delete isn't requested twice. */
    private final Set<String> purging = new HashSet<>();

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();

    public ArchivesViewModel() {
        this(new ArchiveRepository());
    }

    public ArchivesViewModel(ArchiveRepository repository) {
        this.repository = repository;
        show(Kind.JARS);
    }

    public LiveData<State> state() { return state; }
    public LiveData<Event<String>> messages() { return messages; }

    public void show(Kind newKind) {
        kind = newKind;
        subscription.cancel();
        state.setValue(new State(true, null, kind, Collections.emptyList()));
        Kind requested = newKind;
        subscription = repository.observe(newKind, new DataListener<List<Entry>>() {
            @Override
            public void onData(List<Entry> entries) {
                List<Entry> keep = new ArrayList<>();
                for (Entry entry : entries) {
                    if (daysLeft(entry) <= 0) {
                        purgeExpired(entry);
                    } else {
                        keep.add(entry);
                    }
                }
                state.setValue(new State(false, null, requested, keep));
            }

            @Override
            public void onError(Exception e) {
                state.setValue(new State(false, "Couldn't load archives. Check your connection.",
                        requested, Collections.emptyList()));
            }
        });
    }

    public void restore(Entry entry) {
        repository.restore(entry)
                .addOnSuccessListener(v -> messages.setValue(new Event<>(entry.kind == Kind.BILLS
                        ? "Bill restored as unpaid." : "Item restored.")))
                .addOnFailureListener(e -> messages.setValue(new Event<>(e instanceof UserFacingException
                        ? e.getMessage() : "Couldn't restore. Try again.")));
    }

    public void delete(Entry entry) {
        repository.delete(entry)
                .addOnSuccessListener(v -> messages.setValue(new Event<>(entry.kind == Kind.JARS && !entry.ownedByMe
                        ? "Removed from your archives." : "Item deleted permanently.")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Couldn't delete. Try again.")));
    }

    public void deleteAll() {
        State current = state.getValue();
        if (current == null || current.entries.isEmpty()) {
            messages.setValue(new Event<>("No items to delete."));
            return;
        }
        repository.deleteAll(current.entries)
                .addOnSuccessListener(v -> messages.setValue(new Event<>("Archives emptied.")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Some items couldn't be deleted.")));
    }

    /** Whole days until automatic deletion. Items with no archive date never expire. */
    public static long daysLeft(Entry entry) {
        if (entry.archivedAt == null) return ArchiveRepository.RETENTION_DAYS;
        long age = TimeUnit.MILLISECONDS.toDays(new Date().getTime() - entry.archivedAt.getTime());
        return ArchiveRepository.RETENTION_DAYS - age;
    }

    private void purgeExpired(Entry entry) {
        if (purging.add(entry.kind + "/" + entry.id)) {
            repository.delete(entry);
        }
    }

    @Override
    protected void onCleared() {
        subscription.cancel();
    }
}
