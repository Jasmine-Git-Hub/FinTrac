package com.example.fintrac;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.fintrac.core.Money;
import com.example.fintrac.data.UserProfile;
import com.example.fintrac.home.HomeMath;
import com.example.fintrac.home.HomeViewModel;
import com.example.fintrac.home.NotificationsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Home dashboard. Draws {@link HomeViewModel} (balances, chart, recent activity, upcoming bills)
 * and {@link NotificationsViewModel} (bell + inbox). Everything updates live.
 */
public class HomeFragment extends Fragment {

    private static final String[] CHART_LABELS = {"Weekly", "Monthly", "Quarterly", "Yearly"};

    private HomeViewModel home;
    private NotificationsViewModel inbox;

    private TextView tvTotalBalance, tvSpendable, tvNoTransactions, welcomeText, usernameText;
    private TextView tvInJarsAmount, tvInJarsSubtitle, tvUnpaidAmount, tvUnpaidSubtitle, tvNoUnpaidBills;
    private TextView notificationBadge;
    private ImageView profileImage, notificationBell;
    private RecyclerView recentList, upcomingList;
    private MaterialButton btnSeeMore;
    private BarChart barChart;

    private TransactionAdapter recentAdapter;
    private UnpaidBillsAdapter upcomingAdapter;

    // Open inbox sheet (null when closed)
    private BottomSheetDialog inboxDialog;
    private NotificationAdapter inboxAdapter;
    private final List<NotificationItem> inboxRows = new ArrayList<>();
    private int inboxTab = NotificationsViewModel.TAB_ACTION_NEEDED;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        home = new ViewModelProvider(this).get(HomeViewModel.class);
        inbox = new ViewModelProvider(this).get(NotificationsViewModel.class);
        SettingHelper.updateRatesIfNecessary(requireContext());

        tvTotalBalance = view.findViewById(R.id.total_balance_amount);
        tvSpendable = view.findViewById(R.id.spendable_amount);
        tvNoTransactions = view.findViewById(R.id.no_transactions_text);
        welcomeText = view.findViewById(R.id.welcome_text);
        usernameText = view.findViewById(R.id.username_text);
        tvInJarsAmount = view.findViewById(R.id.in_jars_amount);
        tvInJarsSubtitle = view.findViewById(R.id.in_jars_subtitle);
        tvUnpaidAmount = view.findViewById(R.id.unpaid_bills_amount);
        tvUnpaidSubtitle = view.findViewById(R.id.unpaid_bills_subtitle);
        tvNoUnpaidBills = view.findViewById(R.id.tv_no_unpaid_bills);
        profileImage = view.findViewById(R.id.profile_image);
        notificationBell = view.findViewById(R.id.notification_bell);
        notificationBadge = view.findViewById(R.id.notification_badge);
        barChart = view.findViewById(R.id.weeklySpendingChart);

        recentAdapter = new TransactionAdapter();
        recentList = view.findViewById(R.id.transactions_recycler_view);
        recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentList.setAdapter(recentAdapter);

        upcomingAdapter = new UnpaidBillsAdapter();
        upcomingList = view.findViewById(R.id.rv_upcoming_bills);
        upcomingList.setLayoutManager(new LinearLayoutManager(requireContext()));
        upcomingList.setAdapter(upcomingAdapter);

        btnSeeMore = view.findViewById(R.id.btn_see_more_activity);
        btnSeeMore.setOnClickListener(v -> home.toggleRecentExpanded());

        AutoCompleteTextView chartRange = view.findViewById(R.id.dropdown_timeframe);
        chartRange.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, CHART_LABELS));
        chartRange.setText(CHART_LABELS[0], false);
        chartRange.setOnItemClickListener((parent, v, position, id) ->
                home.setChartRange(HomeMath.ChartRange.values()[position]));

        view.findViewById(R.id.add_transaction_fab).setOnClickListener(v -> showNewTransactionDialog());
        view.findViewById(R.id.profile_card).setOnClickListener(v -> showProfileMenu());
        view.findViewById(R.id.notification_bell_card).setOnClickListener(v -> showInbox());

        showGreeting();
        observe();
        startBackgroundSync();
    }

    @Override
    public void onResume() {
        super.onResume();
        showGreeting();
        home.refreshProfile();
    }

    @Override
    public void onDestroyView() {
        if (inboxDialog != null) inboxDialog.dismiss();
        super.onDestroyView();
    }

    private void observe() {
        home.state().observe(getViewLifecycleOwner(), this::render);
        home.profile().observe(getViewLifecycleOwner(), this::renderProfile);
        home.messages().observe(getViewLifecycleOwner(), event -> {
            String text = event.consume();
            if (text != null) toast(text);
        });

        inbox.all().observe(getViewLifecycleOwner(), items -> {
            int unread = NotificationsViewModel.unreadCount(items);
            notificationBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
            notificationBadge.setText(String.valueOf(unread));
            if (NotificationsViewModel.hasPendingInvite(items)) wiggleBell();
            refreshInboxRows();
        });
        inbox.messages().observe(getViewLifecycleOwner(), event -> {
            String text = event.consume();
            if (text != null) toast(text);
        });
        inbox.openJar().observe(getViewLifecycleOwner(), event -> {
            String[] jar = event.consume();
            if (jar != null) openJarDetails(jar[0], jar[1]);
        });
    }

    // ---- Rendering ----

    private void render(HomeViewModel.State s) {
        Context c = requireContext();
        tvTotalBalance.setText(SettingHelper.formatAmount(c, s.totalBalance.toPesos()));
        tvSpendable.setText(SettingHelper.formatAmount(c, s.spendable.toPesos()));
        tvInJarsAmount.setText(SettingHelper.formatAmount(c, s.inJars.toPesos()));
        tvInJarsSubtitle.setText(String.format(Locale.getDefault(), "%.1f%% of total balance", s.inJarsPercent));

        tvUnpaidAmount.setText(SettingHelper.formatAmount(c, s.unpaidTotal.toPesos()));
        int pending = s.upcomingBills.size();
        if (pending == 0) tvUnpaidSubtitle.setText(R.string.no_pending_bills);
        else if (pending == 1) tvUnpaidSubtitle.setText(R.string.one_pending_bill);
        else tvUnpaidSubtitle.setText(getString(R.string.pending_bills_count, pending));
        upcomingAdapter.submit(s.upcomingBills);
        upcomingList.setVisibility(pending == 0 ? View.GONE : View.VISIBLE);
        tvNoUnpaidBills.setVisibility(pending == 0 ? View.VISIBLE : View.GONE);

        recentAdapter.submit(s.recent);
        boolean noRecent = s.recent.isEmpty();
        tvNoTransactions.setVisibility(noRecent ? View.VISIBLE : View.GONE);
        recentList.setVisibility(noRecent ? View.GONE : View.VISIBLE);
        btnSeeMore.setVisibility(noRecent ? View.GONE : View.VISIBLE);
        btnSeeMore.setText(s.recentExpanded ? "See less" : "See more");

        renderChart(s.chartRange, s.chart);
    }

    private void showGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            usernameText.setText(R.string.guest);
            welcomeText.setText(R.string.welcome);
        } else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            usernameText.setText(user.getDisplayName());
            welcomeText.setText(R.string.welcome_back);
        } else {
            usernameText.setText(user.getEmail() != null ? user.getEmail() : getString(R.string.user));
            welcomeText.setText(R.string.welcome);
        }
    }

    private void renderProfile(UserProfile profile) {
        if (profile == null) return;
        if (!"User".equals(profile.displayName)) usernameText.setText(profile.displayName);

        if (profile.avatarUri != null && !profile.avatarUri.isEmpty()) {
            try {
                profileImage.setImageTintList(null);
                profileImage.setImageURI(Uri.parse(profile.avatarUri));
                return;
            } catch (RuntimeException ignored) {
                // Photo no longer accessible (e.g. deleted); fall through to defaults
            }
        }
        int resId = profile.avatarName != null
                ? getResources().getIdentifier(profile.avatarName, "drawable", requireContext().getPackageName()) : 0;
        if (resId != 0) {
            profileImage.setImageTintList(null);
            profileImage.setImageResource(resId);
        } else {
            profileImage.setImageResource(R.drawable.ic_person);
            profileImage.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_primary)));
        }
    }

    private void renderChart(HomeMath.ChartRange range, Money[] buckets) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String[] labels;
        switch (range) {
            case MONTHLY:
                labels = new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                break;
            case QUARTERLY:
                labels = new String[]{"Q1", "Q2", "Q3", "Q4"};
                break;
            case YEARLY:
                labels = new String[]{String.valueOf(year - 4), String.valueOf(year - 3),
                        String.valueOf(year - 2), String.valueOf(year - 1), String.valueOf(year)};
                break;
            case WEEKLY:
            default:
                labels = new String[]{getString(R.string.sun), getString(R.string.mon), getString(R.string.tue),
                        getString(R.string.wed), getString(R.string.thu), getString(R.string.fri), getString(R.string.sat)};
                break;
        }

        // Buckets are base PHP; the chart shows the user's display currency
        double rate = SettingHelper.getExchangeRate(requireContext(), SettingHelper.getCurrency(requireContext()));
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < buckets.length; i++) {
            entries.add(new BarEntry(i, (float) (buckets[i].toPesos() * rate)));
        }

        boolean dark = SettingHelper.isDarkMode(requireContext());
        BarDataSet dataSet = new BarDataSet(entries, "Dashboard Spending");
        dataSet.setColor(Color.parseColor(dark ? "#64B5F6" : "#2196F3"));
        dataSet.setDrawValues(false);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setSpaceTop(15f);
        barChart.getAxisLeft().setTextColor(dark ? Color.WHITE : Color.BLACK);
        barChart.setFitBars(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setContentDescription("Spending chart, " + CHART_LABELS[range.ordinal()]);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextColor(dark ? Color.WHITE : Color.BLACK);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(labels.length, false);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(labels.length - 0.5f);
        barChart.invalidate();
    }

    private void wiggleBell() {
        notificationBell.animate().rotation(10f).setDuration(100)
                .withEndAction(() -> notificationBell.animate().rotation(-10f).setDuration(100)
                        .withEndAction(() -> notificationBell.animate().rotation(0f).setDuration(100)))
                .start();
    }

    // ---- New transaction ----

    private void showNewTransactionDialog() {
        Context context = requireContext();
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_transaction, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(context).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        MaterialButton btnExpense = dialogView.findViewById(R.id.btn_type_expense);
        MaterialButton btnIncome = dialogView.findViewById(R.id.btn_type_income);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_transaction);
        TextInputLayout tilCategory = dialogView.findViewById(R.id.til_category);
        TextView tvCategoryLabel = dialogView.findViewById(R.id.tv_category_label);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        TextInputEditText etDesc = dialogView.findViewById(R.id.et_desc);
        AutoCompleteTextView actCategory = dialogView.findViewById(R.id.act_category);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);

        boolean dark = SettingHelper.isDarkMode(context);
        int red = dark ? Color.parseColor("#EF9A9A") : ContextCompat.getColor(context, R.color.jar_color_red);
        int green = dark ? Color.parseColor("#81C784") : ContextCompat.getColor(context, R.color.green);
        int gray = dark ? Color.parseColor("#424242") : ContextCompat.getColor(context, R.color.light_gray);
        int textGray = dark ? Color.parseColor("#B0B0B0") : ContextCompat.getColor(context, R.color.gray);

        String[] categories = {getString(R.string.food), getString(R.string.transportation),
                getString(R.string.bills), getString(R.string.shopping), getString(R.string.entertainment),
                getString(R.string.health), getString(R.string.education), getString(R.string.other)};
        actCategory.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, categories));
        actCategory.setText(categories[0], false);

        boolean[] isExpense = {true};
        Runnable updateType = () -> {
            int accent = isExpense[0] ? red : green;
            MaterialButton on = isExpense[0] ? btnExpense : btnIncome;
            MaterialButton off = isExpense[0] ? btnIncome : btnExpense;
            on.setStrokeColor(ColorStateList.valueOf(accent));
            on.setTextColor(accent);
            off.setStrokeColor(ColorStateList.valueOf(gray));
            off.setTextColor(textGray);
            int categoryVisibility = isExpense[0] ? View.VISIBLE : View.GONE;
            tilCategory.setVisibility(categoryVisibility);
            tvCategoryLabel.setVisibility(categoryVisibility);
            btnSave.setText(isExpense[0] ? R.string.add_expense : R.string.add_income);
            btnSave.setBackgroundTintList(ColorStateList.valueOf(accent));
        };
        updateType.run();
        btnExpense.setOnClickListener(v -> { isExpense[0] = true; updateType.run(); });
        btnIncome.setOnClickListener(v -> { isExpense[0] = false; updateType.run(); });
        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            Money typed = Money.parse(text(etAmount));
            Money inBase = typed.isPositive()
                    ? Money.ofPesos(SettingHelper.convertToBasePHP(context, typed.toPesos())) : null;
            String error = home.addTransaction(inBase, isExpense[0], text(etDesc), text(actCategory),
                    getString(isExpense[0] ? R.string.expense : R.string.income), dialog::dismiss);
            if (error != null) {
                toast(error);
            } else {
                btnSave.setEnabled(false);
                btnSave.setText(R.string.saving);
            }
        });
        dialog.show();
    }

    // ---- Profile ----

    private void showProfileMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_profile_menu, null, false);
        dialog.setContentView(view);
        if (view.getParent() instanceof View) ((View) view.getParent()).setBackgroundColor(Color.TRANSPARENT);

        view.findViewById(R.id.btnMenuEditProfile).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(requireContext(), EditProfileActivity.class));
        });
        view.findViewById(R.id.btnMenuLogout).setOnClickListener(v -> {
            dialog.dismiss();
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.logout)
                    .setMessage(R.string.logout_confirmation)
                    .setPositiveButton(R.string.yes, (d, w) -> logout())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
        view.findViewById(R.id.btnMenuCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        GoogleCredentialHelper.clearState(requireContext());
        OtpManager.clearSession(requireContext());
        Intent intent = new Intent(requireContext(), LogIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    // ---- Notifications ----

    private void showInbox() {
        inboxDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_notifications, null);
        inboxDialog.setContentView(view);

        TabLayout tabs = view.findViewById(R.id.tab_layout_notifications);
        TabLayout.Tab selected = tabs.getTabAt(inboxTab);
        if (selected != null) selected.select();
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                inboxTab = tab.getPosition();
                refreshInboxRows();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        inboxAdapter = new NotificationAdapter(requireContext(), inboxRows, new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onAcceptInvite(NotificationItem n) {
                toast("Joining jar...");
                inbox.acceptInvite(n);
            }

            @Override
            public void onDeclineInvite(NotificationItem n) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Decline Invitation")
                        .setMessage("Are you sure you want to decline the invitation to join '" + n.getJarName() + "'?")
                        .setPositiveButton("Yes, Decline", (d, w) -> inbox.declineInvite(n))
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onNotificationClick(NotificationItem n) {
                inbox.markRead(n);
                showNotificationDetails(n);
            }

            @Override
            public void onMarkAsRead(NotificationItem n) {
                inbox.markRead(n);
            }
        });
        RecyclerView list = view.findViewById(R.id.notifications_recycler_view);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(inboxAdapter);
        view.findViewById(R.id.mark_all_read).setOnClickListener(v -> inbox.markAllRead());

        inboxDialog.setOnShowListener(d -> {
            View sheet = inboxDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet == null) return;
            ViewGroup.LayoutParams params = sheet.getLayoutParams();
            params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.85);
            sheet.setLayoutParams(params);
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        });
        inboxDialog.setOnDismissListener(d -> {
            inboxDialog = null;
            inboxAdapter = null;
        });

        refreshInboxRows();
        inboxDialog.show();
    }

    private void refreshInboxRows() {
        if (inboxDialog == null || inboxAdapter == null) return;
        List<NotificationItem> all = inbox.all().getValue();
        inboxRows.clear();
        if (all != null) inboxRows.addAll(NotificationsViewModel.forTab(all, inboxTab));
        inboxAdapter.notifyDataSetChanged();

        boolean empty = inboxRows.isEmpty();
        View list = inboxDialog.findViewById(R.id.notifications_recycler_view);
        View emptyText = inboxDialog.findViewById(R.id.no_notifications_text);
        if (list != null) list.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (emptyText != null) emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showNotificationDetails(NotificationItem n) {
        BottomSheetDialog details = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_notification_details, null);
        details.setContentView(view);

        ((TextView) view.findViewById(R.id.detail_title)).setText(n.getTitle());
        ((TextView) view.findViewById(R.id.detail_message)).setText(n.getMessage());
        ((TextView) view.findViewById(R.id.detail_date)).setText(n.getTimestamp() != null
                ? new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(n.getTimestamp())
                : "Just now");

        String type = n.getType() != null ? n.getType() : "";
        int icon = R.drawable.ic_notifications;
        int bg = Color.parseColor("#F1F3F4");
        int tint = Color.parseColor("#5F6368");
        String action = null;
        switch (type) {
            case "BILL_PAID":
                icon = R.drawable.ic_visual_reports; bg = Color.parseColor("#E6F4EA"); tint = Color.parseColor("#4CAF50");
                action = "View Bills";
                break;
            case "BILL_DUE":
                icon = R.drawable.ic_visual_reports; bg = Color.parseColor("#FCE8E6"); tint = Color.parseColor("#EA4335");
                action = "Pay Bill Now";
                break;
            case "JAR_INVITE":
                icon = R.drawable.ic_add_person; bg = Color.parseColor("#E8F0FE"); tint = Color.parseColor("#1976D2");
                action = "Go to Jars";
                break;
            case "JAR_GOAL_REACHED":
                icon = R.drawable.ic_wallet; bg = Color.parseColor("#FEF7E0"); tint = Color.parseColor("#F29900");
                action = "View Jar Details";
                break;
            case "JAR_UPDATE":
            case "ADD_MONEY":
                icon = R.drawable.ic_wallet; bg = Color.parseColor("#E6F4EA"); tint = Color.parseColor("#4CAF50");
                action = "View Jar Details";
                break;
            case "CHALLENGE_UPDATE":
            case "CHALLENGE_COMPLETE":
                bg = Color.parseColor("#F3E5F5"); tint = Color.parseColor("#9C27B0");
                action = "View Challenges";
                break;
        }

        ImageView iconView = view.findViewById(R.id.detail_icon);
        iconView.setImageResource(icon);
        iconView.setColorFilter(tint);
        if (iconView.getBackground() != null) iconView.getBackground().setTint(bg);

        MaterialButton btnAction = view.findViewById(R.id.btn_action);
        if (action == null) {
            btnAction.setVisibility(View.GONE);
        } else {
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setText(action);
            btnAction.setOnClickListener(v -> {
                details.dismiss();
                if (inboxDialog != null) inboxDialog.dismiss();
                followNotification(type, n);
            });
        }
        view.findViewById(R.id.btn_close).setOnClickListener(v -> details.dismiss());
        details.show();
    }

    private void followNotification(String type, NotificationItem n) {
        if ("BILL_PAID".equals(type)) {
            startActivity(new Intent(requireContext(), ArchivesActivity.class));
        } else if (type.contains("BILL")) {
            switchTab(R.id.navigation_bills);
        } else if ("JAR_INVITE".equals(type)) {
            switchTab(R.id.navigation_jars);
        } else if (type.contains("CHALLENGE")) {
            switchTab(R.id.navigation_challenges);
        } else if (n.getJarId() != null) {
            openJarDetails(n.getJarId(), n.getJarName());
        } else {
            switchTab(R.id.navigation_jars);
        }
    }

    private void switchTab(int tabId) {
        if (getActivity() instanceof TabNavigator) {
            ((TabNavigator) getActivity()).selectBottomNavItem(tabId);
        }
    }

    private void openJarDetails(String jarId, String jarName) {
        if (inboxDialog != null) inboxDialog.dismiss();
        Intent intent = new Intent(requireContext(), SharedJarsDetails.class);
        intent.putExtra("JAR_ID", jarId);
        intent.putExtra("JAR_NAME", jarName);
        intent.putExtra("ICON_NAME", "hat");
        intent.putExtra("COLOR", 0xFFF44336);
        intent.putExtra("JAR_TYPE", "Shared");
        startActivity(intent);
    }

    // ---- Misc ----

    private void startBackgroundSync() {
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(FinTracWorker.class, 1, TimeUnit.HOURS).build();
        WorkManager.getInstance(requireContext())
                .enqueueUniquePeriodicWork("FinTracBackgroundSync", ExistingPeriodicWorkPolicy.KEEP, work);

        Intent sync = new Intent(requireContext(), DeadlineReminderReceiver.class);
        sync.putExtra("SILENT_SYNC", true);
        requireContext().sendBroadcast(sync);
    }

    private void toast(String text) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
    }

    private static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    // ---- Adapters ----

    private class UnpaidBillsAdapter extends RecyclerView.Adapter<UnpaidBillsAdapter.Holder> {
        private List<Bill> bills = new ArrayList<>();

        void submit(List<Bill> updated) {
            bills = updated;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unpaid_bill, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Bill bill = bills.get(position);
            holder.name.setText(bill.name);
            holder.category.setText(bill.category != null ? bill.category : "Others");
            holder.dueDate.setText(bill.dueDate);
            holder.amount.setText(SettingHelper.formatAmount(requireContext(), bill.money().toPesos()));
            holder.markPaid.setContentDescription("Mark " + bill.name + " as paid");
            holder.markPaid.setOnClickListener(v -> home.payBill(bill));
        }

        @Override
        public int getItemCount() {
            return bills.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name, amount, category, dueDate;
            final MaterialButton markPaid;

            Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.tv_bill_name);
                amount = itemView.findViewById(R.id.tv_bill_amount);
                category = itemView.findViewById(R.id.tv_bill_category);
                dueDate = itemView.findViewById(R.id.tv_bill_due_date);
                markPaid = itemView.findViewById(R.id.btn_mark_paid);
            }
        }
    }

    private static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.Holder> {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        private List<Transaction> items = new ArrayList<>();

        void submit(List<Transaction> updated) {
            items = updated;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Transaction item = items.get(position);
            Context context = holder.itemView.getContext();
            boolean dark = SettingHelper.isDarkMode(context);

            holder.title.setText(item.getTitle());
            holder.date.setText(item.getTimestamp() != null ? dateFormat.format(item.getTimestamp().toDate())
                    : item.getDate() != null ? dateFormat.format(item.getDate()) : "Recent");
            String desc = item.getDescription();
            holder.desc.setText(desc == null || desc.trim().isEmpty()
                    ? (item.getCategory() != null ? item.getCategory() : "") : desc);

            String amount = SettingHelper.formatAmount(context, item.getAmount());
            int color, background;
            if (item.isExpense()) {
                color = Color.parseColor(dark ? "#EF9A9A" : "#F44336");
                background = Color.parseColor(dark ? "#4A1F1F" : "#FCE8E6");
                holder.amount.setText(context.getString(R.string.amount_format_negative, amount));
                holder.icon.setImageResource(R.drawable.ic_arrow_down);
            } else {
                color = Color.parseColor(dark ? "#81C784" : "#4CAF50");
                background = Color.parseColor(dark ? "#1F4A23" : "#E6F4EA");
                holder.amount.setText(context.getString(R.string.amount_format_positive, amount));
                holder.icon.setImageResource(R.drawable.ic_arrow_up);
            }
            holder.amount.setTextColor(color);
            holder.icon.setColorFilter(color);
            holder.card.setCardBackgroundColor(background);
            holder.title.setTextColor(dark ? Color.WHITE : Color.BLACK);
            holder.date.setTextColor(dark ? Color.LTGRAY : Color.DKGRAY);
            holder.desc.setTextColor(dark ? Color.LTGRAY : Color.DKGRAY);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title, date, amount, desc;
            final CardView card;
            final ImageView icon;

            Holder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.text_title);
                date = itemView.findViewById(R.id.text_date);
                amount = itemView.findViewById(R.id.text_amount);
                desc = itemView.findViewById(R.id.text_desc);
                card = itemView.findViewById(R.id.icon_background_card);
                icon = itemView.findViewById(R.id.icon_image);
            }
        }
    }
}
