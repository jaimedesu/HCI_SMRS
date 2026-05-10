package com.smrs.student;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

/**
 * BaseActivity — shared chrome (header + bottom nav) for all post-NFC screens.
 * Subclasses call buildPage(title, subtitle) and add their content to getContentArea().
 */
public abstract class BaseActivity extends Activity {

    protected LinearLayout contentArea;
    private   TextView tvStatus;

    protected void buildPage(String title, String subtitle) {
        ScrollView root = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // ── Header ──────────────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor("#1565C0"));
        header.setPadding(dp(16), dp(36), dp(16), dp(14));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(tvTitle);

        // NFC badge
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setPadding(0, dp(4), 0, 0);
        badgeRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tvBadge = new TextView(this);
        tvBadge.setText("  ((·))  Clinic NFC ✓");
        tvBadge.setBackgroundColor(Color.parseColor("#1E429A"));
        tvBadge.setTextColor(Color.parseColor("#A0D4FF"));
        tvBadge.setTextSize(10);
        tvBadge.setTypeface(null, android.graphics.Typeface.BOLD);
        tvBadge.setPadding(dp(8), dp(3), dp(10), dp(3));
        badgeRow.addView(tvBadge);

        // Re-tap button
        TextView tvRetap = new TextView(this);
        tvRetap.setText("  ↺ Re-tap  ");
        tvRetap.setBackgroundColor(Color.parseColor("#1A3A8A"));
        tvRetap.setTextColor(Color.parseColor("#80BEFF"));
        tvRetap.setTextSize(10);
        tvRetap.setTypeface(null, android.graphics.Typeface.BOLD);
        tvRetap.setPadding(dp(10), dp(3), dp(10), dp(3));
        LinearLayout.LayoutParams retapLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        retapLp.setMarginStart(dp(8));
        tvRetap.setLayoutParams(retapLp);
        tvRetap.setOnClickListener(v -> {
            StudentSession.get().clear();
            Intent intent = new Intent(this, NfcTapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        badgeRow.addView(tvRetap);
        header.addView(badgeRow);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView tvSub = new TextView(this);
            tvSub.setText(subtitle);
            tvSub.setTextColor(Color.parseColor("#90BBEE"));
            tvSub.setTextSize(11);
            tvSub.setPadding(0, dp(4), 0, 0);
            header.addView(tvSub);
        }

        page.addView(header);

        // ── NFC sync banner ──────────────────────────────────────────
        tvStatus = new TextView(this);
        tvStatus.setText("📡  Data received from Clinic NFC Terminal · " +
                         StudentSession.get().getFullName());
        tvStatus.setBackgroundColor(Color.parseColor("#E6F1FB"));
        tvStatus.setTextColor(Color.parseColor("#185FA5"));
        tvStatus.setTextSize(10);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStatus.setPadding(dp(14), dp(8), dp(14), dp(8));
        page.addView(tvStatus);

        // ── Content area ─────────────────────────────────────────────
        contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setPadding(dp(14), dp(10), dp(14), dp(8));
        page.addView(contentArea);

        root.addView(page);

        // ── Bottom nav ───────────────────────────────────────────────
        LinearLayout navWrapper = new LinearLayout(this);
        navWrapper.setOrientation(LinearLayout.VERTICAL);

        // The scroll + nav in a vertical layout
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.addView(root, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        outer.addView(buildBottomNav());

        setContentView(outer);
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(0, dp(8), 0, dp(12));

        String[][] tabs = {
            {"🏠","Home", HomeActivity.class.getName()},
            {"📅","Schedule", ScheduleActivity.class.getName()},
            {"💊","Meds", MedicationActivity.class.getName()},
            {"👤","Profile", ProfileActivity.class.getName()},
            {"⚙️","Settings", SettingsActivity.class.getName()},
        };

        String currentClass = getClass().getName();
        for (String[] tab : tabs) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(android.view.Gravity.CENTER);
            item.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            boolean active = currentClass.equals(tab[2]);
            int color = active ? Color.parseColor("#1565C0") : Color.parseColor("#888888");

            TextView ic = new TextView(this);
            ic.setText(tab[0]);
            ic.setTextSize(18);
            ic.setGravity(android.view.Gravity.CENTER);
            item.addView(ic);

            TextView lbl = new TextView(this);
            lbl.setText(tab[1]);
            lbl.setTextSize(9);
            lbl.setTextColor(color);
            lbl.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            lbl.setGravity(android.view.Gravity.CENTER);
            item.addView(lbl);

            final String cls = tab[2];
            if (!active) {
                item.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, Class.forName(cls));
                        // Use proper Android 14+ compatible flags
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            }
            nav.addView(item);
        }
        return nav;
    }

    // ── UI helpers ───────────────────────────────────────────────────

    protected LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        return card;
    }

    protected TextView makeSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#1565C0"));
        tv.setTextSize(10);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLetterSpacing(0.08f);
        tv.setPadding(0, 0, 0, dp(8));
        return tv;
    }

    protected View makeDivider() {
        View v = new View(this);
        v.setBackgroundColor(Color.parseColor("#F0F0F0"));
        v.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return v;
    }

    protected LinearLayout makeInfoRow(String label, String value) {
        return makeInfoRow(label, value, "#1A1A1A");
    }

    protected LinearLayout makeInfoRow(String label, String value, String valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(9), 0, dp(9));

        TextView tvL = new TextView(this);
        tvL.setText(label);
        tvL.setTextColor(Color.parseColor("#888888"));
        tvL.setTextSize(12);
        tvL.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(tvL);

        TextView tvV = new TextView(this);
        tvV.setText(value);
        tvV.setTextColor(Color.parseColor(valueColor));
        tvV.setTextSize(12);
        tvV.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tvV);

        return row;
    }

    protected LinearLayout makeStatBox(String label, String value, String valueColor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.parseColor("#F0F4FF"));
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMarginEnd(dp(8));
        box.setLayoutParams(lp);

        TextView tvL = new TextView(this);
        tvL.setText(label);
        tvL.setTextColor(Color.parseColor("#888888"));
        tvL.setTextSize(10);
        box.addView(tvL);

        TextView tvV = new TextView(this);
        tvV.setText(value);
        tvV.setTextColor(Color.parseColor(valueColor));
        tvV.setTextSize(20);
        tvV.setTypeface(null, android.graphics.Typeface.BOLD);
        box.addView(tvV);

        return box;
    }

    protected TextView makeBadge(String text, String bg, String fg) {
        TextView tv = new TextView(this);
        tv.setText(text.toUpperCase());
        tv.setBackgroundColor(Color.parseColor(bg));
        tv.setTextColor(Color.parseColor(fg));
        tv.setTextSize(9);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(dp(8), dp(2), dp(8), dp(2));
        return tv;
    }

    protected int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }
}
