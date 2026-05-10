# SMRS Backend Setup and NFC Flow Guide

This guide covers everything from setup to the live NFC tap in 4 phases.

## Phase 1 — Backend

1. Extract the zip file if needed.
2. Install dependencies:
   - `python -m pip install -r requirements.txt`
   - If you need Flask/SQLAlchemy support too, install: `python -m pip install flask sqlalchemy`
3. Run the backend server:
   - `python main.py`
   - or `python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000`
4. Find your PC's local IP address.
5. Open the admin panel at:
   - `http://localhost:8000/admin`
6. Sample data is already seeded by the backend, so you should see a working admin view.

## Phase 2 — Clinic Terminal

1. Open the `clinic_terminal/` project in Android Studio.
2. Update `BACKEND_URL` in the app configuration to use your PC's local IP address and port `8000`.
3. Enable USB debugging on the clinic device.
4. Install and run the clinic terminal app on the clinic phone.

## Phase 3 — Student App

1. Open the `student_app/` project in Android Studio.
2. Update `BACKEND_URL` in that app to use the same PC IP and port `8000`.
3. Enable USB debugging on the student device.
4. Install and run the student app on the student phone.

## Phase 4 — Full NFC Flow

1. On the clinic phone, enter the student number.
2. Tap `Load Student`.
3. Wait for the green `Broadcasting` banner to appear.
4. Hold the student phone back-to-back with the clinic phone.
5. The student app should receive the records instantly.

## Troubleshooting

Common failure points:

- **Wrong IP address**
  - Make sure both phones and the PC are on the same network.
  - Use the PC's local IP address, not `localhost`, in `BACKEND_URL`.

- **NFC not connecting**
  - Confirm both devices support NFC.
  - Make sure NFC is enabled on both phones.
  - Align the devices back-to-back as recommended by the phone manufacturers.

- **Gradle build errors**
  - Sync the project in Android Studio.
  - Check for missing SDK components or outdated Gradle plugin versions.

- **Crash on launch**
  - Check the Android logcat output.
  - Verify `BACKEND_URL` is set correctly.
  - Confirm the backend is running and reachable from the phone.
