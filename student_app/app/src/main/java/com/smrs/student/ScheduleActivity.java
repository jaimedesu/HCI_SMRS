package com.smrs.student;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.*;

public class ScheduleActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StudentSession s = StudentSession.get();
        if (!s.isLoaded() && !s.loadFromStorage(this)) {
            startActivity(new Intent(this, NfcTapActivity.class));
            finish();
            return;
        }
        buildPage("Appointments", "Your visit schedule");

        JSONArray appts = s.getAppointments();

        // ── Upcoming ─────────────────────────────────────────────────
        LinearLayout upCard = makeCard();
        upCard.addView(makeSectionLabel("📅  UPCOMING"));
        try {
            int count = 0;
            for (int i = 0; i < appts.length(); i++) {
                JSONObject a = appts.getJSONObject(i);
                String st = a.getString("status");
                if (st.equals("upcoming") || st.equals("pending")) {
                    upCard.addView(makeApptRow(a, true));
                    count++;
                }
            }
            if (count == 0) upCard.addView(makeEmpty("No upcoming appointments"));
        } catch (JSONException e) { e.printStackTrace(); }
        contentArea.addView(upCard);

        // ── Completed ────────────────────────────────────────────────
        LinearLayout doneCard = makeCard();
        doneCard.addView(makeSectionLabel("✓  COMPLETED"));
        try {
            int count = 0;
            for (int i = 0; i < appts.length(); i++) {
                JSONObject a = appts.getJSONObject(i);
                if (a.getString("status").equals("completed")) {
                    doneCard.addView(makeApptRow(a, false));
                    count++;
                }
            }
            if (count == 0) doneCard.addView(makeEmpty("No completed appointments"));
        } catch (JSONException e) { e.printStackTrace(); }
        contentArea.addView(doneCard);
    }

    private View makeApptRow(JSONObject a, boolean upcoming) throws JSONException {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor(upcoming ? "#F7F9FC" : "#F9F9F9"));
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);

        // Date block
        String dateBg  = upcoming ? "#E6F1FB" : "#F1EFE8";
        String monthFg = upcoming ? "#185FA5" : "#5F5E5A";
        String dayFg   = upcoming ? "#1565C0" : "#666666";

        LinearLayout dateBlk = new LinearLayout(this);
        dateBlk.setOrientation(LinearLayout.VERTICAL);
        dateBlk.setBackgroundColor(Color.parseColor(dateBg));
        dateBlk.setGravity(Gravity.CENTER);
        dateBlk.setPadding(dp(8), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        dbLp.setMarginEnd(dp(10));
        dateBlk.setLayoutParams(dbLp);

        String[] parts = a.getString("date").split(" ");
        String month = parts.length > 0 ? parts[0].substring(0, Math.min(3, parts[0].length())).toUpperCase() : "";
        String day   = parts.length > 1 ? parts[1].replace(",", "") : "";

        TextView tvM = new TextView(this); tvM.setText(month);
        tvM.setTextSize(8); tvM.setTextColor(Color.parseColor(monthFg));
        tvM.setTypeface(null, android.graphics.Typeface.BOLD);
        tvM.setGravity(Gravity.CENTER);
        dateBlk.addView(tvM);

        TextView tvD = new TextView(this); tvD.setText(day);
        tvD.setTextSize(16); tvD.setTextColor(Color.parseColor(dayFg));
        tvD.setTypeface(null, android.graphics.Typeface.BOLD);
        tvD.setGravity(Gravity.CENTER);
        dateBlk.addView(tvD);
        row.addView(dateBlk);

        // Info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tvType = new TextView(this);
        tvType.setText(a.getString("type"));
        tvType.setTypeface(null, android.graphics.Typeface.BOLD);
        tvType.setTextSize(13);
        tvType.setTextColor(Color.parseColor(upcoming ? "#1A1A1A" : "#888888"));
        info.addView(tvType);

        TextView tvDoc = new TextView(this);
        tvDoc.setText(a.getString("doctor") + " · " + a.getString("time"));
        tvDoc.setTextSize(11);
        tvDoc.setTextColor(Color.parseColor("#888888"));
        info.addView(tvDoc);

        TextView tvLoc = new TextView(this);
        tvLoc.setText(a.getString("location"));
        tvLoc.setTextSize(10);
        tvLoc.setTextColor(Color.parseColor("#AAAAAA"));
        info.addView(tvLoc);
        row.addView(info);

        // Status badge
        String status = a.getString("status");
        String[] badgeColors = statusBadge(status);
        row.addView(makeBadge(status, badgeColors[0], badgeColors[1]));

        return row;
    }

    private String[] statusBadge(String status) {
        switch (status) {
            case "upcoming":  return new String[]{"#E1F5EE", "#0F6E56"};
            case "pending":   return new String[]{"#E6F1FB", "#185FA5"};
            case "completed": return new String[]{"#EAF3DE", "#3B6D11"};
            default:          return new String[]{"#EEEEEE", "#888888"};
        }
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
