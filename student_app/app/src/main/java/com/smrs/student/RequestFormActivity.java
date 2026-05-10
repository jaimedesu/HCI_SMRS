package com.smrs.student;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestFormActivity extends BaseActivity {

    // Change to your backend IP
    private static final String BACKEND_URL = "http://192.168.86.141:8000";

    private Spinner spinRecordType;
    private EditText etPurpose, etDateNeeded;
    private Button   btnClinicPickup, btnEmail, btnSubmit;
    private TextView tvResult;
    private ProgressBar progressBar;
    private String selectedRelease = "Clinic Pick-up";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StudentSession s = StudentSession.get();
        if (!s.isLoaded() && !s.loadFromStorage(this)) {
            startActivity(new Intent(this, NfcTapActivity.class));
            finish();
            return;
        }
        buildPage("Request Medical Record", "Fill out the form below");
        buildForm(s);
    }

    private void buildForm(StudentSession s) {
        // ── NFC auto-fill banner ──────────────────────────────────────
        LinearLayout nfcBanner = new LinearLayout(this);
        nfcBanner.setOrientation(LinearLayout.VERTICAL);
        nfcBanner.setBackgroundColor(Color.parseColor("#E6F1FB"));
        nfcBanner.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams banLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        banLp.setMargins(0, 0, 0, dp(12));
        nfcBanner.setLayoutParams(banLp);

        TextView tvBanTitle = new TextView(this);
        tvBanTitle.setText("📡  Student info received from Clinic NFC Terminal");
        tvBanTitle.setTextColor(Color.parseColor("#1565C0"));
        tvBanTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvBanTitle.setTextSize(11);
        nfcBanner.addView(tvBanTitle);

        TextView tvBanSub = new TextView(this);
        tvBanSub.setText("Fields marked 🔒 are pre-filled and read-only.");
        tvBanSub.setTextColor(Color.parseColor("#185FA5"));
        tvBanSub.setTextSize(10);
        nfcBanner.addView(tvBanSub);
        contentArea.addView(nfcBanner);

        // ── STUDENT INFORMATION (locked) ──────────────────────────────
        LinearLayout stuCard = makeCard();
        stuCard.addView(makeSectionLabel("🔒  STUDENT INFORMATION (NFC)"));

        stuCard.addView(makeLockedField("Full Name", s.getFullName()));
        stuCard.addView(makeLockedField("Student Number", s.getStudentNo()));

        LinearLayout twoCol = new LinearLayout(this);
        twoCol.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams twoLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        twoLp.setMargins(0, 0, 0, dp(4));
        twoCol.setLayoutParams(twoLp);

        LinearLayout progCol = new LinearLayout(this);
        progCol.setOrientation(LinearLayout.VERTICAL);
        progCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        progCol.addView(makeFieldLabel("Program 🔒"));
        progCol.addView(makeLockedInput(s.getProgram()));
        LinearLayout.LayoutParams progLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        progLp.setMarginEnd(dp(8));
        progCol.setLayoutParams(progLp);
        twoCol.addView(progCol);

        LinearLayout yrCol = new LinearLayout(this);
        yrCol.setOrientation(LinearLayout.VERTICAL);
        yrCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        yrCol.addView(makeFieldLabel("Year Level 🔒"));
        yrCol.addView(makeLockedInput(s.getYearLevel()));
        twoCol.addView(yrCol);
        stuCard.addView(twoCol);
        contentArea.addView(stuCard);

        // ── RECORD DETAILS ────────────────────────────────────────────
        LinearLayout detCard = makeCard();
        detCard.addView(makeSectionLabel("📋  RECORD DETAILS"));

        // Record type spinner
        detCard.addView(makeFieldLabel("Type of Record Requested"));
        spinRecordType = new Spinner(this);
        String[] types = {"Medical Certificate", "Health Clearance",
                          "Lab Results", "Full Medical Records"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinRecordType.setAdapter(adapter);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spinLp.setMargins(0, dp(4), 0, dp(12));
        spinRecordType.setLayoutParams(spinLp);
        detCard.addView(spinRecordType);

        // Purpose
        detCard.addView(makeFieldLabel("Purpose / Reason"));
        etPurpose = new EditText(this);
        etPurpose.setHint("e.g. Scholarship application");
        styleEditText(etPurpose);
        detCard.addView(etPurpose);

        // Date needed
        detCard.addView(makeFieldLabel("Date Needed"));
        etDateNeeded = new EditText(this);
        etDateNeeded.setHint("e.g. May 10, 2025");
        styleEditText(etDateNeeded);
        detCard.addView(etDateNeeded);

        // Mode of release toggle
        detCard.addView(makeFieldLabel("Mode of Release"));
        LinearLayout releaseRow = new LinearLayout(this);
        releaseRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rrLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rrLp.setMargins(0, dp(4), 0, dp(4));
        releaseRow.setLayoutParams(rrLp);

        btnClinicPickup = new Button(this);
        btnClinicPickup.setText("🏥 Clinic Pick-up");
        styleReleaseBtn(btnClinicPickup, true);
        btnClinicPickup.setOnClickListener(v -> setRelease("Clinic Pick-up"));

        btnEmail = new Button(this);
        btnEmail.setText("✉ Email");
        styleReleaseBtn(btnEmail, false);
        LinearLayout.LayoutParams emailLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        emailLp.setMarginStart(dp(8));
        btnEmail.setLayoutParams(emailLp);
        btnEmail.setOnClickListener(v -> setRelease("Email"));

        releaseRow.addView(btnClinicPickup);
        releaseRow.addView(btnEmail);
        detCard.addView(releaseRow);
        contentArea.addView(detCard);

        // ── Result message ────────────────────────────────────────────
        tvResult = new TextView(this);
        tvResult.setVisibility(View.GONE);
        tvResult.setPadding(dp(14), dp(10), dp(14), dp(10));
        tvResult.setTextSize(12);
        LinearLayout.LayoutParams resLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        resLp.setMargins(0, 0, 0, dp(10));
        tvResult.setLayoutParams(resLp);
        contentArea.addView(tvResult);

        // ── Progress bar ─────────────────────────────────────────────
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        contentArea.addView(progressBar);

        // ── Submit button ─────────────────────────────────────────────
        btnSubmit = new Button(this);
        btnSubmit.setText("📡  Submit via NFC Session");
        btnSubmit.setBackgroundColor(Color.parseColor("#1565C0"));
        btnSubmit.setTextColor(Color.WHITE);
        btnSubmit.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSubmit.setTextSize(14);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        btnLp.setMargins(0, dp(4), 0, dp(10));
        btnSubmit.setLayoutParams(btnLp);
        btnSubmit.setOnClickListener(v -> submitRequest(s));
        contentArea.addView(btnSubmit);

        // Confirmation note
        TextView tvNote = new TextView(this);
        tvNote.setText("You will receive confirmation via SMS / email.\nProcessing time: 3–5 business days.");
        tvNote.setTextColor(Color.parseColor("#888888"));
        tvNote.setTextSize(10);
        tvNote.setGravity(Gravity.CENTER);
        tvNote.setPadding(dp(16), 0, dp(16), dp(16));
        contentArea.addView(tvNote);
    }

    private void setRelease(String mode) {
        selectedRelease = mode;
        styleReleaseBtn(btnClinicPickup, mode.equals("Clinic Pick-up"));
        styleReleaseBtn(btnEmail, mode.equals("Email"));
    }

    private void styleReleaseBtn(Button btn, boolean active) {
        if (active) {
            btn.setBackgroundColor(Color.parseColor("#1565C0"));
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundColor(Color.parseColor("#F0F0F0"));
            btn.setTextColor(Color.parseColor("#555555"));
        }
    }

    private void submitRequest(StudentSession s) {
        String purpose = etPurpose.getText().toString().trim();
        String dateNeeded = etDateNeeded.getText().toString().trim();
        String recordType = spinRecordType.getSelectedItem().toString();

        if (purpose.isEmpty() || dateNeeded.isEmpty()) {
            showResult("Please fill in all fields.", "#FCEBEB", "#A32D2D");
            return;
        }

        btnSubmit.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvResult.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("student_no", s.getStudentNo());
                payload.put("record_type", recordType);
                payload.put("purpose", purpose);
                payload.put("date_needed", dateNeeded);
                payload.put("mode_of_release", selectedRelease);

                String body = payload.toString();
                URL url = new URL(BACKEND_URL + "/record-requests");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code == 200 || code == 201) {
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        showResult("✅  Request submitted successfully!\n" +
                                   "Record type: " + recordType + "\n" +
                                   "Release mode: " + selectedRelease + "\n" +
                                   "Processing time: 3–5 business days.",
                            "#E1F5EE", "#0F6E56");
                        etPurpose.setText("");
                        etDateNeeded.setText("");

                        // Add a button to go to history
                        Button btnHistory = new Button(this);
                        btnHistory.setText("View Request History");
                        btnHistory.setAllCaps(false);
                        btnHistory.setOnClickListener(v2 -> {
                            startActivity(new Intent(this, RequestHistoryActivity.class));
                        });
                        contentArea.addView(btnHistory);
                    });
                } else {
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSubmit.setEnabled(true);
                        showResult("❌  Server error (HTTP " + code + "). Please try again.",
                            "#FCEBEB", "#A32D2D");
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    showResult("❌  Network error: " + e.getMessage() +
                               "\nMake sure the backend is running.",
                        "#FCEBEB", "#A32D2D");
                });
            }
        });
    }

    private void showResult(String msg, String bg, String fg) {
        tvResult.setText(msg);
        tvResult.setBackgroundColor(Color.parseColor(bg));
        tvResult.setTextColor(Color.parseColor(fg));
        tvResult.setVisibility(View.VISIBLE);
    }

    // ── Locked (read-only) field helpers ─────────────────────────────

    private View makeLockedField(String label, String value) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        col.setLayoutParams(lp);
        col.addView(makeFieldLabel(label + " 🔒"));
        col.addView(makeLockedInput(value));
        return col;
    }

    private TextView makeLockedInput(String value) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setBackgroundColor(Color.parseColor("#F0F6FF"));
        tv.setTextColor(Color.parseColor("#1565C0"));
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextSize(13);
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        return tv;
    }

    private TextView makeFieldLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#555555"));
        tv.setTextSize(11);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        tv.setLayoutParams(lp);
        return tv;
    }

    private void styleEditText(EditText et) {
        et.setBackgroundColor(Color.parseColor("#F7F9FC"));
        et.setTextColor(Color.parseColor("#1A1A1A"));
        et.setTextSize(13);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        et.setLayoutParams(lp);
    }
}
