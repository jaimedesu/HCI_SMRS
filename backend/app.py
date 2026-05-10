"""
SMRS Backend - Student Medical Records System
Flask REST API for the Clinic NFC Terminal App
"""

from flask import Flask, jsonify, request, abort
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime, date
import json
import os

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///smrs.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['SECRET_KEY'] = 'smrs-dev-secret-2025'

db = SQLAlchemy(app)


# ── MODELS ────────────────────────────────────────────────────────────────────

class Student(db.Model):
    __tablename__ = 'students'
    id            = db.Column(db.Integer, primary_key=True)
    student_no    = db.Column(db.String(20), unique=True, nullable=False)
    full_name     = db.Column(db.String(100), nullable=False)
    program       = db.Column(db.String(50))
    year_level    = db.Column(db.String(20))
    date_of_birth = db.Column(db.String(20))
    sex           = db.Column(db.String(10))
    contact       = db.Column(db.String(20))
    emergency_contact = db.Column(db.String(20))
    # Medical
    blood_type    = db.Column(db.String(5))
    weight_kg     = db.Column(db.Float)
    height_cm     = db.Column(db.Float)
    allergies     = db.Column(db.String(200))
    conditions    = db.Column(db.String(200))
    vaccinations  = db.Column(db.String(200))
    surgeries     = db.Column(db.String(200))

    appointments  = db.relationship('Appointment', backref='student', lazy=True)
    medications   = db.relationship('Medication', backref='student', lazy=True)
    record_requests = db.relationship('RecordRequest', backref='student', lazy=True)

    def bmi(self):
        if self.weight_kg and self.height_cm:
            h = self.height_cm / 100
            return round(self.weight_kg / (h * h), 1)
        return None

    def to_nfc_payload(self):
        """Compact JSON payload sent over NFC"""
        return {
            "v": "1",                           # payload version
            "student_no":   self.student_no,
            "full_name":    self.full_name,
            "program":      self.program,
            "year_level":   self.year_level,
            "dob":          self.date_of_birth,
            "sex":          self.sex,
            "contact":      self.contact,
            "emergency_contact": self.emergency_contact,
            "blood_type":   self.blood_type,
            "weight_kg":    self.weight_kg,
            "height_cm":    self.height_cm,
            "bmi":          self.bmi(),
            "allergies":    self.allergies,
            "conditions":   self.conditions,
            "vaccinations": self.vaccinations,
            "surgeries":    self.surgeries,
            "appointments": [a.to_dict() for a in self.appointments],
            "medications":  [m.to_dict() for m in self.medications],
        }

    def to_dict(self):
        return {
            "id":           self.id,
            "student_no":   self.student_no,
            "full_name":    self.full_name,
            "program":      self.program,
            "year_level":   self.year_level,
            "date_of_birth": self.date_of_birth,
            "sex":          self.sex,
            "contact":      self.contact,
            "emergency_contact": self.emergency_contact,
            "blood_type":   self.blood_type,
            "weight_kg":    self.weight_kg,
            "height_cm":    self.height_cm,
            "bmi":          self.bmi(),
            "allergies":    self.allergies,
            "conditions":   self.conditions,
            "vaccinations": self.vaccinations,
            "surgeries":    self.surgeries,
        }


class Appointment(db.Model):
    __tablename__ = 'appointments'
    id          = db.Column(db.Integer, primary_key=True)
    student_id  = db.Column(db.Integer, db.ForeignKey('students.id'), nullable=False)
    title       = db.Column(db.String(100))
    doctor      = db.Column(db.String(100))
    location    = db.Column(db.String(100))
    appt_date   = db.Column(db.String(20))
    appt_time   = db.Column(db.String(10))
    status      = db.Column(db.String(20), default='upcoming')  # upcoming|completed|cancelled

    def to_dict(self):
        return {
            "id":       self.id,
            "title":    self.title,
            "doctor":   self.doctor,
            "location": self.location,
            "date":     self.appt_date,
            "time":     self.appt_time,
            "status":   self.status,
        }


class Medication(db.Model):
    __tablename__ = 'medications'
    id          = db.Column(db.Integer, primary_key=True)
    student_id  = db.Column(db.Integer, db.ForeignKey('students.id'), nullable=False)
    name        = db.Column(db.String(100))
    dosage      = db.Column(db.String(100))
    frequency   = db.Column(db.String(100))
    route       = db.Column(db.String(50))
    prescribed_date = db.Column(db.String(20))
    prescribed_by   = db.Column(db.String(100))
    status      = db.Column(db.String(20), default='active')    # active|completed
    supply_total  = db.Column(db.Integer, default=30)
    supply_remaining = db.Column(db.Integer, default=30)

    def to_dict(self):
        return {
            "id":           self.id,
            "name":         self.name,
            "dosage":       self.dosage,
            "frequency":    self.frequency,
            "route":        self.route,
            "prescribed_date": self.prescribed_date,
            "prescribed_by":   self.prescribed_by,
            "status":       self.status,
            "supply_total": self.supply_total,
            "supply_remaining": self.supply_remaining,
        }


class RecordRequest(db.Model):
    __tablename__ = 'record_requests'
    id              = db.Column(db.Integer, primary_key=True)
    student_id      = db.Column(db.Integer, db.ForeignKey('students.id'), nullable=False)
    record_type     = db.Column(db.String(100))
    purpose         = db.Column(db.String(200))
    date_needed     = db.Column(db.String(20))
    release_mode    = db.Column(db.String(20))   # clinic|email
    status          = db.Column(db.String(20), default='pending')  # pending|processing|ready|released
    submitted_at    = db.Column(db.String(30), default=lambda: datetime.now().isoformat())
    notes           = db.Column(db.String(300))

    def to_dict(self):
        return {
            "id":           self.id,
            "student_id":   self.student_id,
            "record_type":  self.record_type,
            "purpose":      self.purpose,
            "date_needed":  self.date_needed,
            "release_mode": self.release_mode,
            "status":       self.status,
            "submitted_at": self.submitted_at,
            "notes":        self.notes,
        }


# ── ROUTES ────────────────────────────────────────────────────────────────────

@app.route('/api/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "version": "1.0.0"})


# -- Students --

@app.route('/api/students', methods=['GET'])
def list_students():
    students = Student.query.all()
    return jsonify([s.to_dict() for s in students])


@app.route('/api/students/<student_no>', methods=['GET'])
def get_student(student_no):
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    return jsonify(s.to_dict())


@app.route('/api/students', methods=['POST'])
def create_student():
    data = request.get_json()
    if not data:
        abort(400)
    s = Student(**{k: v for k, v in data.items()
                   if k in Student.__table__.columns.keys() and k != 'id'})
    db.session.add(s)
    db.session.commit()
    return jsonify(s.to_dict()), 201


@app.route('/api/students/<student_no>', methods=['PUT'])
def update_student(student_no):
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    data = request.get_json()
    for k, v in data.items():
        if hasattr(s, k) and k not in ('id', 'student_no'):
            setattr(s, k, v)
    db.session.commit()
    return jsonify(s.to_dict())


# -- NFC Payload --

@app.route('/api/nfc/<student_no>', methods=['GET'])
def nfc_payload(student_no):
    """Returns the compact NFC payload for a student — used by the Clinic App"""
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    payload = s.to_nfc_payload()
    return jsonify(payload)


# -- Appointments --

@app.route('/api/students/<student_no>/appointments', methods=['GET'])
def get_appointments(student_no):
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    return jsonify([a.to_dict() for a in s.appointments])


@app.route('/api/students/<student_no>/appointments', methods=['POST'])
def add_appointment(student_no):
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    data = request.get_json()
    a = Appointment(student_id=s.id, **{k: v for k, v in data.items()
                    if k in Appointment.__table__.columns.keys() and k not in ('id','student_id')})
    db.session.add(a)
    db.session.commit()
    return jsonify(a.to_dict()), 201


@app.route('/api/appointments/<int:appt_id>', methods=['PUT'])
def update_appointment(appt_id):
    a = Appointment.query.get_or_404(appt_id)
    data = request.get_json()
    for k, v in data.items():
        if hasattr(a, k) and k not in ('id', 'student_id'):
            setattr(a, k, v)
    db.session.commit()
    return jsonify(a.to_dict())


# -- Medications --

@app.route('/api/students/<student_no>/medications', methods=['GET'])
def get_medications(student_no):
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    return jsonify([m.to_dict() for m in s.medications])


@app.route('/api/students/<student_no>/medications', methods=['POST'])
def add_medication(student_no):
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    data = request.get_json()
    m = Medication(student_id=s.id, **{k: v for k, v in data.items()
                   if k in Medication.__table__.columns.keys() and k not in ('id','student_id')})
    db.session.add(m)
    db.session.commit()
    return jsonify(m.to_dict()), 201


# -- Record Requests --

@app.route('/api/students/<student_no>/requests', methods=['GET'])
def get_requests(student_no):
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    return jsonify([r.to_dict() for r in s.record_requests])


@app.route('/api/students/<student_no>/requests', methods=['POST'])
def submit_request(student_no):
    s = Student.query.filter_by(student_no=student_no).first_or_404()
    data = request.get_json()
    r = RecordRequest(
        student_id   = s.id,
        record_type  = data.get('record_type'),
        purpose      = data.get('purpose'),
        date_needed  = data.get('date_needed'),
        release_mode = data.get('release_mode', 'clinic'),
        submitted_at = datetime.now().isoformat(),
    )
    db.session.add(r)
    db.session.commit()
    return jsonify(r.to_dict()), 201


@app.route('/api/requests/<int:req_id>/status', methods=['PUT'])
def update_request_status(req_id):
    r = RecordRequest.query.get_or_404(req_id)
    data = request.get_json()
    r.status = data.get('status', r.status)
    r.notes  = data.get('notes', r.notes)
    db.session.commit()
    return jsonify(r.to_dict())


@app.route('/api/requests', methods=['GET'])
def all_requests():
    """Clinic dashboard — all pending requests"""
    reqs = RecordRequest.query.order_by(RecordRequest.submitted_at.desc()).all()
    results = []
    for r in reqs:
        d = r.to_dict()
        d['student_name'] = r.student.full_name
        d['student_no']   = r.student.student_no
        results.append(d)
    return jsonify(results)


# ── SEED DATA ─────────────────────────────────────────────────────────────────

def seed():
    if Student.query.count() > 0:
        return

    jose = Student(
        student_no='2400557',
        full_name='Jose Angelo S. Costa',
        program='BSIT',
        year_level='2nd Year',
        date_of_birth='Mar 14, 2004',
        sex='Male',
        contact='+63 912 345 6789',
        emergency_contact='+63 917 000 1111',
        blood_type='O+',
        weight_kg=68,
        height_cm=172,
        allergies='Penicillin, Shrimp',
        conditions='Asthma (mild)',
        vaccinations='Up to date',
        surgeries='None on record',
    )
    db.session.add(jose)
    db.session.flush()

    appointments = [
        Appointment(student_id=jose.id, title='General Check-up',    doctor='Dr. Reyes',  location='Clinic A', appt_date='2025-05-03', appt_time='9:00 AM',  status='upcoming'),
        Appointment(student_id=jose.id, title='Asthma Follow-up',    doctor='Dr. Santos', location='Clinic B', appt_date='2025-05-17', appt_time='2:30 PM',  status='upcoming'),
        Appointment(student_id=jose.id, title='Dental Check-up',     doctor='Dr. Lim',    location='Dental',   appt_date='2025-06-04', appt_time='10:00 AM', status='upcoming'),
        Appointment(student_id=jose.id, title='Annual Physical Exam', doctor='Dr. Reyes', location='Clinic A', appt_date='2025-04-10', appt_time='10:00 AM', status='completed'),
        Appointment(student_id=jose.id, title='Eye Screening',        doctor='Dr. Tan',   location='Clinic C', appt_date='2025-03-22', appt_time='3:00 PM',  status='completed'),
    ]
    medications = [
        Medication(student_id=jose.id, name='Salbutamol',      dosage='2 puffs',  frequency='As needed', route='Inhaler', prescribed_date='2025-04-10', prescribed_by='Dr. Santos', status='active',    supply_total=30, supply_remaining=18),
        Medication(student_id=jose.id, name='Cetirizine 10mg', dosage='1 tablet', frequency='Once daily', route='Oral',   prescribed_date='2025-04-10', prescribed_by='Dr. Reyes',  status='active',    supply_total=30, supply_remaining=24),
        Medication(student_id=jose.id, name='Amoxicillin 500mg', dosage='1 cap', frequency='3x daily',   route='Oral',   prescribed_date='2025-04-10', prescribed_by='Dr. Reyes',  status='completed', supply_total=21, supply_remaining=0),
    ]
    for obj in appointments + medications:
        db.session.add(obj)

    db.session.commit()
    print("✅  Seed data inserted.")


if __name__ == '__main__':
    with app.app_context():
        db.create_all()
        seed()
    app.run(host='0.0.0.0', port=5000, debug=True)
