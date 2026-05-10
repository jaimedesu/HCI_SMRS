package com.smrs.student;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.*;
import android.nfc.tech.IsoDep;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * NfcTapActivity — the NFC tap gate screen.
 *
 * When student taps their phone to the clinic NFC terminal:
 *  1. Android dispatches TECH_DISCOVERED intent to this activity
 *  2. We use IsoDep to send APDU commands to the clinic's HCE service
 *  3. SELECT AID → GET DATA (chunk by chunk) → reassemble JSON
 *  4. Load into StudentSession → navigate to HomeActivity
 */
public class NfcTapActivity extends Activity {

    // Must match clinic terminal's AID
    private static final byte[] SMRS_AID = {
        (byte)0xF0,(byte)0x53,(byte)0x4D,(byte)0x52,(byte)0x53,(byte)0x00
    };
    private static final byte[] CMD_SELECT_AID = buildSelectAid(SMRS_AID);
    private static final byte[] CMD_GET_DATA   = {(byte)0x00,(byte)0xCA,(byte)0x00,(byte)0x00,(byte)0x00};
    private static final byte[] CMD_GET_NEXT   = {(byte)0x00,(byte)0xCA,(byte)0x00,(byte)0x01,(byte)0x00};
    private static final byte[] SW_OK          = {(byte)0x90,(byte)0x00};
    private static final byte[] SW_MORE        = {(byte)0x61,(byte)0x00};

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView tvStatus, tvInstruction;
    private View pulseRing1, pulseRing2, pulseRing3;
    private ProgressBar progressBar;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        // Prepare PendingIntent for NFC foreground dispatch (Required for Android 12+)
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S 
                    ? PendingIntent.FLAG_MUTABLE 
                    : 0;
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
        
        buildUI();
        
        // If data is already saved on device, skip tap screen
        if (StudentSession.get().loadFromStorage(this)) {
            navigateHome();
            return;
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            if (pendingIntent == null) {
                Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0;
                pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
            }
            try {
                nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            setStatus("❌ NFC is disabled. Please enable it in settings.", "#FCEBEB", "#A32D2D");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) readFromClinicTerminal(tag);
        }
    }

    // ── APDU Communication ────────────────────────────────────────────
    private void readFromClinicTerminal(Tag tag) {
        setStatus("📡  Clinic terminal detected…", "#E6F1FB", "#185FA5");
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            IsoDep isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                uiError("This tag does not support ISO-DEP / HCE.");
                return;
            }
            try {
                isoDep.connect();
                isoDep.setTimeout(5000);

                // Step 1: SELECT AID
                byte[] resp = isoDep.transceive(CMD_SELECT_AID);
                if (!endsWith(resp, SW_OK)) {
                    String hex = bytesToHex(resp);
                    String msg = "Clinic terminal rejected (AID not found).\n";
                    if (hex.equals("6A82")) {
                        msg = "Terminal has no student records loaded.\nLoad a student on the terminal first.";
                    } else {
                        msg += "Code: " + hex;
                    }
                    uiError(msg);
                    isoDep.close();
                    return;
                }

                handler.post(() -> setStatus("✅  Terminal authenticated — receiving data…", "#E6F1FB", "#185FA5"));

                // Step 2: Receive chunks
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] cmd = CMD_GET_DATA;
                int chunkCount = 0;
                
                while (true) {
                    resp = isoDep.transceive(cmd);
                    if (resp.length < 2) {
                        uiError("Protocol error: received empty chunk.");
                        break;
                    }

                    byte[] sw = {resp[resp.length-2], resp[resp.length-1]};
                    baos.write(resp, 0, resp.length - 2);
                    chunkCount++;

                    if (Arrays.equals(sw, SW_OK)) {
                        break; // last chunk
                    } else if (Arrays.equals(sw, SW_MORE)) {
                        cmd = CMD_GET_NEXT;
                    } else {
                        uiError("Error during transfer: " + bytesToHex(sw));
                        break;
                    }
                }

                isoDep.close();

                String json = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                if (json.isEmpty()) {
                    uiError("Received empty data from terminal.");
                    return;
                }
                
                Log.d("NfcTap", "Received " + json.length() + " chars in " + chunkCount + " chunks.");

                // Step 3: Load into session and save to device
                StudentSession.get().load(json);
                StudentSession.get().saveToStorage(NfcTapActivity.this);
                handler.post(this::navigateHome);

            } catch (Exception e) {
                uiError("NFC Error: " + e.getMessage());
            }
        }).start();
    }

    private void navigateHome() {
        progressBar.setVisibility(View.GONE);
        Intent intent = new Intent(this, HomeActivity.class);
        // Ensure proper Android 14+ compatibility
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void uiError(String msg) {
        handler.post(() -> {
            progressBar.setVisibility(View.GONE);
            setStatus("❌  " + msg, "#FCEBEB", "#A32D2D");
        });
    }

    // ── UI ────────────────────────────────────────────────────────────
    private void buildUI() {
        // Full blue background
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0D47A1"));
        root.setGravity(android.view.Gravity.CENTER);

        // App label
        TextView tvApp = new TextView(this);
        tvApp.setText("STUDENT MEDICAL RECORDS");
        tvApp.setTextColor(Color.parseColor("#80B8FF"));
        tvApp.setTextSize(10);
        tvApp.setTypeface(null, android.graphics.Typeface.BOLD);
        tvApp.setLetterSpacing(0.15f);
        tvApp.setGravity(android.view.Gravity.CENTER);
        tvApp.setPadding(0, dp(40), 0, dp(32));
        root.addView(tvApp);

        // NFC icon area (concentric circles)
        FrameLayout iconArea = new FrameLayout(this);
        int sz = dp(200);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(sz, sz);
        iconLp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        iconArea.setLayoutParams(iconLp);

        pulseRing1 = makeCircle(sz, Color.parseColor("#1A3A7A"));
        pulseRing2 = makeCircle(dp(150), Color.parseColor("#1B4EA0"));
        pulseRing3 = makeCircle(dp(100), Color.parseColor("#1E60C2"));
        View center  = makeCircle(dp(60), Color.parseColor("#2979FF"));

        iconArea.addView(pulseRing1, centerLp(sz, sz));
        iconArea.addView(pulseRing2, centerLp(sz, dp(150)));
        iconArea.addView(pulseRing3, centerLp(sz, dp(100)));
        iconArea.addView(center, centerLp(sz, dp(60)));

        // NFC waves inside center
        TextView tvNfc = new TextView(this);
        tvNfc.setText("((  ))");
        tvNfc.setTextColor(Color.WHITE);
        tvNfc.setTextSize(18);
        tvNfc.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNfc.setGravity(android.view.Gravity.CENTER);
        FrameLayout.LayoutParams textLp = new FrameLayout.LayoutParams(sz, sz);
        textLp.gravity = android.view.Gravity.CENTER;
        iconArea.addView(tvNfc, textLp);

        root.addView(iconArea);

        // Main text
        TextView tvMain = new TextView(this);
        tvMain.setText("Tap Your Phone");
        tvMain.setTextColor(Color.WHITE);
        tvMain.setTextSize(26);
        tvMain.setTypeface(null, android.graphics.Typeface.BOLD);
        tvMain.setGravity(android.view.Gravity.CENTER);
        tvMain.setPadding(dp(24), dp(28), dp(24), dp(4));
        root.addView(tvMain);

        TextView tvSub = new TextView(this);
        tvSub.setText("to the Clinic NFC Terminal");
        tvSub.setTextColor(Color.parseColor("#80AADD"));
        tvSub.setTextSize(14);
        tvSub.setGravity(android.view.Gravity.CENTER);
        root.addView(tvSub);

        tvInstruction = new TextView(this);
        tvInstruction.setText("The clinic terminal will transfer\nyour medical records to this app.");
        tvInstruction.setTextColor(Color.parseColor("#5588BB"));
        tvInstruction.setTextSize(12);
        tvInstruction.setGravity(android.view.Gravity.CENTER);
        tvInstruction.setPadding(dp(32), dp(16), dp(32), dp(32));
        root.addView(tvInstruction);

        // Status banner
        tvStatus = new TextView(this);
        tvStatus.setText("📡  Waiting for clinic terminal…");
        tvStatus.setTextColor(Color.parseColor("#185FA5"));
        tvStatus.setBackgroundColor(Color.parseColor("#E6F1FB"));
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStatus.setTextSize(12);
        tvStatus.setGravity(android.view.Gravity.CENTER);
        tvStatus.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.addView(tvStatus);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        root.addView(progressBar);

        // How it works strip
        LinearLayout steps = new LinearLayout(this);
        steps.setOrientation(LinearLayout.HORIZONTAL);
        steps.setBackgroundColor(Color.parseColor("#0A255C"));
        steps.setPadding(dp(20), dp(16), dp(20), dp(16));
        LinearLayout.LayoutParams stepsLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stepsLp.topMargin = dp(20);
        steps.setLayoutParams(stepsLp);

        steps.addView(makeStep("1", "Visit\nthe clinic"));
        steps.addView(makeArrow());
        steps.addView(makeStep("2", "Tap phone\nto terminal"));
        steps.addView(makeArrow());
        steps.addView(makeStep("3", "Records load\ninstantly"));

        root.addView(steps);

        // Security note
        TextView tvSec = new TextView(this);
        tvSec.setText("🔒  Only available at authorized clinic terminals");
        tvSec.setTextColor(Color.parseColor("#3D6A9A"));
        tvSec.setTextSize(10);
        tvSec.setGravity(android.view.Gravity.CENTER);
        tvSec.setPadding(dp(16), dp(16), dp(16), dp(8));
        root.addView(tvSec);

        setContentView(root);
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private void setStatus(String msg, String bg, String fg) {
        tvStatus.setText(msg);
        tvStatus.setBackgroundColor(Color.parseColor(bg));
        tvStatus.setTextColor(Color.parseColor(fg));
    }

    private View makeCircle(int size, int color) {
        View v = new View(this);
        v.setBackgroundColor(color);
        // Circle via clipToOutline would need API21+ outline, use simple bg for now
        return v;
    }

    private FrameLayout.LayoutParams centerLp(int parentSz, int childSz) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(childSz, childSz);
        lp.gravity = android.view.Gravity.CENTER;
        return lp;
    }

    private LinearLayout makeStep(String num, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(android.view.Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView tvNum = new TextView(this);
        tvNum.setText(num);
        tvNum.setTextColor(Color.WHITE);
        tvNum.setTextSize(18);
        tvNum.setTypeface(null, android.graphics.Typeface.BOLD);
        tvNum.setGravity(android.view.Gravity.CENTER);
        col.addView(tvNum);

        TextView tvLbl = new TextView(this);
        tvLbl.setText(label);
        tvLbl.setTextColor(Color.parseColor("#7AAAD4"));
        tvLbl.setTextSize(9);
        tvLbl.setGravity(android.view.Gravity.CENTER);
        col.addView(tvLbl);

        return col;
    }

    private TextView makeArrow() {
        TextView tv = new TextView(this);
        tv.setText("›");
        tv.setTextColor(Color.parseColor("#2A4A6A"));
        tv.setTextSize(20);
        tv.setGravity(android.view.Gravity.CENTER);
        return tv;
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    // ── APDU Builders ─────────────────────────────────────────────────
    private static byte[] buildSelectAid(byte[] aid) {
        byte[] cmd = new byte[5 + aid.length + 1];
        cmd[0] = (byte)0x00; cmd[1] = (byte)0xA4;
        cmd[2] = (byte)0x04; cmd[3] = (byte)0x00;
        cmd[4] = (byte)aid.length;
        System.arraycopy(aid, 0, cmd, 5, aid.length);
        cmd[5 + aid.length] = (byte)0x00;
        return cmd;
    }

    private boolean endsWith(byte[] arr, byte[] suffix) {
        if (arr.length < suffix.length) return false;
        for (int i = 0; i < suffix.length; i++)
            if (arr[arr.length - suffix.length + i] != suffix[i]) return false;
        return true;
    }

    private String bytesToHex(byte[] b) {
        if (b == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02X", x));
        return sb.toString();
    }
}
