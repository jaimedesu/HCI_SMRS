package com.smrs.student;
// HomeActivity.java
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import org.json.*;

public class HomeActivity extends BaseActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        StudentSession s = StudentSession.get();
        if (!s.isLoaded() && !s.loadFromStorage(this)) { startActivity(new Intent(this, NfcTapActivity.class)); finish(); return; }
        buildPage("Student Medical Records", null);

        // Student card
        LinearLayout studentCard = new LinearLayout(this);
        studentCard.setOrientation(LinearLayout.HORIZONTAL);
        studentCard.setBackgroundColor(Color.parseColor("#1251A3"));
        studentCard.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scLp.setMargins(0, 0, 0, dp(12));
        studentCard.setLayoutParams(scLp);

        // Avatar
        TextView avatar = new TextView(this);
        String initials = s.getFullName().length() >= 2 ?
            s.getFullName().substring(0,1) + s.getFullName().split(" ")[s.getFullName().split(" ").length-1].substring(0,1) : "JA";
        avatar.setText(initials);
        avatar.setTextColor(Color.WHITE);
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.setTextSize(16);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundColor(Color.parseColor("#1E5FBE"));
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        avLp.setMarginEnd(dp(12));
        avatar.setLayoutParams(avLp);
        studentCard.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this); tvName.setText(s.getFullName());
        tvName.setTextColor(Color.WHITE); tvName.setTextSize(15);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        info.addView(tvName);

        TextView tvNo = new TextView(this); tvNo.setText("Stu. No. " + s.getStudentNo());
        tvNo.setTextColor(Color.parseColor("#90BBEE")); tvNo.setTextSize(11);
        info.addView(tvNo);

        LinearLayout tags = new LinearLayout(this); tags.setOrientation(LinearLayout.HORIZONTAL);
        tags.setPadding(0, dp(4), 0, 0);
        tags.addView(makeBadge(s.getProgram(), "#1A4A9A", "#A0D0FF"));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.setMarginStart(dp(6));
        TextView yr = makeBadge(s.getYearLevel(), "#1A4A9A", "#A0D0FF"); yr.setLayoutParams(tLp);
        tags.addView(yr);
        info.addView(tags);
        studentCard.addView(info);
        contentArea.addView(studentCard);

        // Patient Info card
        LinearLayout piCard = makeCard();
        piCard.addView(makeSectionLabel("📋  PATIENT INFORMATION"));
        LinearLayout statRow1 = new LinearLayout(this); statRow1.setOrientation(LinearLayout.HORIZONTAL);
        statRow1.addView(makeStatBox("Blood Type", s.getBloodType(), "#1565C0"));
        statRow1.addView(makeStatBox("BMI", s.getBmi(), "#1A1A1A"));
        LinearLayout.LayoutParams sr1Lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sr1Lp.setMargins(0, 0, 0, dp(8));
        statRow1.setLayoutParams(sr1Lp);
        piCard.addView(statRow1);
        LinearLayout statRow2 = new LinearLayout(this); statRow2.setOrientation(LinearLayout.HORIZONTAL);
        statRow2.addView(makeStatBox("Weight", s.getWeight(), "#1A1A1A"));
        statRow2.addView(makeStatBox("Height", s.getHeight(), "#1A1A1A"));
        piCard.addView(statRow2);
        contentArea.addView(piCard);

        // Appointments preview
        LinearLayout apCard = makeCard();
        apCard.addView(makeSectionLabel("📅  UPCOMING APPOINTMENTS"));
        try {
            JSONArray appts = s.getAppointments();
            int shown = 0;
            for (int i = 0; i < appts.length() && shown < 2; i++) {
                JSONObject a = appts.getJSONObject(i);
                if (!a.getString("status").equals("upcoming")) continue;
                apCard.addView(makeApptRow(a)); shown++;
            }
            if (shown == 0) { TextView e = new TextView(this); e.setText("No upcoming appointments"); e.setTextColor(Color.parseColor("#888888")); apCard.addView(e); }
        } catch (Exception e) { e.printStackTrace(); }
        contentArea.addView(apCard);

        // Medications preview
        LinearLayout medCard = makeCard();
        medCard.addView(makeSectionLabel("💊  ACTIVE MEDICATIONS"));
        try {
            JSONArray meds = s.getMedications();
            int shown = 0;
            for (int i = 0; i < meds.length() && shown < 2; i++) {
                JSONObject m = meds.getJSONObject(i);
                if (!m.getString("status").equals("active")) continue;
                medCard.addView(makeMedRow(m)); shown++;
            }
        } catch (Exception e) { e.printStackTrace(); }
        contentArea.addView(medCard);
    }

    private View makeApptRow(JSONObject a) throws JSONException {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#F7F9FC"));
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);

        LinearLayout dateBlk = new LinearLayout(this);
        dateBlk.setOrientation(LinearLayout.VERTICAL);
        dateBlk.setBackgroundColor(Color.parseColor("#E6F1FB"));
        dateBlk.setGravity(Gravity.CENTER);
        dateBlk.setPadding(dp(8), dp(4), dp(8), dp(4));
        LinearLayout.LayoutParams dbLp = new LinearLayout.LayoutParams(dp(44), dp(44));
        dbLp.setMarginEnd(dp(10));
        dateBlk.setLayoutParams(dbLp);
        String[] parts = a.getString("date").split(" ");
        TextView tvMo = new TextView(this); tvMo.setText(parts.length > 0 ? parts[0].toUpperCase().substring(0,3) : ""); tvMo.setTextSize(8); tvMo.setTextColor(Color.parseColor("#185FA5")); tvMo.setGravity(Gravity.CENTER); tvMo.setTypeface(null, android.graphics.Typeface.BOLD); dateBlk.addView(tvMo);
        TextView tvDy = new TextView(this); tvDy.setText(parts.length > 1 ? parts[1].replace(",","") : ""); tvDy.setTextSize(15); tvDy.setTextColor(Color.parseColor("#1565C0")); tvDy.setGravity(Gravity.CENTER); tvDy.setTypeface(null, android.graphics.Typeface.BOLD); dateBlk.addView(tvDy);
        row.addView(dateBlk);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView tvType = new TextView(this); tvType.setText(a.getString("type")); tvType.setTypeface(null, android.graphics.Typeface.BOLD); tvType.setTextSize(12); tvType.setTextColor(Color.parseColor("#1A1A1A")); info.addView(tvType);
        TextView tvDoc = new TextView(this); tvDoc.setText(a.getString("doctor") + " · " + a.getString("time")); tvDoc.setTextSize(10); tvDoc.setTextColor(Color.parseColor("#888888")); info.addView(tvDoc);
        row.addView(info);
        return row;
    }

    private View makeMedRow(JSONObject m) throws JSONException {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.parseColor("#FAF9F7"));
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setLayoutParams(infoLp);
        TextView tvName = new TextView(this); tvName.setText(m.getString("name")); tvName.setTypeface(null, android.graphics.Typeface.BOLD); tvName.setTextSize(12); tvName.setTextColor(Color.parseColor("#1A1A1A")); info.addView(tvName);
        TextView tvDose = new TextView(this); tvDose.setText(m.getString("dosage") + " · " + m.getString("frequency")); tvDose.setTextSize(10); tvDose.setTextColor(Color.parseColor("#888888")); info.addView(tvDose);
        row.addView(info);
        row.addView(makeBadge("Active", "#FAEEDA", "#854F0B"));
        return row;
    }
}
