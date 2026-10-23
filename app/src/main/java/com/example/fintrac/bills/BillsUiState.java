package com.example.fintrac.bills;

import com.example.fintrac.Bill;
import com.example.fintrac.core.Money;

import java.util.Collections;
import java.util.List;

/** Everything the Bills screen needs to draw itself. Immutable; a new one is emitted per change. */
public final class BillsUiState {

    public final boolean loading;
    /** Non-null when bills couldn't be loaded. */
    public final String error;
    /** Bills after the category and paid/unpaid filters. */
    public final List<Bill> visibleBills;
    public final Money totalDue;
    public final Money totalPaid;
    public final boolean showPaid;

    BillsUiState(boolean loading, String error, List<Bill> visibleBills,
                 Money totalDue, Money totalPaid, boolean showPaid) {
        this.loading = loading;
        this.error = error;
        this.visibleBills = Collections.unmodifiableList(visibleBills);
        this.totalDue = totalDue;
        this.totalPaid = totalPaid;
        this.showPaid = showPaid;
    }

    static BillsUiState initial() {
        return new BillsUiState(true, null, Collections.emptyList(), Money.ZERO, Money.ZERO, false);
    }

    public boolean isEmpty() {
        return !loading && error == null && visibleBills.isEmpty();
    }
}
