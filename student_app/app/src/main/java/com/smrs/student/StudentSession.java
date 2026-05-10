package com.smrs.student;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Singleton that holds the student data received from the clinic NFC terminal.
 * Shared across all activities so data persists during the session.
 * Now includes persistence to SharedPreferences.
 */
public class StudentSession {

    private static final String PREF_NAME = "SMRS_PREFS";
    private static final String KEY_DATA  = "STUDENT_DATA_JSON";
    private static StudentSession instance;

    private JSONObject rawData;
    private JSONObject student;
    private JSONArray  appointments;
    private JSONArray  medications;
    private boolean    loaded = false;

    private StudentSession() {}

    public static StudentSession get() {
        if (instance == null) instance = new StudentSession();
        return instance;
    }

    public void load(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        this.rawData = root;
        student      = root.getJSONObject("student");
        appointments = root.getJSONArray("appointments");
        medications  = root.getJSONArray("medications");
        loaded = true;
    }

    public void saveToStorage(Context context) {
        if (!loaded || rawData == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DATA, rawData.toString()).apply();
    }

    public boolean loadFromStorage(Context context) {
        if (loaded) return true;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_DATA, null);
        if (json != null) {
            try {
                load(json);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void clear(Context context) {
        student = null; 
        appointments = null; 
        medications = null; 
        rawData = null;
        loaded = false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_DATA).apply();
    }

    public void clear() {
        // Legacy clear, doesn't clear storage
        student = null; appointments = null; medications = null; rawData = null; loaded = false;
    }

    public boolean isLoaded() { return loaded; }

    // ── Student getters ───────────────────────────────────────────
    public String getFullName()      { return safe(student, "full_name"); }
    public String getStudentNo()     { return safe(student, "student_no"); }
    public String getProgram()       { return safe(student, "program"); }
    public String getYearLevel()     { return safe(student, "year_level"); }
    public String getBloodType()     { return safe(student, "blood_type"); }
    public String getWeight()        { return safe(student, "weight_kg") + " kg"; }
    public String getHeight()        { return safe(student, "height_cm") + " cm"; }
    public String getBmi()           { return safe(student, "bmi"); }
    public String getAllergies()      { return safe(student, "allergies"); }
    public String getConditions()    { return safe(student, "conditions"); }
    public String getSurgeries()     { return safe(student, "surgeries"); }
    public String getVaccinations()  { return safe(student, "vaccinations"); }
    public String getDob()           { return safe(student, "date_of_birth"); }
    public String getSex()           { return safe(student, "sex"); }
    public String getContact()       { return safe(student, "contact"); }
    public String getEmergency()     { return safe(student, "emergency_contact"); }

    public JSONArray getAppointments() { return appointments; }
    public JSONArray getMedications()  { return medications; }

    private String safe(JSONObject obj, String key) {
        try { return obj.getString(key); } catch (Exception e) { return "–"; }
    }
}
