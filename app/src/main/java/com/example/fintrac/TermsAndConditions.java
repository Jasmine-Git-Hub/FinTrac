package com.example.fintrac;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class TermsAndConditions {

    /**
     * Shows Terms & Privacy Policy dialog with card-style boxes for each section
     * Matches the "Start Challenge" design with individual cards
     */
    public static void showTermsDialog(Context context, CheckBox termsCheckbox) {
        // Check if dark mode is enabled
        boolean isDarkMode = SettingHelper.isDarkMode(context);

        // Color scheme matching your app
        int bgColor = isDarkMode ? Color.parseColor("#1A1A1A") : Color.parseColor("#FFFFFF");
        int textColor = isDarkMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#1A1A1A");
        int cardBgColor = isDarkMode ? Color.parseColor("#252525") : Color.parseColor("#F8F8F8");
        int dividerColor = isDarkMode ? Color.parseColor("#2A2A2A") : Color.parseColor("#F0F0F0");
        int accentColor = Color.parseColor("#0080FF"); // Your blue
        int secondaryTextColor = isDarkMode ? Color.parseColor("#CCCCCC") : Color.parseColor("#666666");

        // --- Root container ---
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(bgColor);

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(bgColor);
        cardBackground.setCornerRadius(dpToPx(context, 16));
        rootLayout.setBackground(cardBackground);

        int padding = dpToPx(context, 16);
        rootLayout.setPadding(padding, dpToPx(context, 16), padding, dpToPx(context, 16));

        // --- Header Row: Title + X button ---
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleText = new TextView(context);
        titleText.setText("Terms & Conditions");
        titleText.setTextSize(18f);
        titleText.setTextColor(textColor);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleText.setLayoutParams(titleParams);

        // Last Updated text
        TextView lastUpdatedText = new TextView(context);
        lastUpdatedText.setText("Last Updated: February 23, 2026");
        lastUpdatedText.setTextSize(10f);
        lastUpdatedText.setTextColor(secondaryTextColor);
        lastUpdatedText.setPadding(0, dpToPx(context, 4), 0, 0);

        TextView closeBtn = new TextView(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(26f);
        closeBtn.setTextColor(accentColor);
        closeBtn.setPadding(dpToPx(context, 8), 0, 0, 0);

        headerRow.addView(titleText);
        headerRow.addView(closeBtn);

        // --- Divider ---
        View divider = new View(context);
        divider.setBackgroundColor(dividerColor);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 1));
        dividerParams.setMargins(0, dpToPx(context, 12), 0, dpToPx(context, 12));
        divider.setLayoutParams(dividerParams);

        // --- ScrollView with card items ---
        ScrollView scrollView = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 480));
        scrollView.setLayoutParams(scrollParams);

        // --- Content container (vertical layout for cards) ---
        LinearLayout contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ===== WELCOME CARD =====
        contentContainer.addView(createTitleCard(context, "Welcome to Fintrac",
                "By registering an account, you agree to be bound by these Terms and Conditions.",
                cardBgColor, textColor, secondaryTextColor));

        // ===== SECTION 1: Description of Service =====
        contentContainer.addView(createSectionCard(context, "1. Description of Service",
                "Fintrac is a financial organization and tracking tool. Our services include:\n\n" +
                        "• Jars: Virtual buckets to categorize and track savings goals.\n" +
                        "• Shared Jars: Collaborative tracking where users can invite others to view or contribute to a shared goal.\n" +
                        "• Challenges: Educational savings templates (pre-set or custom).\n" +
                        "• Bill Tracker: A dashboard to record and monitor paid/unpaid obligations.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 2: No Financial Advice or Banking Services =====
        contentContainer.addView(createSectionCard(context, "2. No Financial Advice or Banking Services",
                "Not a Bank: Fintrac is a data-tracking platform. We do not hold, manage, or provide insurance for physical currency unless explicitly integrated with a licensed financial institution partner.\n\n" +
                        "No Advice: Any \"Challenges\" or templates provided are for motivational purposes. Fintrac does not provide professional financial, investment, or legal advice.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 3: Account Registration & Security =====
        contentContainer.addView(createSectionCard(context, "3. Account Registration & Security",
                "Eligibility: You must be at least 18 years old to use this App.\n\n" +
                        "Information Accuracy: You agree to provide accurate email and profile information.\n\n" +
                        "Account Security: You are solely responsible for maintaining the confidentiality of your password. Any activity occurring under your account is your responsibility.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 4: Shared Jars & User Conduct =====
        contentContainer.addView(createSectionCard(context, "4. Shared Jars & User Conduct",
                "Shared Visibility: By inviting a user to a \"Shared Jar,\" you acknowledge that they will be able to see the Jar's balance, goal, and contribution history.\n\n" +
                        "User Disputes: Fintrac is a neutral platform. We are not responsible for disputes between users regarding money contributed to or withdrawn from shared goals.\n\n" +
                        "Prohibited Content: Users may not use Jar names or descriptions that are offensive, fraudulent, or illegal.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 5: Philippine Data Privacy Act (DPA) Compliance =====
        contentContainer.addView(createSectionCard(context, "5. Philippine Data Privacy Act (DPA) Compliance",
                "In accordance with Republic Act No. 10173, Fintrac protects your privacy through the following:\n\n" +
                        "Consent: By clicking \"I Agree,\" you give explicit consent to the collection and processing of your personal information (Name, Email, Password) for the purpose of account management and app functionality.\n\n" +
                        "Data Subject Rights: You have the right to access, correct, and request deletion of your personal data at any time via the App settings or by contacting our Data Protection Officer.\n\n" +
                        "Transparency: We do not sell your personal data to third parties. Data sharing is limited to what is necessary for \"Shared Jars\" features as initiated by you.\n\n" +
                        "Security: We implement industry-standard encryption to protect your sensitive login and financial tracking data.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 6: Challenges and Bill Tracking =====
        contentContainer.addView(createSectionCard(context, "6. Challenges and Bill Tracking",
                "Custom Challenges: Fintrac is not liable for any financial strain caused by following user-generated or template-based savings challenges.\n\n" +
                        "Bill Accuracy: It is your responsibility to verify the accuracy of your \"Paid/Unpaid\" status. Fintrac is not responsible for late fees or penalties incurred with your external service providers.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 7: Termination of Use =====
        contentContainer.addView(createSectionCard(context, "7. Termination of Use",
                "Fintrac reserves the right to suspend or terminate your account if:\n\n" +
                        "• You violate these Terms.\n" +
                        "• You engage in fraudulent or illegal activity.\n" +
                        "• You attempt to bypass the App's security measures.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 8: Limitation of Liability =====
        contentContainer.addView(createSectionCard(context, "8. Limitation of Liability",
                "To the fullest extent permitted under Philippine law, Fintrac and its developers shall not be liable for any indirect, incidental, or consequential damages resulting from your use of the App, including loss of data or financial loss.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 9: Governing Law =====
        contentContainer.addView(createSectionCard(context, "9. Governing Law",
                "These Terms are governed by the laws of the Republic of the Philippines. Any legal action arising from these Terms shall be filed in the competent courts of [Your City], Philippines.",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 10: Contact Us =====
        contentContainer.addView(createSectionCard(context, "10. Contact Us",
                "For questions or data privacy concerns, please contact our Data Protection Officer at:\n\n" +
                        "Email: support@fintrac.ph",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        scrollView.addView(contentContainer);

        // --- Assemble layout ---
        rootLayout.addView(headerRow);
        rootLayout.addView(lastUpdatedText);
        rootLayout.addView(divider);
        rootLayout.addView(scrollView);

        // --- Build & show dialog ---
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(rootLayout)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95);
            dialog.getWindow().setAttributes(layoutParams);
        }

        dialog.show();
        animateDialogEntrance(rootLayout);

        closeBtn.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * Create a title/welcome card
     */
    private static View createTitleCard(Context context, String title, String description,
                                        int cardBgColor, int textColor, int secondaryTextColor) {
        LinearLayout cardLayout = new LinearLayout(context);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundColor(cardBgColor);

        GradientDrawable cardDrawable = new GradientDrawable();
        cardDrawable.setColor(cardBgColor);
        cardDrawable.setCornerRadius(dpToPx(context, 12));
        cardLayout.setBackground(cardDrawable);

        int padding = dpToPx(context, 14);
        cardLayout.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(context, 12));
        cardLayout.setLayoutParams(cardParams);

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15f);
        titleView.setTextColor(textColor);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Description
        TextView descView = new TextView(context);
        descView.setText(description);
        descView.setTextSize(12.5f);
        descView.setTextColor(secondaryTextColor);
        descView.setLineSpacing(4f, 1.2f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, dpToPx(context, 8), 0, 0);
        descView.setLayoutParams(descParams);

        cardLayout.addView(titleView);
        cardLayout.addView(descView);

        return cardLayout;
    }

    /**
     * Create a section card with number, title, and content
     */
    private static View createSectionCard(Context context, String title, String content,
                                          int cardBgColor, int accentColor, int textColor, int secondaryTextColor) {
        LinearLayout cardLayout = new LinearLayout(context);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundColor(cardBgColor);

        GradientDrawable cardDrawable = new GradientDrawable();
        cardDrawable.setColor(cardBgColor);
        cardDrawable.setCornerRadius(dpToPx(context, 12));
        cardLayout.setBackground(cardDrawable);

        int padding = dpToPx(context, 14);
        cardLayout.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(context, 12));
        cardLayout.setLayoutParams(cardParams);

        // Section title (colored)
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(14f);
        titleView.setTextColor(accentColor);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Content
        TextView contentView = new TextView(context);
        contentView.setText(content);
        contentView.setTextSize(12.5f);
        contentView.setTextColor(secondaryTextColor);
        contentView.setLineSpacing(5f, 1.2f);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.setMargins(0, dpToPx(context, 8), 0, 0);
        contentView.setLayoutParams(contentParams);

        cardLayout.addView(titleView);
        cardLayout.addView(contentView);

        return cardLayout;
    }

    /**
     * Animate dialog entrance
     */
    private static void animateDialogEntrance(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.85f);
        view.setScaleY(0.85f);

        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();
    }

    // Helper: dp to px
    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}