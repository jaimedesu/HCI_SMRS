package com.smrs.student;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.*;

public class MedicationActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StudentSession s = StudentSession.get();
        if (!s.isLoaded() && !s.loadFromStorage(this)) {
            startActivity(new Intent(this, NfcTapActivity.class));
            finish();
            return;
        }
        buildPage("Medication", "Prescribed medicines");

        JSONArray meds = s.getMedications();

        // ── Allergy warning ───────────────────────────────────────────
        String allergies = s.getAllergies();
        if (!allergies.equalsIgnoreCase("none") && !allergies.isEmpty()) {
            LinearLayout allergyBanner = new LinearLayout(this);
            allergyBanner.setOrientation(LinearLayout.VERTICAL);
            allergyBanner.setBackgroundColor(Color.parseColor("#FCEBEB"));
            allergyBanner.setPadding(dp(14), dp(10), dp(14), dp(10));
            LinearLayout.LayoutParams alLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            alLp.setMargins(0, 0, 0, dp(12));
            allergyBanner.setLayoutParams(alLp);

            TextView tvAW = new TextView(this);
            tvAW.setText("⚠  Known Allergies");
            tvAW.setTextColor(Color.parseColor("#A32D2D"));
            tvAW.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAW.setTextSize(12);
            allergyBanner.addView(tvAW);

            TextView tvAV = new TextView(this);
            tvAV.setText(allergies + " — flagged from Clinic NFC record");
            tvAV.setTextColor(Color.parseColor("#A32D2D"));
            tvAV.setTextSize(11);
            allergyBanner.addView(tvAV);

            contentArea.addView(allergyBanner);
        }

        // ── Active prescriptions ──────────────────────────────────────
        LinearLayout activeCard = makeCard();
        activeCard.addView(makeSectionLabel("💊  ACTIVE PRESCRIPTIONS"));

        try {
            int count = 0;
            for (int i = 0; i < meds.length(); i++) {
                JSONObject m = meds.getJSONObject(i);
                if (m.getString("status").equals("active")) {
                    activeCard.addView(makeMedBlock(m, true));
                    count++;
                }
            }
            if (count == 0) activeCard.addView(makeEmpty("No active prescriptions"));
        } catch (JSONException e) { e.printStackTrace(); }

        contentArea.addView(activeCard);

        // ── Completed ─────────────────────────────────────────────────
        LinearLayout doneCard = makeCard();
        doneCard.addView(makeSectionLabel("✓  COMPLETED"));

        try {
            int count = 0;
            for (int i = 0; i < meds.length(); i++) {
                JSONObject m = meds.getJSONObject(i);
                if (m.getString("status").equals("completed")) {
                    doneCard.addView(makeMedRow(m));
                    count++;
                }
            }
            if (count == 0) doneCard.addView(makeEmpty("No completed medications"));
        } catch (JSONException e) { e.printStackTrace(); }

        contentArea.addView(doneCard);
    }

    // Full block with progress bar (active meds)
    private View makeMedBlock(JSONObject m, boolean active) throws JSONException {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setBackgroundColor(Color.parseColor("#FAF9F7"));
        block.setPadding(dp(12), dp(10), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        block.setLayoutParams(lp);

        // Top row: name + badge
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(m.getString("name"));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextSize(13);
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        topRow.addView(tvName);
        topRow.addView(makeBadge("Active", "#FAEEDA", "#854F0B"));
        block.addView(topRow);

        // Dose / frequency / route
        TextView tvInfo = new TextView(this);
        tvInfo.setText(m.getString("dosage") + "  ·  " + m.getString("frequency") +
                       "  ·  " + m.getString("route"));
        tvInfo.setTextSize(11);
        tvInfo.setTextColor(Color.parseColor("#666666"));
        tvInfo.setPadding(0, dp(3), 0, dp(8));
        block.addView(tvInfo);

        // Supply progress
        int total = m.getInt("supply_total");
        int remaining = m.getInt("supply_remaining");

        LinearLayout progLabelRow = new LinearLayout(this);
        progLabelRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView tvProgL = new TextView(this);
        tvProgL.setText("Supply");
        tvProgL.setTextSize(10);
        tvProgL.setTextColor(Color.parseColor("#888888"));
        tvProgL.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        progLabelRow.addView(tvProgL);
        TextView tvProgR = new TextView(this);
        tvProgR.setText(remaining + " / " + total + " remaining");
        tvProgR.setTextSize(10);
        tvProgR.setTextColor(Color.parseColor("#888888"));
        progLabelRow.addView(tvProgR);
        block.addView(progLabelRow);

        // Progress bar track
        FrameLayout track = new FrameLayout(this);
        track.setBackgroundColor(Color.parseColor("#EEEEEE"));
        LinearLayout.LayoutParams trackLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
        trackLp.setMargins(0, dp(4), 0, dp(10));
        track.setLayoutParams(trackLp);

        View fill = new View(this);
        fill.setBackgroundColor(Color.parseColor("#BA7517"));
        int fillPct = total > 0 ? (int)((remaining / (float) total) * 100) : 0;
        FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(0, dp(6));
        fill.setLayoutParams(fillLp);
        track.addView(fill);
        block.addView(track);

        // Post-layout fill width
        track.post(() -> {
            int trackW = track.getWidth();
            FrameLayout.LayoutParams fl = (FrameLayout.LayoutParams) fill.getLayoutParams();
            fl.width = (int)(trackW * fillPct / 100f);
            fill.setLayoutParams(fl);
        });

        // Tags row
        LinearLayout tags = new LinearLayout(this);
        tags.setOrientation(LinearLayout.HORIZONTAL);
        tags.addView(makeTag("Prescribed: " + m.getString("prescribed_date")));
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagLp.setMarginStart(dp(6));
        TextView docTag = makeTag(m.getString("doctor"));
        docTag.setLayoutParams(tagLp);
        tags.addView(docTag);
        block.addView(tags);

        return block;
    }

    // Simple row for completed meds
    private View makeMedRow(JSONObject m) throws JSONException {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#F5F5F5"));
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(this);
        tvName.setText(m.getString("name"));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextSize(12);
        tvName.setTextColor(Color.parseColor("#888888"));
        info.addView(tvName);

        TextView tvDetail = new TextView(this);
        tvDetail.setText(m.getString("dosage") + " · " + m.getString("frequency") +
                         " · " + m.getString("prescribed_date"));
        tvDetail.setTextSize(10);
        tvDetail.setTextColor(Color.parseColor("#AAAAAA"));
        info.addView(tvDetail);

        row.addView(info);
        row.addView(makeBadge("Done", "#EAF3DE", "#3B6D11"));
        return row;
    }

    private TextView makeTag(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setBackgroundColor(Color.parseColor("#FAEEDA"));
        tv.setTextColor(Color.parseColor("#633806"));
        tv.setTextSize(9);
        tv.setPadding(dp(8), dp(3), dp(8), dp(3));
        return tv;
    }

    private TextView makeEmpty(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(Color.parseColor("#AAAAAA"));
        tv.setTextSize(12);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }
}
