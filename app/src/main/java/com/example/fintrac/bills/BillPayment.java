package com.example.fintrac.bills;

import com.example.fintrac.Bill;
import com.example.fintrac.data.BillRepository;
import com.google.android.gms.tasks.Task;

import java.util.Date;

/**
 * The single rule for paying a bill, used by both the Bills and Home screens:
 * mark it paid, and if it's already past due, move it to Archives. Then record the
 * payment (notification + expense entry).
 */
public final class BillPayment {

    private BillPayment() {}

    /** @return a confirmation message for the user */
    public static Task<String> pay(BillRepository repository, Bill bill) {
        boolean overdue = bill.isOverdue(new Date());
        Task<Void> write = overdue ? repository.archive(bill.id) : repository.setPaid(bill.id, true);
        return write.continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            repository.recordPayment(bill);
            if (overdue) repository.clearReminders(bill.id);
            return overdue ? bill.name + " paid and archived (overdue)" : bill.name + " marked as paid";
        });
    }
}
