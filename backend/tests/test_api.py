"""
Complete test suite for RNPS Home Cinema API.
Run with: pytest tests/ -v
"""
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.main import app
from app.config.database import get_db, Base
from app.services.seeder import run_seeds

# ─── Test Database Setup ──────────────────────────────────────────────────────

TEST_DATABASE_URL = "sqlite:///./database/test_cinema.db"

engine = create_engine(TEST_DATABASE_URL, connect_args={"check_same_thread": False})
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def override_get_db():
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()


app.dependency_overrides[get_db] = override_get_db

Base.metadata.drop_all(bind=engine)
Base.metadata.create_all(bind=engine)

# Seed test database with RNPS Home Cinema data
with TestingSessionLocal() as session:
    run_seeds(session)

client = TestClient(app)


def get_auth_token():
    res = client.post("/api/auth/login", json={
        "email": "admin@rnps.com",
        "password": "admin123",
    })
    return res.json()["access_token"]


# ─── Health Tests ─────────────────────────────────────────────────────────────

def test_root():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "app" in data


def test_health():
    response = client.get("/api/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"


# ─── Payment Flow Tests ───────────────────────────────────────────────────────

def test_payment_submission_flow():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Get show & seats
    shows = client.get("/api/shows/", headers=headers).json()
    show_id = shows[0]["id"]
    seat_data = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    available_seats = [s for s in seat_data["seats"] if s["status"] == "AVAILABLE"]

    # 2. Create booking
    booking_res = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_id, "seat_ids": [available_seats[0]["seat_id"]]}
    )
    assert booking_res.status_code == 201
    booking_id = booking_res.json()["id"]

    # 3. Add snacks
    snacks = client.get("/api/snacks/", headers=headers).json()
    client.post(
        f"/api/snacks/booking/{booking_id}",
        headers=headers,
        json={"items": [{"snack_id": snacks[0]["id"], "quantity": 1}]}
    )

    # 4. Submit payment ("I Have Paid")
    pay_res = client.post(
        "/api/payments/submit",
        headers=headers,
        json={"booking_id": booking_id, "payment_method": "UPI", "transaction_reference": "TXN-123456"}
    )
    assert pay_res.status_code == 200
    res_data = pay_res.json()

    assert res_data["payment"] is not None
    assert res_data["payment"]["status"] == "SUBMITTED"
    assert res_data["ticket"] is not None
    assert "ticket_code" in res_data["ticket"]

    # 5. Prevent duplicate payment submission
    dup_pay = client.post(
        "/api/payments/submit",
        headers=headers,
        json={"booking_id": booking_id, "payment_method": "UPI"}
    )
    assert dup_pay.status_code == 409
