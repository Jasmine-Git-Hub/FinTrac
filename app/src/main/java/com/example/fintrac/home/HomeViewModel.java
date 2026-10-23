package com.example.fintrac.home;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fintrac.Bill;
import com.example.fintrac.JarFirestoreModel;
import com.example.fintrac.Transaction;
import com.example.fintrac.bills.BillPayment;
import com.example.fintrac.core.DataListener;
import com.example.fintrac.core.Event;
import com.example.fintrac.core.Money;
import com.example.fintrac.core.Subscription;
import com.example.fintrac.data.BillRepository;
import com.example.fintrac.data.FirestoreBillRepository;
import com.example.fintrac.data.FirestoreJarRepository;
import com.example.fintrac.data.JarRepository;
import com.example.fintrac.data.TransactionRepository;
import com.example.fintrac.data.UserProfile;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Home dashboard. Listens live to the user's transactions, jars and bills, so balances,
 * the chart and lists update by themselves after any change anywhere in the app.
 */
public class HomeViewModel extends ViewModel {

    /** Immutable snapshot of the dashboard. Amounts are in base currency (PHP). */
    public static final class State {
        public final Money totalBalance;
        public final Money spendable;
        public final Money inJars;
        /** In-jars as a percent of total balance (0 when there's no balance). */
        public final double inJarsPercent;
        public final List<Bill> upcomingBills;
        public final Money unpaidTotal;
        public final List<Transaction> recent;
        public final boolean recentExpanded;
        public final HomeMath.ChartRange chartRange;
        public final Money[] chart;

        State(Money totalBalance, Money spendable, Money inJars, double inJarsPercent,
              List<Bill> upcomingBills, Money unpaidTotal, List<Transaction> recent,
              boolean recentExpanded, HomeMath.ChartRange chartRange, Money[] chart) {
            this.totalBalance = totalBalance;
            this.spendable = spendable;
            this.inJars = inJars;
            this.inJarsPercent = inJarsPercent;
            this.upcomingBills = Collections.unmodifiableList(upcomingBills);
            this.unpaidTotal = unpaidTotal;
            this.recent = Collections.unmodifiableList(recent);
            this.recentExpanded = recentExpanded;
            this.chartRange = chartRange;
            this.chart = chart;
        }
    }

    private final TransactionRepository transactions;
    private final JarRepository jars;
    private final BillRepository bills;

    private final Subscription txSubscription, jarSubscription, billSubscription;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> messages = new MutableLiveData<>();

    private List<Transaction> allTransactions = Collections.emptyList();
    private List<JarFirestoreModel> allJars = Collections.emptyList();
    private List<Bill> allBills = Collections.emptyList();
    private boolean recentExpanded = false;
    private HomeMath.ChartRange chartRange = HomeMath.ChartRange.WEEKLY;

    public HomeViewModel() {
        this(new TransactionRepository(), new FirestoreJarRepository(), new FirestoreBillRepository());
    }

    public HomeViewModel(TransactionRepository transactions, JarRepository jars, BillRepository bills) {
        this.transactions = transactions;
        this.jars = jars;
        this.bills = bills;

        txSubscription = transactions.observeAll(new DataListener<List<Transaction>>() {
            @Override
            public void onData(List<Transaction> data) {
                allTransactions = data;
                publish();
            }

            @Override
            public void onError(Exception e) {
                messages.setValue(new Event<>("Couldn't load your transactions."));
            }
        });
        jarSubscription = jars.observeActiveJars(new JarRepository.Listener() {
            @Override
            public void onJars(List<JarFirestoreModel> data) {
                allJars = data;
                publish();
            }

            @Override
            public void onError(Exception e) {
                // Jars card just stays at its last value
            }
        });
        billSubscription = bills.observeActiveBills(new BillRepository.Listener() {
            @Override
            public void onBills(List<Bill> data) {
                allBills = data;
                publish();
            }

            @Override
            public void onError(Exception e) {
                // Bills card just stays at its last value
            }
        });
        refreshProfile();
    }

    public LiveData<State> state() { return state; }
    public LiveData<UserProfile> profile() { return profile; }
    public LiveData<Event<String>> messages() { return messages; }

    /** Called on resume, since the profile is edited on another screen. */
    public void refreshProfile() {
        String uid = jars.currentUserId();
        if (uid == null) return;
        jars.loadProfile(uid).addOnSuccessListener(profile::setValue);
    }

    public void toggleRecentExpanded() {
        recentExpanded = !recentExpanded;
        publish();
    }

    public void setChartRange(HomeMath.ChartRange range) {
        chartRange = range;
        publish();
    }

    public void payBill(Bill bill) {
        BillPayment.pay(bills, bill)
                .addOnSuccessListener(text -> messages.setValue(new Event<>(text)))
                .addOnFailureListener(e -> messages.setValue(new Event<>("Failed to mark bill as paid")));
    }

    /**
     * @param amountInBase already converted to base PHP; null if the typed amount was invalid
     * @return an error message for the form, or null if saving started
     */
    @Nullable
    public String addTransaction(@Nullable Money amountInBase, boolean isExpense, String description,
                                 String category, String fallbackTitle, Runnable onSaved) {
        String uid = jars.currentUserId();
        if (uid == null) return "Please log in again.";
        if (amountInBase == null || !amountInBase.isPositive()) return "Enter an amount greater than zero";
        if (description.isEmpty()) return "Please enter a description";
        if (isExpense && category.isEmpty()) return "Please select a category";

        String title = isExpense ? category : description;
        if (title.isEmpty()) title = fallbackTitle;

        Transaction transaction = new Transaction(title, description, category,
                amountInBase.toPesos(), isExpense, uid);
        transactions.add(transaction)
                .addOnSuccessListener(v -> {
                    messages.setValue(new Event<>("Transaction saved"));
                    onSaved.run();
                })
                .addOnFailureListener(e -> messages.setValue(new Event<>("Couldn't save. Check your connection.")));
        return null;
    }

    private void publish() {
        // Spendable = income − expenses. Jar deposits are recorded as expenses ("Saved to Jar"),
        // so the money set aside is added back to get the total. Spendable may go negative
        // (overspending), which is shown honestly rather than as zero.
        Money[] totals = HomeMath.incomeAndExpense(allTransactions);
        Money spendable = totals[0].minus(totals[1]);

        String uid = jars.currentUserId();
        Money inJars = uid != null ? HomeMath.myJarSavings(allJars, uid) : Money.ZERO;
        Money balance = spendable.plus(inJars);
        double percent = balance.isPositive() ? inJars.cents() * 100.0 / balance.cents() : 0;

        List<Bill> upcoming = HomeMath.upcomingUnpaid(allBills);
        Date now = new Date();

        state.setValue(new State(balance, spendable, inJars, percent, upcoming, HomeMath.total(upcoming),
                HomeMath.recent(allTransactions, recentExpanded ? 50 : 5, now), recentExpanded,
                chartRange, HomeMath.expenseBuckets(allTransactions, chartRange, now)));
    }

    @Override
    protected void onCleared() {
        txSubscription.cancel();
        jarSubscription.cancel();
        billSubscription.cancel();
    }
}
