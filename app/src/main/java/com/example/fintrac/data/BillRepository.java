package com.example.fintrac.data;

import com.example.fintrac.Bill;
import com.example.fintrac.core.Subscription;
import com.google.android.gms.tasks.Task;

import java.util.List;

/**
 * All bill data access goes through here, so screens and ViewModels never touch Firestore
 * directly and can be tested with a fake implementation.
 */
public interface BillRepository {

    interface Listener {
        void onBills(List<Bill> bills);
        void onError(Exception e);
    }

    /** Live list of the signed-in user's non-archived bills. Cancel when no longer needed. */
    Subscription observeActiveBills(Listener listener);

    /** Adds the bill if it has no id, otherwise overwrites it. */
    Task<Void> save(Bill bill);

    Task<Void> setPaid(String billId, boolean paid);

    Task<Void> archive(String billId);

    /** Side effects of paying a bill: in-app notification and an auto-recorded expense. Best effort. */
    void recordPayment(Bill bill);

    /** Marks unread reminder notifications for this bill as read. Best effort. */
    void clearReminders(String billId);
}
