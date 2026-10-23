package com.example.fintrac;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fintrac.challenges.ChallengesViewModel;
import com.example.fintrac.core.Money;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Savings challenges. Draws {@link ChallengesViewModel}; the list updates live, and check-ins
 * are validated by the repository (one per day, resetting at 8:00 AM).
 */
public class Challenge extends Fragment {

    private ChallengesViewModel viewModel;
    private ChallengeAdapter adapter;

    private TextView tvActiveCount, tvSavedTotal, tvMessage;
    private RecyclerView list;

    /** Set when opened from a notification; shown once the challenge has loaded. */
    private String pendingOpenId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_challenge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChallengesViewModel.class);

        tvActiveCount = view.findViewById(R.id.tv_active_count);
        tvSavedTotal = view.findViewById(R.id.tv_saved_total);
        tvMessage = view.findViewById(R.id.tv_challenges_message);

        adapter = new ChallengeAdapter(this::showDetails);
        list = view.findViewById(R.id.rv_active_challenges);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        view.findViewById(R.id.card_no_cafe).setOnClickListener(v ->
                showSetupDialog("No-Café Week", "Skip buying coffee for 7 days.", 7, 1050));
        view.findViewById(R.id.card_daily_20p).setOnClickListener(v ->
                showSetupDialog("Daily 20 Pesos Challenge", "Save 20 pesos every day for 30 days", 30, 600));
        view.findViewById(R.id.fab_add_challenge).setOnClickListener(v ->
                new TemplatesDialogFragment().show(getChildFragmentManager(), "templates_dialog"));

        getChildFragmentManager().setFragmentResultListener(TemplatesDialogFragment.REQUEST_KEY, getViewLifecycleOwner(),
                (key, result) -> {
                    String action = result.getString(TemplatesDialogFragment.KEY_ACTION, "");
                    String name = result.getString(TemplatesDialogFragment.KEY_NAME);
                    String desc = result.getString(TemplatesDialogFragment.KEY_DESC);
                    int days = result.getInt(TemplatesDialogFragment.KEY_DURATION);
                    double target = result.getDouble(TemplatesDialogFragment.KEY_TARGET);
                    if (TemplatesDialogFragment.ACTION_SELECT_TEMPLATE.equals(action)) {
                        showSetupDialog(name, desc, days, target);
                    } else if (TemplatesDialogFragment.ACTION_CREATE_CUSTOM.equals(action)) {
                        viewModel.start(name, desc, days, target);
                    }
                });

        // Opened from a notification (see ChallengeNavigationHelper)
        Bundle args = getArguments();
        if (args != null && savedInstanceState == null) {
            pendingOpenId = args.getString("open_challenge_id");
        }
        getParentFragmentManager().setFragmentResultListener("open_challenge_request", getViewLifecycleOwner(),
                (key, result) -> {
                    pendingOpenId = result.getString("open_challenge_id");
                    openPendingChallenge();
                });

        observe();
    }

    private void observe() {
        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            adapter.submitList(state.active);
            tvActiveCount.setText(String.valueOf(state.active.size()));
            tvSavedTotal.setText(SettingHelper.formatAmount(requireContext(), state.savedTotal.toPesos()));

            if (state.error != null) {
                tvMessage.setText(state.error);
                tvMessage.setVisibility(View.VISIBLE);
            } else if (!state.loading && state.active.isEmpty()) {
                tvMessage.setText(R.string.challenges_empty);
                tvMessage.setVisibility(View.VISIBLE);
            } else {
                tvMessage.setVisibility(View.GONE);
            }
            openPendingChallenge();
        });
        viewModel.messages().observe(getViewLifecycleOwner(), event -> {
            String text = event.consume();
            if (text != null) toast(text);
        });
        viewModel.checkedIn().observe(getViewLifecycleOwner(), event -> {
            Money amount = event.consume();
            if (amount != null) toast("Checked In! +" + format(amount) + " saved!");
        });
        viewModel.completed().observe(getViewLifecycleOwner(), event -> {
            ChallengeItem item = event.consume();
            if (item != null) showCompletedDialog(item);
        });
    }

    private void openPendingChallenge() {
        if (pendingOpenId == null) return;
        ChallengesViewModel.State state = viewModel.state().getValue();
        if (state == null || state.loading) return;
        for (ChallengeItem item : state.active) {
            if (pendingOpenId.equals(item.getDocumentId())) {
                pendingOpenId = null;
                showDetails(item);
                return;
            }
        }
        pendingOpenId = null; // Finished or deleted since the notification was sent
    }

    private void showSetupDialog(String name, String desc, int days, double target) {
        CustomChallengeDialogFragment dialog = new CustomChallengeDialogFragment();
        Bundle args = new Bundle();
        args.putString("template_name", name);
        args.putString("template_desc", desc);
        args.putInt("template_duration", days);
        args.putDouble("template_target", target);
        dialog.setArguments(args);
        dialog.setOnCustomChallengeCreatedListener(c ->
                viewModel.start(c.getName(), c.getDescription(), c.getDurationDays(), c.getTargetAmount()));
        dialog.show(getChildFragmentManager(), "custom_challenge_dialog");
    }

    private void showDetails(ChallengeItem item) {
        View view = getLayoutInflater().inflate(R.layout.dialog_challenge_detail, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        boolean dark = SettingHelper.isDarkMode(requireContext());
        TextView name = view.findViewById(R.id.tv_challenge_name);
        TextView desc = view.findViewById(R.id.tv_challenge_desc);
        TextView day = view.findViewById(R.id.tv_current_day);
        name.setText(item.getName());
        desc.setText(item.getDescription());
        ((TextView) view.findViewById(R.id.tv_saved_amount)).setText(format(item.savedSoFar()));
        name.setTextColor(dark ? Color.WHITE : Color.parseColor("#37474F"));
        desc.setTextColor(Color.parseColor(dark ? "#B0BEC5" : "#546E7A"));
        day.setTextColor(dark ? Color.WHITE : Color.parseColor("#37474F"));

        ImageView icon = view.findViewById(R.id.iv_challenge_icon);
        icon.setImageResource(item.getIconResId());
        icon.clearColorFilter();
        MaterialCardView iconCard = view.findViewById(R.id.card_icon_container);
        iconCard.setCardBackgroundColor(Color.parseColor(dark ? "#1DE9B6" : "#E0F2F1"));
        if (dark) iconCard.setAlpha(0.2f);

        String dayText = "Day " + item.getCurrentDay() + " of " + item.getTotalDays();
        MaterialButton checkIn = view.findViewById(R.id.btn_check_in);
        if (item.canCheckInNow()) {
            day.setText(dayText);
            checkIn.setEnabled(true);
            checkIn.setAlpha(1f);
            checkIn.setText("Check In Today (+" + format(item.dailyAmount()) + ")");
            checkIn.setOnClickListener(v -> {
                checkIn.setEnabled(false); // no double taps
                viewModel.checkIn(item);
                dialog.dismiss();
            });
        } else {
            day.setText(dayText + "\nNext check-in at 8:00 AM");
            checkIn.setEnabled(false);
            checkIn.setAlpha(0.5f);
            checkIn.setText("✓ Checked In Today");
        }

        view.findViewById(R.id.btn_give_up).setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("Stop Challenge")
                .setMessage("Do you want to archive this challenge or delete it permanently?")
                .setPositiveButton("Archive", (d, w) -> {
                    viewModel.archive(item);
                    dialog.dismiss();
                })
                .setNegativeButton("Delete Permanently", (d, w) -> {
                    viewModel.delete(item);
                    dialog.dismiss();
                })
                .setNeutralButton("Cancel", null)
                .show());
        view.findViewById(R.id.btn_close).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showCompletedDialog(ChallengeItem item) {
        View view = getLayoutInflater().inflate(R.layout.dialog_challenge_completed, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).setCancelable(false).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        ((TextView) view.findViewById(R.id.tv_completed_title)).setText("You've conquered the " + item.getName() + "!");
        ((TextView) view.findViewById(R.id.tv_completed_desc)).setText("Incredible discipline! You successfully saved "
                + format(item.savedSoFar()) + " in " + item.getTotalDays() + " days. Keep up the great work!");
        view.findViewById(R.id.btn_continue_success).setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.archive(item);
        });
        dialog.show();
    }

    private String format(Money amount) {
        return SettingHelper.formatAmount(requireContext(), amount.toPesos());
    }

    private void toast(String text) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
    }
}
