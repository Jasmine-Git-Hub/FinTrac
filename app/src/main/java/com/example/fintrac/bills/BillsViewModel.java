package com.example.fintrac.bills;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fintrac.Bill;
import com.example.fintrac.R;
import com.example.fintrac.core.Event;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.data.BillRepository;
import com.example.fintrac.data.FirestoreBillRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Bills screen logic: live bill list, filtering, totals, and pay/archive/save rules.
 * Survives rotation, and the Firestore listener is removed in {@link #onCleared()}.
 */
public class BillsViewModel extends ViewModel {

    public static final String ALL = "All";

    private final BillRepository repository;
    private Subscription subscription;

    private final MutableLiveData<BillsUiState> state = new MutableLiveData<>(BillsUiState.initial());
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();

    private List<Bill> allBills = Collections.emptyList();
    private String category = ALL;
    private boolean showPaid = false;
    private boolean loading = true;
    private String error;
    private boolean overdueSweepDone = false;

    public BillsViewModel() {
        this(new FirestoreBillRepository());
    }

    public BillsViewModel(BillRepository repository) {
        this.repository = repository;
        subscribe();
    }

    /** After a load error: drop the failed listener and listen again. */
    public void retry() {
        subscription.cancel();
        loading = true;
        error = null;
        publish();
        subscribe();
    }

    private void subscribe() {
        subscription = repository.observeActiveBills(new BillRepository.Listener() {
            @Override
            public void onBills(List<Bill> bills) {
                allBills = bills;
                loading = false;
                error = null;
                archiveOverduePaidBillsOnce();
                publish();
            }

            @Override
            public void onError(Exception e) {
                loading = false;
                error = "Couldn't load your bills. Check your connection.";
                publish();
            }
        });
    }

    public LiveData<BillsUiState> state() {
        return state;
    }

    public LiveData<Event<String>> messages() {
        return messages;
    }

    public void setCategory(String newCategory) {
        category = newCategory == null ? ALL : newCategory;
        publish();
    }

    public void toggleShowPaid() {
        showPaid = !showPaid;
        publish();
    }

    /** Checkbox tap: pay an unpaid bill, or un-pay a paid one. */
    public void togglePaid(Bill bill) {
        if (bill == null || bill.id == null) return;

        if (bill.isPaid) {
            repository.setPaid(bill.id, false)
                    .addOnSuccessListener(v -> message("Bill marked as unpaid"))
                    .addOnFailureListener(e -> message("Failed to update bill status"));
            return;
        }

        BillPayment.pay(repository, bill)
                .addOnSuccessListener(this::message)
                .addOnFailureListener(e -> message("Failed to mark bill as paid"));
    }

    public void archive(Bill bill) {
        if (bill == null || bill.id == null) return;
        repository.archive(bill.id)
                .addOnSuccessListener(v -> {
                    message("Bill moved to Archives");
                    repository.clearReminders(bill.id);
                })
                .addOnFailureListener(e -> message("Failed to archive bill"));
    }

    /**
     * Validates and saves a new or edited bill.
     *
     * @param amountInBase amount already converted to the base currency (PHP), or null if the
     *                     entered text wasn't a number
     * @return an error message to show in the form, or null if the save was started
     */
    @Nullable
    public String save(@Nullable Bill existing, String name, @Nullable Money amountInBase,
                       String dueDate, String category, String frequency) {
        if (name.isEmpty() || dueDate.isEmpty()) return "Please fill all fields";
        if (amountInBase == null || !amountInBase.isPositive()) return "Enter an amount greater than zero";

        Bill bill = new Bill(name, category, frequency, null, "Due: " + dueDate,
                R.drawable.ic_visual_reports, existing != null && existing.isPaid);
        bill.setMoney(amountInBase);
        if (bill.dueDateValue() == null) return "Choose a valid due date";
        if (existing != null) bill.id = existing.id;

        boolean editing = existing != null;
        repository.save(bill)
                .addOnSuccessListener(v -> message(editing ? "Bill updated" : "Bill added"))
                .addOnFailureListener(e -> message(editing ? "Failed to update bill" : "Failed to add bill"));
        return null;
    }

    /** Paid bills whose due date has passed are moved to Archives (once per screen session). */
    private void archiveOverduePaidBillsOnce() {
        if (overdueSweepDone) return;
        overdueSweepDone = true;
        Date now = new Date();
        for (Bill bill : allBills) {
            if (bill.isPaid && bill.id != null && bill.isOverdue(now)) {
                repository.archive(bill.id);
            }
        }
    }

    private void publish() {
        List<Bill> visible = new ArrayList<>();
        Money due = Money.ZERO;
        Money paid = Money.ZERO;

        for (Bill bill : allBills) {
            if (bill.isPaid) {
                paid = paid.plus(bill.money());
            } else {
                due = due.plus(bill.money());
            }

            boolean categoryMatches = ALL.equalsIgnoreCase(category)
                    || (bill.category != null && bill.category.equalsIgnoreCase(category));
            if (categoryMatches && (showPaid || !bill.isPaid)) {
                visible.add(bill);
            }
        }

        state.setValue(new BillsUiState(loading, error, visible, due, paid, showPaid));
    }

    private void message(String text) {
        messages.setValue(new Event<>(text));
    }

    @Override
    protected void onCleared() {
        subscription.cancel();
    }
}
