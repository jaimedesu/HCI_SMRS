"""
SMRS Backend - FastAPI server
Clinic admin panel + REST API for clinic terminal app to sync records
"""
from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse
import uvicorn
from database import Database
from models import Student, Appointment, Medication, MedicalRecord
from typing import List, Optional
import json

app = FastAPI(title="SMRS Backend", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

db = Database()

# ── Students ────────────────────────────────────────────────
@app.get("/students", response_model=List[Student])
def list_students():
    return db.get_all_students()

@app.get("/students/{student_no}", response_model=Student)
def get_student(student_no: str):
    s = db.get_student(student_no)
    if not s:
        raise HTTPException(404, "Student not found")
    return s

@app.post("/students", response_model=Student)
def create_student(student: Student):
    if db.get_student(student.student_no):
        raise HTTPException(status_code=409, detail="Student already exists")
    return db.create_student(student)

@app.put("/students/{student_no}", response_model=Student)
def update_student(student_no: str, student: Student):
    existing = db.get_student(student_no)
    if not existing:
        raise HTTPException(status_code=404, detail="Student not found")
    return db.update_student(student_no, student)

# ── Full NFC Payload ─────────────────────────────────────────
@app.get("/nfc-payload/{student_no}")
def get_nfc_payload(student_no: str):
    """
    Returns the full JSON blob that the clinic terminal will
    push to the student's phone via NFC HCE / NDEF.
    """
    student = db.get_student(student_no)
    if not student:
        raise HTTPException(404, "Student not found")
    appointments = db.get_appointments(student_no)
    medications   = db.get_medications(student_no)
    return {
        "version": "1.0",
        "source": "SMRS_CLINIC_TERMINAL",
        "student": student.dict(),
        "appointments": [a.dict() for a in appointments],
        "medications":  [m.dict() for m in medications],
    }

# ── Appointments ─────────────────────────────────────────────
@app.get("/students/{student_no}/appointments", response_model=List[Appointment])
def get_appointments(student_no: str):
    return db.get_appointments(student_no)

@app.post("/students/{student_no}/appointments", response_model=Appointment)
def add_appointment(student_no: str, appt: Appointment):
    appt.student_no = student_no
    return db.create_appointment(appt)

@app.put("/appointments/{appt_id}", response_model=Appointment)
def update_appointment(appt_id: int, appt: Appointment):
    return db.update_appointment(appt_id, appt)

@app.delete("/appointments/{appt_id}")
def delete_appointment(appt_id: int):
    db.delete_appointment(appt_id)
    return {"deleted": appt_id}

# ── Medications ──────────────────────────────────────────────
@app.get("/students/{student_no}/medications", response_model=List[Medication])
def get_medications(student_no: str):
    return db.get_medications(student_no)

@app.post("/students/{student_no}/medications", response_model=Medication)
def add_medication(student_no: str, med: Medication):
    med.student_no = student_no
    return db.create_medication(med)

@app.put("/medications/{med_id}", response_model=Medication)
def update_medication(med_id: int, med: Medication):
    return db.update_medication(med_id, med)

@app.delete("/medications/{med_id}")
def delete_medication(med_id: int):
    db.delete_medication(med_id)
    return {"deleted": med_id}

# ── Medical Record Requests ──────────────────────────────────
@app.get("/record-requests")
def list_record_requests():
    return db.get_record_requests()

@app.get("/students/{student_no}/record-requests")
def get_student_record_requests(student_no: str):
    return db.get_student_record_requests(student_no)

@app.post("/record-requests")
def submit_record_request(req: MedicalRecord):
    return db.create_record_request(req)

@app.put("/record-requests/{req_id}/status")
def update_request_status(req_id: int, status: str):
    return db.update_request_status(req_id, status)

@app.delete("/record-requests/{req_id}")
def delete_record_request(req_id: int):
    db.delete_record_request(req_id)
    return {"deleted": req_id}

# ── Admin HTML Panel ─────────────────────────────────────────
@app.get("/admin", response_class=HTMLResponse)
def admin_panel():
    with open("admin.html", encoding="utf-8") as f:
        return f.read()

if __name__ == "__main__":
    db.init()
    db.seed_sample_data()
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=True)
