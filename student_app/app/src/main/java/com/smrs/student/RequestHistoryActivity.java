package com.smrs.student;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestHistoryActivity extends BaseActivity {

    private static final String BACKEND_URL = "http://192.168.86.141:8000";

    private LinearLayout llHistoryList;
    private ProgressBar progressBar;
    private TextView tvEmpty;

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
        buildPage("Request History", "Status of your record requests");

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        contentArea.addView(progressBar);

        tvEmpty = new TextView(this);
        tvEmpty.setText("No requests found.");
        tvEmpty.setTextColor(Color.parseColor("#888888"));
        tvEmpty.setTextSize(14);
        tvEmpty.setGravity(Gravity.CENTER);
        tvEmpty.setPadding(0, dp(40), 0, dp(40));
        tvEmpty.setVisibility(View.GONE);
        contentArea.addView(tvEmpty);

        llHistoryList = new LinearLayout(this);
        llHistoryList.setOrientation(LinearLayout.VERTICAL);
        contentArea.addView(llHistoryList);

        loadHistory(s.getStudentNo());
    }

    private void loadHistory(String studentNo) {
        progressBar.setVisibility(View.VISIBLE);
        llHistoryList.removeAllViews();
        tvEmpty.setVisibility(View.GONE);

        executor.execute(() -> {
            try {
                URL url = new URL(BACKEND_URL + "/students/" + studentNo + "/record-requests");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONArray arr = new JSONArray(sb.toString());
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (arr.length() == 0) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            renderHistory(arr);
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Server error: " + code, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderHistory(JSONArray arr) {
        try {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                llHistoryList.addView(makeRequestRow(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View makeRequestRow(JSONObject r) throws Exception {
        LinearLayout row = makeCard();
        
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        
        TextView tvType = new TextView(this);
        tvType.setText(r.getString("record_type"));
        tvType.setTypeface(null, android.graphics.Typeface.BOLD);
        tvType.setTextSize(14);
        tvType.setTextColor(Color.parseColor("#1A1A1A"));
        tvType.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(tvType);
        
        String status = r.getString("status");
        header.addView(makeStatusBadge(status));
        row.addView(header);
        
        TextView tvDate = new TextView(this);
        tvDate.setText("Needed by: " + r.getString("date_needed"));
        tvDate.setTextSize(11);
        tvDate.setTextColor(Color.parseColor("#666666"));
        tvDate.setPadding(0, dp(4), 0, dp(8));
        row.addView(tvDate);
        
        row.addView(makeDivider());
        
        row.addView(makeInfoRow("Purpose", r.getString("purpose")));
        row.addView(makeInfoRow("Release", r.getString("mode_of_release")));
        row.addView(makeInfoRow("Submitted", r.optString("submitted_at", "–")));
        
        return row;
    }

    private TextView makeStatusBadge(String status) {
        String bg = "#EEEEEE", fg = "#888888";
        if (status.equalsIgnoreCase("approved")) { bg = "#E1F5EE"; fg = "#0F6E56"; }
        else if (status.equalsIgnoreCase("pending")) { bg = "#E6F1FB"; fg = "#185FA5"; }
        else if (status.equalsIgnoreCase("denied")) { bg = "#FCEBEB"; fg = "#A32D2D"; }
        
        TextView tv = new TextView(this);
        tv.setText(status.toUpperCase());
        tv.setBackgroundColor(Color.parseColor(bg));
        tv.setTextColor(Color.parseColor(fg));
        tv.setTextSize(9);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(dp(8), dp(2), dp(8), dp(2));
        return tv;
    }
}
