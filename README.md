# SMRS — Student Medical Records System
## NFC-Based Android + Python

---

## Architecture

```
Clinic Terminal Phone  ──NFC tap──►  Student Phone
(HCE broadcaster)      ISO-DEP       (APDU reader)
        │
        │ HTTP REST
        ▼
  Python Backend
  FastAPI + SQLite
  localhost:8000
  /admin  Web Panel
```

---

## 1. Python Backend

```bash
cd backend/
pip install -r requirements.txt
python main.py
```
Admin panel: http://localhost:8000/admin
Sample student (2400557) auto-seeded on first run.

Key endpoints:
  GET  /nfc-payload/{student_no}  — full JSON for NFC transfer
  GET  /admin                     — web admin panel
  POST /record-requests           — submit record request

---

## 2. Clinic Terminal App

1. Open clinic_terminal/ in Android Studio
2. In MainActivity.java set BACKEND_URL to your server IP
3. Install on clinic staff phone (NFC required)
4. Enter student number → Load Student → terminal broadcasts via NFC HCE

---

## 3. Student App

1. Open student_app/ in Android Studio
2. In RequestFormActivity.java set BACKEND_URL
3. Install on student phone (NFC required)
4. Open app → tap phone to clinic terminal → records load instantly

---

## 4. NFC Protocol

AID: F0 53 4D 52 53 00

Flow:
  Student sends SELECT AID → clinic responds 90 00
  Student sends GET DATA   → clinic sends chunks (61 00 = more / 90 00 = done)
  Student reassembles JSON → loads into session

---

## 5. Project Structure

smrs/
  backend/
    main.py           FastAPI routes
    database.py       SQLite CRUD
    models.py         Pydantic models
    admin.html        Web admin panel
    requirements.txt

  clinic_terminal/
    MainActivity.java       Staff UI
    NfcHceService.java      HCE broadcaster

  student_app/
    NfcTapActivity.java     NFC reader
    StudentSession.java     In-memory session
    BaseActivity.java       Shared chrome
    HomeActivity.java
    ScheduleActivity.java
    MedicationActivity.java
    ProfileActivity.java
    SettingsActivity.java
    RequestFormActivity.java

---

## Requirements

  Python 3.10+, FastAPI 0.110+
  Android Studio Hedgehog+
  Android minSdk 26 (Android 8.0)
  NFC hardware on both phones
  Both phones + server on same Wi-Fi
