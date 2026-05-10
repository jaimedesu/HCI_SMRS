package com.smrs.clinic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainActivity — Clinic Terminal UI
 *
 * Fixed for Android 14:
 * - All network calls on background thread (ExecutorService)
 * - UI updates only on main thread via Handler
 * - No StrictMode violations
 * - Proper NFC availability check
 * - Error handling with AlertDialogs instead of silent crashes
 */
public class MainActivity extends Activity {

    private static final String TAG = "ClinicTerminal";

    // !! CHANGE THIS to your backend server IP !!
    private static final String BACKEND_URL = "http://192.168.86.141:8000";

    // UI refs
    private EditText    etStudentNo;
    private Button      btnLoad;
    private Button      btnClear;
    private TextView    tvStatus;
    private TextView    tvStudentName;
    private TextView    tvStudentDetails;
    private LinearLayout llStudentCard;
    private LinearLayout llReadyBanner;
    private LinearLayout llAppointmentsList;
    private LinearLayout llMedsList;
    private ProgressBar  progressBar;

    // Threading
    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    // ── Lifecycle ─────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on while broadcasting NFC
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Check NFC availability before anything else
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc == null) {
            showFatalDialog("NFC Not Supported",
                "This device does not support NFC. " +
                "The clinic terminal requires NFC hardware.");
            return;
        }
        if (!nfc.isEnabled()) {
            showFatalDialog("NFC Disabled",
                "Please enable NFC in Settings → Connections → NFC, " +
                "then reopen the app.");
            return;
        }

        buildUI();
        setStatus("📡  Ready — enter a student number to begin",
                  "#E6F1FB", "#185FA5");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        NfcHceService.clearPayload();
    }

    // ── Network: load student from backend ───────────────────────────

    /**
     * Runs on the background thread — fetches the full NFC payload JSON
     * from the Python backend, then updates UI on the main thread.
     *
     * Android 14 will crash with NetworkOnMainThreadException
     * if you try to do any network call on the main thread.
     */
    private void loadStudent() {
        String sno = etStudentNo.getText().toString().trim();
        if (sno.isEmpty()) {
            toast("Please enter a student number");
            return;
        }

        // Disable button, show spinner
        btnLoad.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        llStudentCard.setVisibility(View.GONE);
        llReadyBanner.setVisibility(View.GONE);
        NfcHceService.clearPayload();
        setStatus("⏳  Fetching records for " + sno + "…", "#E6F1FB", "#185FA5");

        // All network work on background thread
        executor.execute(() -> {
            try {
                String url  = BACKEND_URL + "/nfc-payload/" + sno;
                String json = httpGet(url);

                // Parse to validate and extract display info
                JSONObject root    = new JSONObject(json);
                JSONObject student = root.getJSONObject("student");
                JSONArray  appts   = root.getJSONArray("appointments");
                JSONArray  meds    = root.getJSONArray("medications");

                // Set payload in HCE service BEFORE updating UI
                NfcHceService.setPayload(json);

                // Post UI update to main thread
                mainHandler.post(() -> onStudentLoaded(student, appts, meds, json.length()));

            } catch (java.io.FileNotFoundException e) {
                mainHandler.post(() -> onError("Student not found. " +
                    "Check the student number and try again."));
            } catch (java.net.ConnectException e) {
                mainHandler.post(() -> onError(
                    "Cannot reach backend at " + BACKEND_URL + ".\n\n" +
                    "Check:\n• Python server is running (python main.py)\n" +
                    "• Both devices are on the same Wi-Fi\n" +
                    "• BACKEND_URL in MainActivity.java is correct"));
            } catch (Exception e) {
                Log.e(TAG, "Load error", e);
                mainHandler.post(() -> onError("Error: " + e.getMessage()));
            }
        });
    }

    // Called on main thread after successful fetch
    private void onStudentLoaded(JSONObject student, JSONArray appts,
                                  JSONArray meds, int payloadSize) {
        try {
            progressBar.setVisibility(View.GONE);
            btnLoad.setEnabled(true);

            String name = student.optString("full_name", "Unknown");
            String sno  = student.optString("student_no", "");
            String prog = student.optString("program", "") +
                          " · " + student.optString("year_level", "");
            String bt   = student.optString("blood_type", "");
            String allg = student.optString("allergies", "None");

            // Student card
            tvStudentName.setText(name);
            tvStudentDetails.setText(
                "No. " + sno + "  ·  " + prog +
                "\nBlood Type: " + bt +
                "\nAllergies: " + allg
            );
            llStudentCard.setVisibility(View.VISIBLE);

            // Appointments
            llAppointmentsList.removeAllViews();
            for (int i = 0; i < appts.length(); i++) {
                JSONObject a = appts.getJSONObject(i);
                llAppointmentsList.addView(makeListRow(
                    a.optString("type", "Appointment"),
                    a.optString("date", "") + " · " + a.optString("doctor", ""),
                    statusColor(a.optString("status", ""))
                ));
            }
            if (appts.length() == 0)
                llAppointmentsList.addView(makeEmptyLabel("No appointments on record"));

            // Medications
            llMedsList.removeAllViews();
            for (int i = 0; i < meds.length(); i++) {
                JSONObject m = meds.getJSONObject(i);
                llMedsList.addView(makeListRow(
                    m.optString("name", "Medication"),
                    m.optString("dosage", "") + " · " + m.optString("frequency", ""),
                    m.optString("status", "").equals("active") ?
                        "#854F0B" : "#888888"
                ));
            }
            if (meds.length() == 0)
                llMedsList.addView(makeEmptyLabel("No medications on record"));

            // Ready banner
            llReadyBanner.setVisibility(View.VISIBLE);
            setStatus("✅  " + name + " loaded (" + payloadSize + " bytes) " +
                      "— NFC broadcasting active", "#E1F5EE", "#0F6E56");

        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
            onError("Failed to parse student data: " + e.getMessage());
        }
    }

    // Called on main thread on any error
    private void onError(String msg) {
        progressBar.setVisibility(View.GONE);
        btnLoad.setEnabled(true);
        llReadyBanner.setVisibility(View.GONE);
        NfcHceService.clearPayload();
        setStatus("❌  " + msg, "#FCEBEB", "#A32D2D");
    }

    private void clearTerminal() {
        NfcHceService.clearPayload();
        etStudentNo.setText("");
        llStudentCard.setVisibility(View.GONE);
        llReadyBanner.setVisibility(View.GONE);
        llAppointmentsList.removeAllViews();
        llMedsList.removeAllViews();
        setStatus("📡  Ready — enter a student number to begin",
                  "#E6F1FB", "#185FA5");
    }

    // ── HTTP helper (runs on background thread only) ──────────────────

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");

        int code = conn.getResponseCode();
        if (code == 404) throw new java.io.FileNotFoundException("404");
        if (code != 200) throw new Exception("HTTP " + code);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        conn.disconnect();
        return sb.toString();
    }

    // ── UI builder ────────────────────────────────────────────────────

    private void buildUI() {
        // Root scroll container
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#F5F5F5"));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        // Header
        page.addView(buildHeader());

        // Status bar
        tvStatus = new TextView(this);
        tvStatus.setPadding(dp(16), dp(10), dp(16), dp(10));
        tvStatus.setTextSize(12);
        tvStatus.setTypeface(null, Typeface.BOLD);
        page.addView(tvStatus);

        // Input card
        page.addView(buildInputCard());

        // Ready banner (hidden until student loaded)
        llReadyBanner = buildReadyBanner();
        llReadyBanner.setVisibility(View.GONE);
        page.addView(llReadyBanner);

        // Student info card (hidden until loaded)
        llStudentCard = buildStudentCard();
        llStudentCard.setVisibility(View.GONE);
        page.addView(llStudentCard);

        // Appointments card
        LinearLayout apptCard = buildListCard("APPOINTMENTS IN PAYLOAD");
        llAppointmentsList = new LinearLayout(this);
        llAppointmentsList.setOrientation(LinearLayout.VERTICAL);
        apptCard.addView(llAppointmentsList);
        page.addView(apptCard);

        // Medications card
        LinearLayout medCard = buildListCard("MEDICATIONS IN PAYLOAD");
        llMedsList = new LinearLayout(this);
        llMedsList.setOrientation(LinearLayout.VERTICAL);
        medCard.addView(llMedsList);
        page.addView(medCard);

        // Bottom padding
        View pad = new View(this);
        pad.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));
        page.addView(pad);

        scroll.addView(page);
        setContentView(scroll);
    }

    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.parseColor("#1565C0"));
        header.setPadding(dp(16), dp(44), dp(16), dp(16));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("SMRS Clinic Terminal");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, Typeface.BOLD);
        header.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("NFC Broadcasting Station");
        tvSub.setTextColor(Color.parseColor("#B3D4F5"));
        tvSub.setTextSize(12);
        tvSub.setPadding(0, dp(2), 0, 0);
        header.addView(tvSub);

        return header;
    }

    private LinearLayout buildInputCard() {
        LinearLayout card = makeCard();

        TextView lbl = makeSectionLabel("STUDENT NUMBER");
        card.addView(lbl);

        etStudentNo = new EditText(this);
        etStudentNo.setHint("e.g. 2400557");
        etStudentNo.setTextSize(16);
        etStudentNo.setTextColor(Color.parseColor("#1A1A1A"));
        etStudentNo.setHintTextColor(Color.parseColor("#AAAAAA"));
        etStudentNo.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        etStudentNo.setPadding(dp(12), dp(10), dp(12), dp(10));
        etStudentNo.setBackgroundColor(Color.parseColor("#F0F4FF"));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(0, dp(4), 0, dp(12));
        etStudentNo.setLayoutParams(etLp);
        card.addView(etStudentNo);

        // Button row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        btnLoad = new Button(this);
        btnLoad.setText("Load Student");
        btnLoad.setBackgroundColor(Color.parseColor("#1565C0"));
        btnLoad.setTextColor(Color.WHITE);
        btnLoad.setTypeface(null, Typeface.BOLD);
        btnLoad.setAllCaps(false);
        btnLoad.setOnClickListener(v -> loadStudent());
        LinearLayout.LayoutParams loadLp = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        loadLp.setMarginEnd(dp(8));
        btnLoad.setLayoutParams(loadLp);
        btnRow.addView(btnLoad);

        btnClear = new Button(this);
        btnClear.setText("Clear");
        btnClear.setBackgroundColor(Color.parseColor("#FCEBEB"));
        btnClear.setTextColor(Color.parseColor("#A32D2D"));
        btnClear.setTypeface(null, Typeface.BOLD);
        btnClear.setAllCaps(false);
        btnClear.setOnClickListener(v -> clearTerminal());
        btnRow.addView(btnClear);

        card.addView(btnRow);

        progressBar = new ProgressBar(this, null,
            android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(4));
        pbLp.setMargins(0, dp(8), 0, 0);
        progressBar.setLayoutParams(pbLp);
        card.addView(progressBar);

        return card;
    }

    private LinearLayout buildReadyBanner() {
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setBackgroundColor(Color.parseColor("#E1F5EE"));
        banner.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(2), 0, 0);
        banner.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("BROADCASTING VIA NFC HCE");
        tvTitle.setTextColor(Color.parseColor("#0F6E56"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(14);
        banner.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText("Student may now tap their phone to this device");
        tvSub.setTextColor(Color.parseColor("#186B52"));
        tvSub.setTextSize(12);
        tvSub.setPadding(0, dp(2), 0, 0);
        banner.addView(tvSub);

        return banner;
    }

    private LinearLayout buildStudentCard() {
        LinearLayout card = makeCard();
        card.addView(makeSectionLabel("LOADED STUDENT"));

        tvStudentName = new TextView(this);
        tvStudentName.setTextSize(17);
        tvStudentName.setTypeface(null, Typeface.BOLD);
        tvStudentName.setTextColor(Color.parseColor("#1565C0"));
        tvStudentName.setPadding(0, dp(4), 0, dp(2));
        card.addView(tvStudentName);

        tvStudentDetails = new TextView(this);
        tvStudentDetails.setTextSize(12);
        tvStudentDetails.setTextColor(Color.parseColor("#555555"));
        tvStudentDetails.setLineSpacing(0, 1.5f);
        card.addView(tvStudentDetails);

        return card;
    }

    private LinearLayout buildListCard(String title) {
        LinearLayout card = makeCard();
        card.addView(makeSectionLabel(title));
        return card;
    }

    // ── Widget helpers ────────────────────────────────────────────────

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(12), dp(10), dp(12), 0);
        card.setLayoutParams(lp);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        return card;
    }

    private TextView makeSectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#1565C0"));
        tv.setTextSize(10);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        tv.setLayoutParams(lp);
        return tv;
    }

    private View makeListRow(String title, String subtitle, String accentColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundColor(Color.parseColor("#F7F9FC"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        row.setLayoutParams(lp);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));

        TextView tvT = new TextView(this);
        tvT.setText(title);
        tvT.setTypeface(null, Typeface.BOLD);
        tvT.setTextSize(13);
        tvT.setTextColor(Color.parseColor("#1A1A1A"));
        row.addView(tvT);

        TextView tvS = new TextView(this);
        tvS.setText(subtitle);
        tvS.setTextSize(11);
        tvS.setTextColor(Color.parseColor(accentColor));
        row.addView(tvS);

        return row;
    }

    private TextView makeEmptyLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#AAAAAA"));
        tv.setTextSize(12);
        tv.setPadding(0, dp(2), 0, dp(2));
        return tv;
    }

    private void setStatus(String msg, String bg, String fg) {
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(Color.parseColor(bg));
        tvStatus.setTextColor(Color.parseColor(fg));
    }

    private String statusColor(String status) {
        switch (status) {
            case "upcoming":  return "#0F6E56";
            case "pending":   return "#185FA5";
            case "completed": return "#3B6D11";
            default:          return "#888888";
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void showFatalDialog(String title, String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", (d, w) -> finish())
            .setCancelable(false)
            .show();
    }

    private int dp(int val) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(val * density);
    }
}
