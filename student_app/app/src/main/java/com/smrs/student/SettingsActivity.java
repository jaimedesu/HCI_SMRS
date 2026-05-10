package com.smrs.student;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StudentSession s = StudentSession.get();
        if (!s.isLoaded()) {
            startActivity(new Intent(this, NfcTapActivity.class));
            finish();
            return;
        }
        buildPage("Settings", "Manage your preferences");

        // ── NOTIFICATIONS ─────────────────────────────────────────────
        contentArea.addView(makeGroupLabel("NOTIFICATIONS"));
        LinearLayout notifCard = makeCard();
        notifCard.addView(makeToggleRow("🔔", "#E6F1FB", "Appointment reminders",
            "Notify 1 day before schedule", true));
        notifCard.addView(makeDivider());
        notifCard.addView(makeToggleRow("💊", "#FAEEDA", "Medication alerts",
            "Daily dosage reminders", true));
        notifCard.addView(makeDivider());
        notifCard.addView(makeToggleRow("📢", "#E1F5EE", "Clinic announcements",
            "Health advisories & news", false));
        contentArea.addView(notifCard);

        // ── ACCOUNT ───────────────────────────────────────────────────
        contentArea.addView(makeGroupLabel("ACCOUNT"));
        LinearLayout accountCard = makeCard();
        accountCard.addView(makeNavRow("🔒", "#EEEDFE", "Change password",
            "Last changed 3 months ago", null));
        accountCard.addView(makeDivider());
        accountCard.addView(makeNavRow("🛡", "#EAF3DE", "Privacy & data sharing",
            "Control who sees your records", null));
        accountCard.addView(makeDivider());
        accountCard.addView(makeNavRow("💬", "#F1EFE8", "Contact clinic support",
            "Report an issue or inquiry", null));
        accountCard.addView(makeDivider());
        // Request Medical Record — navigates to form
        accountCard.addView(makeNavRow("📄", "#DDEEFF", "Request Medical Record",
            "Submit a records request form via NFC", () ->
                startActivity(new Intent(this, RequestFormActivity.class)),
            "#1565C0", true));
        accountCard.addView(makeDivider());
        accountCard.addView(makeNavRow("📋", "#E6F1FB", "Request History",
            "View status of submitted requests", () ->
                startActivity(new Intent(this, RequestHistoryActivity.class))));
        contentArea.addView(accountCard);

        // ── APP ───────────────────────────────────────────────────────
        contentArea.addView(makeGroupLabel("APP"));
        LinearLayout appCard = makeCard();
        appCard.addView(makeNavRow("☀️", "#F1EFE8", "Appearance", "Light mode", null));
        appCard.addView(makeDivider());
        // Sign out — clears NFC session and storage
        appCard.addView(makeNavRow("🚪", "#FCEBEB", "Sign out",
            "Clears Clinic NFC session · " + s.getFullName(),
            () -> {
                StudentSession.get().clear(this);
                Intent i = new Intent(this, NfcTapActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
            }, "#A32D2D", false));
        contentArea.addView(appCard);
    }

    // ── Row builders ─────────────────────────────────────────────────

    private View makeToggleRow(String emoji, String iconBg,
                                String title, String subtitle, boolean on) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(11));

        row.addView(makeIconBox(emoji, iconBg));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        info.setPadding(dp(10), 0, 0, 0);

        TextView tvT = new TextView(this);
        tvT.setText(title);
        tvT.setTypeface(null, android.graphics.Typeface.BOLD);
        tvT.setTextSize(13);
        tvT.setTextColor(Color.parseColor("#1A1A1A"));
        info.addView(tvT);

        TextView tvS = new TextView(this);
        tvS.setText(subtitle);
        tvS.setTextSize(10);
        tvS.setTextColor(Color.parseColor("#888888"));
        info.addView(tvS);
        row.addView(info);

        // Toggle switch
        Switch toggle = new Switch(this);
        toggle.setChecked(on);
        toggle.setOnCheckedChangeListener((v, checked) -> {
            // In production: save preference to SharedPreferences
        });
        row.addView(toggle);
        return row;
    }

    private View makeNavRow(String emoji, String iconBg,
                             String title, String subtitle, Runnable action) {
        return makeNavRow(emoji, iconBg, title, subtitle, action, "#1A1A1A", false);
    }

    private View makeNavRow(String emoji, String iconBg, String title,
                             String subtitle, Runnable action,
                             String titleColor, boolean showNew) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(11), 0, dp(11));
        if (action != null) row.setOnClickListener(v -> action.run());

        row.addView(makeIconBox(emoji, iconBg));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        info.setPadding(dp(10), 0, 0, 0);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvT = new TextView(this);
        tvT.setText(title);
        tvT.setTypeface(null, android.graphics.Typeface.BOLD);
        tvT.setTextSize(13);
        tvT.setTextColor(Color.parseColor(titleColor));
        titleRow.addView(tvT);

        if (showNew) {
            TextView tvNew = makeBadge("NEW", "#1565C0", "#FFFFFF");
            LinearLayout.LayoutParams newLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            newLp.setMarginStart(dp(8));
            tvNew.setLayoutParams(newLp);
            titleRow.addView(tvNew);
        }
        info.addView(titleRow);

        TextView tvS = new TextView(this);
        tvS.setText(subtitle);
        tvS.setTextSize(10);
        tvS.setTextColor(Color.parseColor("#888888"));
        info.addView(tvS);
        row.addView(info);

        // Chevron
        TextView tvChev = new TextView(this);
        tvChev.setText("›");
        tvChev.setTextSize(20);
        tvChev.setTextColor(Color.parseColor("#CCCCCC"));
        row.addView(tvChev);

        return row;
    }

    private View makeIconBox(String emoji, String bg) {
        TextView tv = new TextView(this);
        tv.setText(emoji);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(Color.parseColor(bg));
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(34), dp(34)));
        return tv;
    }

    private TextView makeGroupLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#888888"));
        tv.setTextSize(10);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(6));
        tv.setLayoutParams(lp);
        return tv;
    }
}
