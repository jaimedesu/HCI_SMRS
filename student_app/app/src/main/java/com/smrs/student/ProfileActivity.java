package com.smrs.student;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class ProfileActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StudentSession s = StudentSession.get();
        if (!s.isLoaded() && !s.loadFromStorage(this)) {
            startActivity(new Intent(this, NfcTapActivity.class));
            finish();
            return;
        }
        buildPage("My Profile", null);

        // ── Avatar + name header card ─────────────────────────────────
        LinearLayout avatarCard = makeCard();
        avatarCard.setOrientation(LinearLayout.HORIZONTAL);
        avatarCard.setGravity(Gravity.CENTER_VERTICAL);

        // Avatar circle
        TextView avatar = new TextView(this);
        String name = s.getFullName();
        String[] parts = name.split(" ");
        String initials = parts.length >= 2
            ? String.valueOf(parts[0].charAt(0)) + parts[parts.length - 1].charAt(0)
            : name.substring(0, Math.min(2, name.length()));
        avatar.setText(initials.toUpperCase());
        avatar.setTextColor(Color.WHITE);
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.setTextSize(18);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundColor(Color.parseColor("#1565C0"));
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(56), dp(56));
        avLp.setMarginEnd(dp(14));
        avatar.setLayoutParams(avLp);
        avatarCard.addView(avatar);

        // Info column
        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(16);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#1A1A1A"));
        infoCol.addView(tvName);

        TextView tvNo = new TextView(this);
        tvNo.setText("Student No. " + s.getStudentNo());
        tvNo.setTextSize(11);
        tvNo.setTextColor(Color.parseColor("#888888"));
        tvNo.setPadding(0, dp(2), 0, dp(6));
        infoCol.addView(tvNo);

        LinearLayout tagRow = new LinearLayout(this);
        tagRow.setOrientation(LinearLayout.HORIZONTAL);
        tagRow.addView(makeBadge(s.getProgram(), "#E6F1FB", "#185FA5"));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tLp.setMarginStart(dp(6));
        TextView yrBadge = makeBadge(s.getYearLevel(), "#E6F1FB", "#185FA5");
        yrBadge.setLayoutParams(tLp);
        tagRow.addView(yrBadge);
        infoCol.addView(tagRow);

        avatarCard.addView(infoCol);
        contentArea.addView(avatarCard);

        // ── NFC read note ────────────────────────────────────────────
        LinearLayout nfcNote = new LinearLayout(this);
        nfcNote.setBackgroundColor(Color.parseColor("#E6F1FB"));
        nfcNote.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteLp.setMargins(0, 0, 0, dp(12));
        nfcNote.setLayoutParams(noteLp);
        TextView tvNote = new TextView(this);
        tvNote.setText("📡  All data received from Clinic NFC Terminal · Read-only");
        tvNote.setTextColor(Color.parseColor("#185FA5"));
        tvNote.setTextSize(10);
        tvNote.setTypeface(null, android.graphics.Typeface.BOLD);
        nfcNote.addView(tvNote);
        contentArea.addView(nfcNote);

        // ── Stats grid ───────────────────────────────────────────────
        LinearLayout statsRow1 = new LinearLayout(this);
        statsRow1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams srLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        srLp.setMargins(0, 0, 0, dp(8));
        statsRow1.setLayoutParams(srLp);
        statsRow1.addView(makeStatBox("Blood Type", s.getBloodType(), "#1565C0"));
        statsRow1.addView(makeStatBox("BMI", s.getBmi(), "#1A1A1A"));
        contentArea.addView(statsRow1);

        LinearLayout statsRow2 = new LinearLayout(this);
        statsRow2.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams sr2Lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sr2Lp.setMargins(0, 0, 0, dp(12));
        statsRow2.setLayoutParams(sr2Lp);
        statsRow2.addView(makeStatBox("Weight", s.getWeight(), "#1A1A1A"));
        statsRow2.addView(makeStatBox("Height", s.getHeight(), "#1A1A1A"));
        contentArea.addView(statsRow2);

        // ── Personal details card ─────────────────────────────────────
        LinearLayout personalCard = makeCard();
        LinearLayout personalHdr = new LinearLayout(this);
        personalHdr.setOrientation(LinearLayout.HORIZONTAL);
        personalHdr.setGravity(Gravity.CENTER_VERTICAL);
        personalHdr.setPadding(0, 0, 0, dp(8));
        TextView tvPH = new TextView(this);
        tvPH.setText("👤  PERSONAL DETAILS");
        tvPH.setTextColor(Color.parseColor("#1565C0"));
        tvPH.setTextSize(10);
        tvPH.setTypeface(null, android.graphics.Typeface.BOLD);
        tvPH.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        personalHdr.addView(tvPH);
        TextView tvNfcTag = makeBadge("NFC Read", "#E6F1FB", "#185FA5");
        personalHdr.addView(tvNfcTag);
        personalCard.addView(personalHdr);

        personalCard.addView(makeDivider());
        personalCard.addView(makeInfoRow("Full Name", s.getFullName()));
        personalCard.addView(makeDivider());
        personalCard.addView(makeInfoRow("Date of Birth", s.getDob()));
        personalCard.addView(makeDivider());
        personalCard.addView(makeInfoRow("Sex", s.getSex()));
        personalCard.addView(makeDivider());
        personalCard.addView(makeInfoRow("Contact", s.getContact(), "#185FA5"));
        personalCard.addView(makeDivider());
        personalCard.addView(makeInfoRow("Emergency Contact", s.getEmergency(), "#185FA5"));
        contentArea.addView(personalCard);

        // ── Medical details card ──────────────────────────────────────
        LinearLayout medCard = makeCard();
        LinearLayout medHdr = new LinearLayout(this);
        medHdr.setOrientation(LinearLayout.HORIZONTAL);
        medHdr.setGravity(Gravity.CENTER_VERTICAL);
        medHdr.setPadding(0, 0, 0, dp(8));
        TextView tvMH = new TextView(this);
        tvMH.setText("🩺  MEDICAL DETAILS");
        tvMH.setTextColor(Color.parseColor("#A32D2D"));
        tvMH.setTextSize(10);
        tvMH.setTypeface(null, android.graphics.Typeface.BOLD);
        tvMH.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        medHdr.addView(tvMH);
        TextView tvMedTag = makeBadge("NFC Read", "#FCEBEB", "#A32D2D");
        medHdr.addView(tvMedTag);
        medCard.addView(medHdr);

        medCard.addView(makeDivider());
        medCard.addView(makeInfoRow("Allergies", s.getAllergies(), "#A32D2D"));
        medCard.addView(makeDivider());
        medCard.addView(makeInfoRow("Conditions", s.getConditions()));
        medCard.addView(makeDivider());
        medCard.addView(makeInfoRow("Surgeries", s.getSurgeries()));
        medCard.addView(makeDivider());

        String vacc = s.getVaccinations();
        String vaccColor = vacc.toLowerCase().contains("up") ? "#0F6E56" : "#A32D2D";
        medCard.addView(makeInfoRow("Vaccinations", vacc, vaccColor));
        contentArea.addView(medCard);
    }
}
