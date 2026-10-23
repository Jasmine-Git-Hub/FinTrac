package com.example.fintrac.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fintrac.NotificationItem;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Event;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.core.UserFacingException;
import com.example.fintrac.data.NotificationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/** The notification bell and inbox on Home. */
public class NotificationsViewModel extends ViewModel {

    /** Types that need the user to do something; unread ones go in the first tab. */
    private static final Set<String> ACTIONABLE = new HashSet<>(Arrays.asList(
            "BILL_DUE", "JAR_DEADLINE", "CHALLENGE_REMINDER", "CHALLENGE_DEADLINE", "JAR_INVITE"));

    public static final int TAB_ACTION_NEEDED = 0;
    public static final int TAB_OTHER = 1;

    private final NotificationRepository repository;
    private final Subscription subscription;

    private final MutableLiveData<List<NotificationItem>> all = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();
    /** Emits the jar to open after an invite is accepted: {jarId, jarName}. */
    private final MutableLiveData<Event<String[]>> openJar = new MutableLiveData<>();

    public NotificationsViewModel() {
        this(new NotificationRepository());
    }

    public NotificationsViewModel(NotificationRepository repository) {
        this.repository = repository;
        subscription = repository.observe(new DataListener<List<NotificationItem>>() {
            @Override
            public void onData(List<NotificationItem> data) {
                all.setValue(data);
            }

            @Override
            public void onError(Exception e) {
                // Bell just shows no badge
            }
        });
    }

    public LiveData<List<NotificationItem>> all() { return all; }
    public LiveData<Event<String>> messages() { return messages; }
    public LiveData<Event<String[]>> openJar() { return openJar; }

    public static int unreadCount(List<NotificationItem> items) {
        int n = 0;
        for (NotificationItem item : items) {
            if (!item.isRead()) n++;
        }
        return n;
    }

    public static boolean hasPendingInvite(List<NotificationItem> items) {
        for (NotificationItem item : items) {
            if (!item.isRead() && "JAR_INVITE".equals(item.getType())) return true;
        }
        return false;
    }

    public static List<NotificationItem> forTab(List<NotificationItem> items, int tab) {
        List<NotificationItem> out = new ArrayList<>();
        for (NotificationItem item : items) {
            boolean actionable = !item.isRead() && ACTIONABLE.contains(item.getType());
            if ((tab == TAB_ACTION_NEEDED) == actionable) out.add(item);
        }
        return out;
    }

    public void markRead(NotificationItem item) {
        if (item.isRead() || item.getId() == null) return;
        repository.markRead(Collections.singletonList(item.getId()));
    }

    public void markAllRead() {
        List<String> ids = new ArrayList<>();
        for (NotificationItem item : all.getValue()) {
            if (!item.isRead() && item.getId() != null) ids.add(item.getId());
        }
        if (ids.isEmpty()) {
            messages.setValue(new Event<>("You have no unread notifications."));
            return;
        }
        repository.markRead(ids)
                .addOnSuccessListener(v -> messages.setValue(new Event<>("All notifications marked as read.")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Failed to update some notifications.")));
    }

    public void acceptInvite(NotificationItem invite) {
        repository.acceptJarInvite(invite)
                .addOnSuccessListener(v -> {
                    messages.setValue(new Event<>("You've joined " + invite.getJarName() + "!"));
                    openJar.setValue(new Event<>(new String[]{invite.getJarId(), invite.getJarName()}));
                })
                .addOnFailureListener(e -> messages.setValue(new Event<>(e instanceof UserFacingException
                        ? e.getMessage() : "Couldn't accept the invitation. Try again.")));
    }

    public void declineInvite(NotificationItem invite) {
        repository.declineJarInvite(invite)
                .addOnSuccessListener(v -> messages.setValue(new Event<>("Invitation declined")))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Couldn't decline. Try again.")));
    }

    @Override
    protected void onCleared() {
        subscription.cancel();
    }
}
