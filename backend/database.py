import sqlite3
import json
from models import Student, Appointment, Medication, MedicalRecord
from datetime import datetime

DB_PATH = "smrs.db"

class Database:
    def __init__(self):
        self.path = DB_PATH

    def _conn(self):
        conn = sqlite3.connect(self.path)
        conn.row_factory = sqlite3.Row
        return conn

    def init(self):
        conn = self._conn()
        c = conn.cursor()
        c.executescript("""
        CREATE TABLE IF NOT EXISTS students (
            student_no TEXT PRIMARY KEY,
            full_name TEXT, program TEXT, year_level TEXT,
            date_of_birth TEXT, sex TEXT, contact TEXT,
            emergency_contact TEXT, blood_type TEXT,
            weight_kg REAL, height_cm REAL, bmi REAL,
            allergies TEXT, conditions TEXT,
            surgeries TEXT, vaccinations TEXT
        );
        CREATE TABLE IF NOT EXISTS appointments (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            student_no TEXT, doctor TEXT, type TEXT,
            date TEXT, time TEXT, location TEXT, status TEXT,
            FOREIGN KEY(student_no) REFERENCES students(student_no)
        );
        CREATE TABLE IF NOT EXISTS medications (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            student_no TEXT, name TEXT, dosage TEXT,
            frequency TEXT, route TEXT, prescribed_date TEXT,
            doctor TEXT, supply_total INTEGER,
            supply_remaining INTEGER, status TEXT,
            FOREIGN KEY(student_no) REFERENCES students(student_no)
        );
        CREATE TABLE IF NOT EXISTS record_requests (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            student_no TEXT, record_type TEXT, purpose TEXT,
            date_needed TEXT, mode_of_release TEXT,
            status TEXT DEFAULT 'pending',
            submitted_at TEXT
        );
        """)
        conn.commit()
        conn.close()

    def seed_sample_data(self):
        conn = self._conn()
        c = conn.cursor()
        # Check if already seeded
        if c.execute("SELECT COUNT(*) FROM students").fetchone()[0] > 0:
            conn.close()
            return
        # Student
        c.execute("""INSERT INTO students VALUES
            ('2400557','Jose Angelo S. Costa','BSIT','2nd Year',
             'Mar 14, 2004','Male','+63 912 345 6789','+63 917 000 1111',
             'O+',68.0,172.0,23.0,'Penicillin, Shrimp',
             'Asthma (mild)','None on record','Up to date')""")
        # Appointments
        appts = [
            ('2400557','Dr. Reyes','General Check-up','May 03, 2025','9:00 AM','Clinic A','upcoming'),
            ('2400557','Dr. Santos','Asthma Follow-up','May 17, 2025','2:30 PM','Clinic B','upcoming'),
            ('2400557','Dr. Lim','Dental Check-up','Jun 04, 2025','10:00 AM','Dental','pending'),
            ('2400557','Dr. Reyes','Annual Physical Exam','Apr 10, 2025','10:00 AM','Clinic A','completed'),
            ('2400557','Dr. Tan','Eye Screening','Mar 22, 2025','3:00 PM','Clinic C','completed'),
        ]
        c.executemany("INSERT INTO appointments(student_no,doctor,type,date,time,location,status) VALUES(?,?,?,?,?,?,?)", appts)
        # Medications
        meds = [
            ('2400557','Salbutamol','2 puffs','As needed','Inhaler','Apr 10, 2025','Dr. Santos',30,18,'active'),
            ('2400557','Cetirizine 10mg','1 tablet','Once daily','Oral','Apr 10, 2025','Dr. Reyes',30,24,'active'),
            ('2400557','Amoxicillin 500mg','1 capsule','3x daily','Oral','Apr 10, 2025','Dr. Reyes',21,0,'completed'),
        ]
        c.executemany("INSERT INTO medications(student_no,name,dosage,frequency,route,prescribed_date,doctor,supply_total,supply_remaining,status) VALUES(?,?,?,?,?,?,?,?,?,?)", meds)
        conn.commit()
        conn.close()
        print("Sample data seeded.")

    # ── Students ────────────────────────────
    def get_all_students(self):
        conn = self._conn()
        rows = conn.execute("SELECT * FROM students").fetchall()
        conn.close()
        return [Student(**dict(r)) for r in rows]

    def get_student(self, student_no):
        conn = self._conn()
        row = conn.execute("SELECT * FROM students WHERE student_no=?", (student_no,)).fetchone()
        conn.close()
        return Student(**dict(row)) if row else None

    def create_student(self, s: Student):
        conn = self._conn()
        conn.execute("""INSERT INTO students VALUES
            (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)""",
            (s.student_no, s.full_name, s.program, s.year_level,
             s.date_of_birth, s.sex, s.contact, s.emergency_contact,
             s.blood_type, s.weight_kg, s.height_cm, s.bmi,
             s.allergies, s.conditions, s.surgeries, s.vaccinations))
        conn.commit()
        conn.close()
        return s

    def update_student(self, student_no, s: Student):
        conn = self._conn()
        conn.execute("""UPDATE students SET full_name=?,program=?,year_level=?,
            date_of_birth=?,sex=?,contact=?,emergency_contact=?,blood_type=?,
            weight_kg=?,height_cm=?,bmi=?,allergies=?,conditions=?,surgeries=?,vaccinations=?
            WHERE student_no=?""",
            (s.full_name, s.program, s.year_level, s.date_of_birth, s.sex,
             s.contact, s.emergency_contact, s.blood_type, s.weight_kg, s.height_cm,
             s.bmi, s.allergies, s.conditions, s.surgeries, s.vaccinations, student_no))
        conn.commit()
        conn.close()
        return s

    # ── Appointments ────────────────────────
    def get_appointments(self, student_no):
        conn = self._conn()
        rows = conn.execute("SELECT * FROM appointments WHERE student_no=? ORDER BY date", (student_no,)).fetchall()
        conn.close()
        return [Appointment(**dict(r)) for r in rows]

    def create_appointment(self, a: Appointment):
        conn = self._conn()
        cur = conn.execute("""INSERT INTO appointments(student_no,doctor,type,date,time,location,status)
            VALUES(?,?,?,?,?,?,?)""",
            (a.student_no, a.doctor, a.type, a.date, a.time, a.location, a.status))
        a.id = cur.lastrowid
        conn.commit()
        conn.close()
        return a

    def update_appointment(self, appt_id, a: Appointment):
        conn = self._conn()
        conn.execute("""UPDATE appointments SET doctor=?,type=?,date=?,time=?,location=?,status=?
            WHERE id=?""", (a.doctor, a.type, a.date, a.time, a.location, a.status, appt_id))
        conn.commit()
        conn.close()
        a.id = appt_id
        return a

    def delete_appointment(self, appt_id):
        conn = self._conn()
        conn.execute("DELETE FROM appointments WHERE id=?", (appt_id,))
        conn.commit()
        conn.close()

    # ── Medications ─────────────────────────
    def get_medications(self, student_no):
        conn = self._conn()
        rows = conn.execute("SELECT * FROM medications WHERE student_no=?", (student_no,)).fetchall()
        conn.close()
        return [Medication(**dict(r)) for r in rows]

    def create_medication(self, m: Medication):
        conn = self._conn()
        cur = conn.execute("""INSERT INTO medications(student_no,name,dosage,frequency,route,prescribed_date,doctor,supply_total,supply_remaining,status)
            VALUES(?,?,?,?,?,?,?,?,?,?)""",
            (m.student_no, m.name, m.dosage, m.frequency, m.route,
             m.prescribed_date, m.doctor, m.supply_total, m.supply_remaining, m.status))
        m.id = cur.lastrowid
        conn.commit()
        conn.close()
        return m

    def update_medication(self, med_id, m: Medication):
        conn = self._conn()
        conn.execute("""UPDATE medications SET name=?,dosage=?,frequency=?,route=?,
            prescribed_date=?,doctor=?,supply_total=?,supply_remaining=?,status=?
            WHERE id=?""",
            (m.name, m.dosage, m.frequency, m.route, m.prescribed_date,
             m.doctor, m.supply_total, m.supply_remaining, m.status, med_id))
        conn.commit()
        conn.close()
        m.id = med_id
        return m

    def delete_medication(self, med_id):
        conn = self._conn()
        conn.execute("DELETE FROM medications WHERE id=?", (med_id,))
        conn.commit()
        conn.close()

    # ── Record Requests ─────────────────────
    def get_record_requests(self):
        conn = self._conn()
        rows = conn.execute("SELECT * FROM record_requests ORDER BY submitted_at DESC").fetchall()
        conn.close()
        return [dict(r) for r in rows]

    def create_record_request(self, req: MedicalRecord):
        conn = self._conn()
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        cur = conn.execute("""INSERT INTO record_requests(student_no,record_type,purpose,date_needed,mode_of_release,status,submitted_at)
            VALUES(?,?,?,?,?,?,?)""",
            (req.student_no, req.record_type, req.purpose, req.date_needed,
             req.mode_of_release, "pending", now))
        req.id = cur.lastrowid
        req.submitted_at = now
        conn.commit()
        conn.close()
        return req

    def update_request_status(self, req_id, status):
        conn = self._conn()
        conn.execute("UPDATE record_requests SET status=? WHERE id=?", (status, req_id))
        conn.commit()
        conn.close()
        return {"id": req_id, "status": status}
