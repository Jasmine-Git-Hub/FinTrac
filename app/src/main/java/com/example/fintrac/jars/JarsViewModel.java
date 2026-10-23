package com.example.fintrac.jars;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fintrac.JarFirestoreModel;
import com.example.fintrac.JarItem;
import com.example.fintrac.core.Event;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.data.FirestoreJarRepository;
import com.example.fintrac.data.JarRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Jars screen logic: live jar list, type filter, totals, the "fund your Mandatory jar first"
 * lock, and create/delete rules.
 */
public class JarsViewModel extends ViewModel {

    public static final String ALL = "All";

    private final JarRepository repository;
    private Subscription subscription;

    private final MutableLiveData<JarsUiState> state = new MutableLiveData<>(JarsUiState.initial());
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();

    private List<JarFirestoreModel> allJars = Collections.emptyList();
    private String filter = ALL;
    private boolean loading = true;
    private String error;

    public JarsViewModel() {
        this(new FirestoreJarRepository());
    }

    public JarsViewModel(JarRepository repository) {
        this.repository = repository;
        subscribe();
    }

    public LiveData<JarsUiState> state() {
        return state;
    }

    public LiveData<Event<String>> messages() {
        return messages;
    }

    public void setFilter(String newFilter) {
        filter = newFilter == null ? ALL : newFilter;
        publish();
    }

    /** Pull-to-refresh / Retry: re-attach the live listener. */
    public void retry() {
        subscription.cancel();
        loading = true;
        error = null;
        publish();
        subscribe();
    }

    /**
     * @param targetInBase goal already converted to base currency (PHP); null if not a number
     * @return an error message for the form, or null if creation started
     */
    @Nullable
    public String create(String name, @Nullable Money targetInBase, String type,
                         String iconName, int color, String deadline) {
        String uid = repository.currentUserId();
        if (uid == null) return "Please log in first";
        if (name == null || name.trim().isEmpty()) return "Enter a jar name";
        if (targetInBase == null || !targetInBase.isPositive()) return "Enter a goal greater than zero";

        JarFirestoreModel jar = new JarFirestoreModel(null, name.trim(),
                type != null ? type : "Flexible", 0.0, targetInBase.toPesos(),
                deadline, "", iconName, color, uid);
        repository.create(jar)
                .addOnSuccessListener(v -> message("Jar created successfully!"))
                .addOnFailureListener(e -> message("Failed to create jar"));
        return null;
    }

    /** The owner archives a jar (for everyone); other participants can only leave it. */
    public boolean isOwner(JarItem jar) {
        String uid = repository.currentUserId();
        return uid != null && uid.equals(jar.getCreatedBy());
    }

    /** Owner: move to Archives (restorable for 30 days, deletable there). */
    public void archive(JarItem jar) {
        if (!isOwner(jar)) return;
        repository.archive(jar.getId())
                .addOnSuccessListener(v -> message("Jar moved to Archives"))
                .addOnFailureListener(e -> message("Couldn't archive the jar. Try again."));
    }

    /** Participant: leave a jar someone else owns. */
    public void leave(JarItem jar) {
        String uid = repository.currentUserId();
        if (uid == null || isOwner(jar)) return;
        repository.removeParticipant(jar.getId(), uid)
                .addOnSuccessListener(v -> message("You left " + jar.getName()))
                .addOnFailureListener(e -> message("Couldn't leave the jar. Try again."));
    }

    private void subscribe() {
        subscription = repository.observeActiveJars(new JarRepository.Listener() {
            @Override
            public void onJars(List<JarFirestoreModel> jars) {
                allJars = jars;
                loading = false;
                error = null;
                publish();
            }

            @Override
            public void onError(Exception e) {
                loading = false;
                error = "Couldn't load your jars. Check your connection.";
                publish();
            }
        });
    }

    private void publish() {
        // Until a Mandatory jar has some savings, every other jar is locked
        boolean hasMandatory = false;
        boolean mandatoryFunded = false;
        for (JarFirestoreModel jar : allJars) {
            if (isMandatory(jar.getType())) {
                hasMandatory = true;
                if (jar.getSavedAmount() > 0) mandatoryFunded = true;
            }
        }
        boolean lockOthers = hasMandatory && !mandatoryFunded;

        List<JarItem> visible = new ArrayList<>();
        Money saved = Money.ZERO;
        Money goal = Money.ZERO;
        for (JarFirestoreModel jar : allJars) {
            saved = saved.plus(Money.ofPesos(jar.getSavedAmount()));
            goal = goal.plus(Money.ofPesos(jar.getGoalAmount()));

            if (!ALL.equalsIgnoreCase(filter) && !filter.equalsIgnoreCase(jar.getType())) continue;
            JarItem item = jar.toJarItem();
            item.setLocked(!isMandatory(jar.getType()) && lockOthers);
            visible.add(item);
        }

        boolean filteredOut = visible.isEmpty() && !allJars.isEmpty();
        state.setValue(new JarsUiState(loading, error, visible, saved, goal, filteredOut));
    }

    private static boolean isMandatory(String type) {
        return type != null && type.toLowerCase(Locale.US).contains("mandatory");
    }

    private void message(String text) {
        messages.setValue(new Event<>(text));
    }

    @Override
    protected void onCleared() {
        subscription.cancel();
    }
}
