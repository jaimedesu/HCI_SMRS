from pydantic import BaseModel
from typing import Optional, List

class Student(BaseModel):
    student_no: str
    full_name: str
    program: str
    year_level: str
    date_of_birth: str
    sex: str
    contact: str
    emergency_contact: str
    blood_type: str
    weight_kg: float
    height_cm: float
    bmi: float
    allergies: str
    conditions: str
    surgeries: str
    vaccinations: str

class Appointment(BaseModel):
    id: Optional[int] = None
    student_no: Optional[str] = None
    doctor: str
    type: str
    date: str
    time: str
    location: str
    status: str  # upcoming | completed | pending

class Medication(BaseModel):
    id: Optional[int] = None
    student_no: Optional[str] = None
    name: str
    dosage: str
    frequency: str
    route: str
    prescribed_date: str
    doctor: str
    supply_total: int
    supply_remaining: int
    status: str  # active | completed

class MedicalRecord(BaseModel):
    id: Optional[int] = None
    student_no: str
    record_type: str
    purpose: str
    date_needed: str
    mode_of_release: str
    status: Optional[str] = "pending"
    submitted_at: Optional[str] = None
