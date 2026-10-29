"""
Complete test suite for RNPS Home Cinema API.
Run with: pytest tests/ -v
"""
from datetime import datetime, timedelta
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.main import app
from app.config.database import get_db, Base
from app.services.seeder import run_seeds

# ─── Test Database Setup ──────────────────────────────────────────────────────
import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent
DB_DIR = BASE_DIR / "database"
DB_DIR.mkdir(parents=True, exist_ok=True)
TEST_DATABASE_URL = f"sqlite:///{DB_DIR / 'test_cinema.db'}"

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


# ─── Auth Endpoint Tests ──────────────────────────────────────────────────────

def test_auth_successful_registration():
    res = client.post("/api/auth/register", json={
        "name": "New User",
        "email": "newuser@example.com",
        "phone": "+91-9999888877",
        "password": "securepassword123"
    })
    assert res.status_code == 201
    data = res.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert data["user"]["email"] == "newuser@example.com"
    assert data["user"]["name"] == "New User"
    assert data["user"]["role"] == "USER"


def test_auth_duplicate_registration_fails():
    res = client.post("/api/auth/register", json={
        "name": "Duplicate User",
        "email": "newuser@example.com",
        "password": "securepassword123"
    })
    assert res.status_code == 409
    assert "already exists" in res.json()["detail"].lower()


def test_auth_registration_validation_error():
    res = client.post("/api/auth/register", json={
        "name": "X",
        "email": "not-an-email",
        "password": "123"
    })
    assert res.status_code == 422


def test_auth_successful_login():
    res = client.post("/api/auth/login", json={
        "email": "newuser@example.com",
        "password": "securepassword123"
    })
    assert res.status_code == 200
    data = res.json()
    assert "access_token" in data
    assert data["user"]["email"] == "newuser@example.com"


def test_auth_login_invalid_password():
    res = client.post("/api/auth/login", json={
        "email": "newuser@example.com",
        "password": "wrongpassword"
    })
    assert res.status_code == 401
    assert "incorrect email or password" in res.json()["detail"].lower()


def test_auth_login_nonexistent_email():
    res = client.post("/api/auth/login", json={
        "email": "doesnotexist@example.com",
        "password": "somepassword"
    })
    assert res.status_code == 401
    assert "incorrect email or password" in res.json()["detail"].lower()


def test_auth_get_me():
    token = get_auth_token()
    res = client.get("/api/auth/me", headers={"Authorization": f"Bearer {token}"})
    assert res.status_code == 200
    assert res.json()["email"] == "admin@rnps.com"
    assert res.json()["role"] == "ADMIN"


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


# ─── Pricing Tests (₹10, ₹20 & End-to-End Calculation) ────────────────────────

def test_configured_ticket_price_10_and_20():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    # Get movie and screen
    movies = client.get("/api/movies/", headers=headers).json()
    screens = client.get("/api/admin/screens/", headers=headers).json()
    movie_id = movies[0]["id"]
    screen_id = screens[0]["id"]

    # ─── TEST CASE A: Configure ticket price ₹10 ───
    show_10_res = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movie_id,
            "screen_id": screen_id,
            "start_time": "2026-10-15T10:00:00",
            "end_time": "2026-10-15T12:30:00",
            "base_price": 10.0
        }
    )
    assert show_10_res.status_code == 201
    show_10_id = show_10_res.json()["id"]

    # Verify seat prices returned by /shows/{show_id}/seats are ₹10
    seats_10_data = client.get(f"/api/shows/{show_10_id}/seats", headers=headers).json()
    avail_10 = [s for s in seats_10_data["seats"] if s["status"] == "AVAILABLE"]
    assert len(avail_10) >= 2
    for s in avail_10:
        assert s["price"] == 10.0, f"Expected 10.0, got {s['price']}"

    # Select 1 seat -> ₹10 ticket subtotal
    book_1_seat = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_10_id, "seat_ids": [avail_10[0]["seat_id"]]}
    )
    assert book_1_seat.status_code == 201
    b1_data = book_1_seat.json()
    assert b1_data["ticket_amount"] == 10.0
    assert b1_data["total_amount"] == 10.0
    assert b1_data["convenience_fee"] == 0.0

    # Select 2 seats -> ₹20 ticket subtotal
    show_10_res_b = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movie_id,
            "screen_id": screen_id,
            "start_time": "2026-10-15T14:00:00",
            "end_time": "2026-10-15T16:30:00",
            "base_price": 10.0
        }
    )
    show_10_b_id = show_10_res_b.json()["id"]
    seats_10_b = client.get(f"/api/shows/{show_10_b_id}/seats", headers=headers).json()
    avail_10_b = [s for s in seats_10_b["seats"] if s["status"] == "AVAILABLE"]

    book_2_seats = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_10_b_id, "seat_ids": [avail_10_b[0]["seat_id"], avail_10_b[1]["seat_id"]]}
    )
    assert book_2_seats.status_code == 201
    b2_data = book_2_seats.json()
    assert b2_data["ticket_amount"] == 20.0
    assert b2_data["total_amount"] == 20.0
    assert b2_data["convenience_fee"] == 0.0

    # Add snacks to 2-seat booking
    snacks = client.get("/api/snacks/", headers=headers).json()
    snack = snacks[0]
    snack_qty = 2
    snack_cost = snack["price"] * snack_qty

    add_snack_res = client.post(
        f"/api/snacks/booking/{b2_data['id']}",
        headers=headers,
        json={"items": [{"snack_id": snack["id"], "quantity": snack_qty}]}
    )
    assert add_snack_res.status_code == 200
    snack_b_data = add_snack_res.json()
    assert snack_b_data["ticket_amount"] == 20.0
    assert snack_b_data["snacks_amount"] == snack_cost
    assert snack_b_data["total_amount"] == round(20.0 + snack_cost, 2)

    # ─── TEST CASE B: Configure ticket price ₹20 ───
    show_20_res = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movie_id,
            "screen_id": screen_id,
            "start_time": "2026-10-16T18:00:00",
            "end_time": "2026-10-16T20:30:00",
            "base_price": 20.0
        }
    )
    assert show_20_res.status_code == 201
    show_20_id = show_20_res.json()["id"]

    # Verify seat prices returned by /shows/{show_id}/seats are ₹20 (not 250)
    seats_20_data = client.get(f"/api/shows/{show_20_id}/seats", headers=headers).json()
    avail_20 = [s for s in seats_20_data["seats"] if s["status"] == "AVAILABLE"]
    for s in avail_20:
        assert s["price"] == 20.0, f"Expected 20.0, got {s['price']}"
        assert s["price"] != 250.0

    # 1 seat -> ₹20
    b_20_1 = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_20_id, "seat_ids": [avail_20[0]["seat_id"]]}
    )
    assert b_20_1.status_code == 201
    assert b_20_1.json()["ticket_amount"] == 20.0
    assert b_20_1.json()["total_amount"] == 20.0


def test_admin_payment_approval_and_seat_release():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    # Create show with price ₹15
    movies = client.get("/api/movies/", headers=headers).json()
    screens = client.get("/api/admin/screens/", headers=headers).json()
    show_res = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movies[0]["id"],
            "screen_id": screens[0]["id"],
            "start_time": "2026-10-17T18:00:00",
            "end_time": "2026-10-17T20:30:00",
            "base_price": 15.0
        }
    )
    show_id = show_res.json()["id"]
    seats_data = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    avail = [s for s in seats_data["seats"] if s["status"] == "AVAILABLE"]
    target_seat = avail[0]

    # Create booking
    b_res = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_id, "seat_ids": [target_seat["seat_id"]]}
    )
    assert b_res.status_code == 201
    b_id = b_res.json()["id"]

    # Seat should now be HELD (temporary hold, not yet permanently BOOKED)
    seats_after = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    s_state = [s for s in seats_after["seats"] if s["seat_id"] == target_seat["seat_id"]][0]
    assert s_state["status"] == "HELD"

    # Submit payment -> marks seat permanently as BOOKED
    pay_res = client.post(
        "/api/payments/submit",
        headers=headers,
        json={"booking_id": b_id, "payment_method": "UPI", "transaction_reference": "TXN-TEST-REJECT"}
    )
    seats_after_pay = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    s_paid = [s for s in seats_after_pay["seats"] if s["seat_id"] == target_seat["seat_id"]][0]
    assert s_paid["status"] == "BOOKED"
    payment_id = pay_res.json()["payment"]["id"]

    # Admin rejects payment -> seat should be released back to AVAILABLE
    reject_res = client.post(
        f"/api/admin/payments/{payment_id}/verify",
        headers=headers,
        json={"action": "REJECT"}
    )
    assert reject_res.status_code == 200
    assert reject_res.json()["status"] == "CANCELLED"

    # Check seat is released
    seats_after_reject = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    s_released = [s for s in seats_after_reject["seats"] if s["seat_id"] == target_seat["seat_id"]][0]
    assert s_released["status"] == "AVAILABLE"


def test_show_price_update_syncs_available_seats_only():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    movies = client.get("/api/movies/", headers=headers).json()
    screens = client.get("/api/admin/screens/", headers=headers).json()

    # 1. Create show with initial base_price = 50.0
    show_res = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movies[0]["id"],
            "screen_id": screens[0]["id"],
            "start_time": "2026-10-18T10:00:00",
            "end_time": "2026-10-18T12:30:00",
            "base_price": 50.0
        }
    )
    assert show_res.status_code == 201
    show_id = show_res.json()["id"]

    # 2. Book 1 seat at ₹50
    seats_data = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    avail = [s for s in seats_data["seats"] if s["status"] == "AVAILABLE"]
    seat_1 = avail[0]
    seat_2 = avail[1]

    book_1 = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_id, "seat_ids": [seat_1["seat_id"]]}
    )
    assert book_1.status_code == 201
    b1_data = book_1.json()
    assert b1_data["ticket_amount"] == 50.0
    assert b1_data["total_amount"] == 50.0

    # 3. Admin updates show price to ₹20.0
    update_res = client.put(
        f"/api/admin/shows/{show_id}",
        headers=headers,
        json={"base_price": 20.0}
    )
    assert update_res.status_code == 200
    assert update_res.json()["base_price"] == 20.0

    # 4. Fetch seats: AVAILABLE seats must now be ₹20, BOOKED seat must retain ₹50
    updated_seats = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()["seats"]
    for s in updated_seats:
        if s["status"] == "AVAILABLE":
            assert s["price"] == 20.0, f"Expected 20.0 for available seat, got {s['price']}"
        elif s["seat_id"] == seat_1["seat_id"]:
            assert s["price"] == 50.0, f"Expected 50.0 for booked seat, got {s['price']}"

    # 5. Book a second seat: must cost ₹20
    book_2 = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_id, "seat_ids": [seat_2["seat_id"]]}
    )
    assert book_2.status_code == 201
    b2_data = book_2.json()
    assert b2_data["ticket_amount"] == 20.0
    assert b2_data["total_amount"] == 20.0


def test_stale_250_seats_sync_on_retrieval_and_booking():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    from app.models.models import ShowSeat

    movies = client.get("/api/movies/", headers=headers).json()
    screens = client.get("/api/admin/screens/", headers=headers).json()

    # 1. Create show with price ₹10
    show_res = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movies[0]["id"],
            "screen_id": screens[0]["id"],
            "start_time": "2026-10-19T10:00:00",
            "end_time": "2026-10-19T12:30:00",
            "base_price": 10.0
        }
    )
    show_id = show_res.json()["id"]

    # 2. Artificially simulate stale ₹250 prices in database for this show's seats
    db = TestingSessionLocal()
    try:
        db.query(ShowSeat).filter(ShowSeat.show_id == show_id).update({ShowSeat.price: 250.0})
        db.commit()
    finally:
        db.close()

    # 3. GET /shows/{show_id}/seats must detect show.base_price=10 and self-heal the seats to ₹10
    seats_data = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    for s in seats_data["seats"]:
        assert s["price"] == 10.0, f"Expected self-healed price 10.0, got {s['price']}"

    # 4. Verify in DB that ShowSeat.price was synchronized to 10.0
    db = TestingSessionLocal()
    try:
        db_seats = db.query(ShowSeat).filter(ShowSeat.show_id == show_id).all()
        for dbs in db_seats:
            assert dbs.price == 10.0
    finally:
        db.close()

    # 5. Booking 3 seats directly must yield 3 x 10.0 = ₹30.0
    avail_seats = [s for s in seats_data["seats"] if s["status"] == "AVAILABLE"]
    chosen_ids = [s["seat_id"] for s in avail_seats[:3]]
    b_res = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_id, "seat_ids": chosen_ids}
    )
    assert b_res.status_code == 201
    b_data = b_res.json()
    assert b_data["ticket_amount"] == 30.0
    assert b_data["total_amount"] == 30.0
    assert b_data["convenience_fee"] == 0.0

    # 6. Submit payment and verify payment amount matches booking total exactly
    pay_res = client.post(
        "/api/payments/submit",
        headers=headers,
        json={"booking_id": b_data["id"], "payment_method": "UPI", "transaction_reference": "TXN-SYNC-TEST"}
    )
    assert pay_res.status_code == 200
    assert pay_res.json()["payment"]["amount"] == 30.0


def test_show_creation_without_end_time_auto_derives_duration():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    # Create a movie with 120 minutes duration
    movie_res = client.post(
        "/api/admin/movies/",
        headers=headers,
        json={
            "title": "Test Duration Movie",
            "duration_minutes": 120,
            "language": "Tamil",
            "is_upcoming": False
        }
    )
    assert movie_res.status_code == 201
    movie_id = movie_res.json()["id"]

    screens = client.get("/api/admin/screens/", headers=headers).json()
    screen_id = screens[0]["id"]

    # Create show without end_time
    show_res = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movie_id,
            "screen_id": screen_id,
            "start_time": "2026-10-25T14:30:00",
            "base_price": 50.0
        }
    )
    assert show_res.status_code == 201
    show_data = show_res.json()
    assert show_data["start_time"] == "2026-10-25T14:30:00"
    # End time should be exactly 120 minutes later (16:30:00)
    assert show_data["end_time"] == "2026-10-25T16:30:00"


def test_movie_status_now_showing_and_upcoming_separation_and_update():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Create an UPCOMING movie
    upcoming_res = client.post(
        "/api/admin/movies/",
        headers=headers,
        json={
            "title": "Upcoming Blockbuster",
            "duration_minutes": 150,
            "language": "Tamil",
            "is_upcoming": True
        }
    )
    assert upcoming_res.status_code == 201
    upcoming_movie = upcoming_res.json()
    assert upcoming_movie["is_upcoming"] is True
    upcoming_id = upcoming_movie["id"]

    # 2. Create a NOW SHOWING movie
    showing_res = client.post(
        "/api/admin/movies/",
        headers=headers,
        json={
            "title": "Now Showing Hit",
            "duration_minutes": 135,
            "language": "Tamil",
            "is_upcoming": False
        }
    )
    assert showing_res.status_code == 201
    showing_movie = showing_res.json()
    assert showing_movie["is_upcoming"] is False
    showing_id = showing_movie["id"]

    # 3. Test filtering: only upcoming
    upcoming_list = client.get("/api/movies/?is_upcoming=true", headers=headers).json()
    upcoming_ids = [m["id"] for m in upcoming_list]
    assert upcoming_id in upcoming_ids
    assert showing_id not in upcoming_ids

    # 4. Test filtering: only now showing
    showing_list = client.get("/api/movies/?is_upcoming=false", headers=headers).json()
    showing_ids = [m["id"] for m in showing_list]
    assert showing_id in showing_ids
    assert upcoming_id not in showing_ids

    # 5. Update upcoming movie to NOW SHOWING
    update_res = client.put(
        f"/api/admin/movies/{upcoming_id}",
        headers=headers,
        json={"is_upcoming": False}
    )
    assert update_res.status_code == 200
    assert update_res.json()["is_upcoming"] is False

    # 6. Verify it now appears in NOW SHOWING list
    refreshed_showing_list = client.get("/api/movies/?is_upcoming=false", headers=headers).json()
    refreshed_ids = [m["id"] for m in refreshed_showing_list]
    assert upcoming_id in refreshed_ids


# ─── Seat Hold & Concurrent Booking Tests ─────────────────────────────────────

def test_seat_hold_and_cancel_releases_seats():
    """Verify that creating a booking places a hold, and cancelling releases seats back to AVAILABLE."""
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    # 1. Create a dedicated show
    movies = client.get("/api/movies/", headers=headers).json()
    screens = client.get("/api/admin/screens/", headers=headers).json()
    show_res = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movies[0]["id"],
            "screen_id": screens[0]["id"],
            "start_time": "2026-10-19T10:00:00",
            "end_time": "2026-10-19T12:30:00",
            "base_price": 25.0
        }
    )
    assert show_res.status_code == 201
    show_id = show_res.json()["id"]

    # 2. Pick an available seat
    seats_data = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    seat = [s for s in seats_data["seats"] if s["status"] == "AVAILABLE"][0]
    seat_id = seat["seat_id"]

    # 3. Book the seat -> becomes HELD
    b_res = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_id, "seat_ids": [seat_id]}
    )
    assert b_res.status_code == 201
    booking_id = b_res.json()["id"]

    seats_after_book = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    s_held = [s for s in seats_after_book["seats"] if s["seat_id"] == seat_id][0]
    assert s_held["status"] == "HELD"

    # 4. User cancels / abandons the booking flow before submitting payment
    cancel_res = client.post(f"/api/bookings/{booking_id}/cancel", headers=headers)
    assert cancel_res.status_code == 200

    # 5. Verify the seat is back to AVAILABLE
    seats_after_cancel = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    s_avail = [s for s in seats_after_cancel["seats"] if s["seat_id"] == seat_id][0]
    assert s_avail["status"] == "AVAILABLE"


def test_concurrent_booking_conflict():
    """Verify that two users trying to book the same seat results in a 409 Conflict for the second user."""
    admin_token = get_auth_token()
    headers_admin = {"Authorization": f"Bearer {admin_token}"}

    # Register user 2
    reg_res = client.post("/api/auth/register", json={
        "name": "User Two",
        "email": "user2@concurrent.com",
        "password": "password123"
    })
    token_user2 = reg_res.json()["access_token"] if reg_res.status_code == 201 else client.post("/api/auth/login", json={
        "email": "user2@concurrent.com",
        "password": "password123"
    }).json()["access_token"]
    headers_user2 = {"Authorization": f"Bearer {token_user2}"}

    # Create show
    movies = client.get("/api/movies/", headers=headers_admin).json()
    screens = client.get("/api/admin/screens/", headers=headers_admin).json()
    show_res = client.post(
        "/api/admin/shows/",
        headers=headers_admin,
        json={
            "movie_id": movies[0]["id"],
            "screen_id": screens[0]["id"],
            "start_time": "2026-10-20T10:00:00",
            "end_time": "2026-10-20T12:30:00",
            "base_price": 25.0
        }
    )
    show_id = show_res.json()["id"]

    # Pick seat
    seats = client.get(f"/api/shows/{show_id}/seats", headers=headers_admin).json()["seats"]
    target_seat_id = [s for s in seats if s["status"] == "AVAILABLE"][0]["seat_id"]

    # User 1 books the seat
    b1 = client.post(
        "/api/bookings/",
        headers=headers_admin,
        json={"show_id": show_id, "seat_ids": [target_seat_id]}
    )
    assert b1.status_code == 201

    # User 2 attempts to book the same seat -> 409 CONFLICT
    b2 = client.post(
        "/api/bookings/",
        headers=headers_user2,
        json={"show_id": show_id, "seat_ids": [target_seat_id]}
    )
    assert b2.status_code == 409
    assert "no longer available" in b2.json()["detail"].lower()


def test_cannot_cancel_booking_after_payment_submitted():
    """Verify that once payment is submitted, pressing cancel refuses with 400 Bad Request."""
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    # Create show
    movies = client.get("/api/movies/", headers=headers).json()
    screens = client.get("/api/admin/screens/", headers=headers).json()
    show_res = client.post(
        "/api/admin/shows/",
        headers=headers,
        json={
            "movie_id": movies[0]["id"],
            "screen_id": screens[0]["id"],
            "start_time": "2026-10-21T10:00:00",
            "end_time": "2026-10-21T12:30:00",
            "base_price": 30.0
        }
    )
    show_id = show_res.json()["id"]

    seats = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()["seats"]
    seat_id = [s for s in seats if s["status"] == "AVAILABLE"][0]["seat_id"]

    # Book seat
    b = client.post(
        "/api/bookings/",
        headers=headers,
        json={"show_id": show_id, "seat_ids": [seat_id]}
    )
    assert b.status_code == 201
    booking_id = b.json()["id"]

    # Submit payment
    pay = client.post(
        "/api/payments/submit",
        headers=headers,
        json={"booking_id": booking_id, "payment_method": "UPI", "transaction_reference": "TXN-PAID-IMMUTABLE"}
    )
    assert pay.status_code == 200

    # Attempt cancel -> MUST FAIL with 400
    cancel_res = client.post(f"/api/bookings/{booking_id}/cancel", headers=headers)
    assert cancel_res.status_code == 400
    assert "cannot cancel booking" in cancel_res.json()["detail"].lower()


# ─── Regression Tests for My Bookings Route and Filters ────────────────────────

def test_get_my_bookings_returns_200_not_422_int_parsing_error():
    """
    Regression test for critical bug:
    Ensures GET /api/bookings/my is routed to get_my_bookings instead of being
    swallowed by GET /api/bookings/{booking_id} with 'my' parsed as an integer (HTTP 422).
    """
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    res = client.get("/api/bookings/my", headers=headers)
    assert res.status_code == 200, f"Expected 200 OK but got {res.status_code}: {res.text}"
    data = res.json()
    assert isinstance(data, list)


def test_get_my_bookings_status_filters_and_user_isolation():
    """
    Verify All, PENDING, CONFIRMED, and CANCELLED status filter tabs on /api/bookings/my,
    and verify authenticated user isolation.
    """
    # 1. Register a fresh user
    user1_email = "mybookings_test1@example.com"
    r1 = client.post("/api/auth/register", json={
        "name": "Bookings Tester",
        "email": user1_email,
        "password": "password123"
    })
    token1 = r1.json()["access_token"]
    h1 = {"Authorization": f"Bearer {token1}"}

    # Verify initially empty
    empty_res = client.get("/api/bookings/my", headers=h1)
    assert empty_res.status_code == 200
    assert empty_res.json() == []

    # Admin creates a show
    admin_token = get_auth_token()
    h_admin = {"Authorization": f"Bearer {admin_token}"}
    movies = client.get("/api/movies/", headers=h_admin).json()
    screens = client.get("/api/admin/screens/", headers=h_admin).json()
    show_res = client.post(
        "/api/admin/shows/",
        headers=h_admin,
        json={
            "movie_id": movies[0]["id"],
            "screen_id": screens[0]["id"],
            "start_time": "2026-10-21T18:00:00",
            "end_time": "2026-10-21T20:30:00",
            "base_price": 40.0
        }
    )
    show_id = show_res.json()["id"]
    seats = client.get(f"/api/shows/{show_id}/seats", headers=h_admin).json()["seats"]
    avail = [s for s in seats if s["status"] == "AVAILABLE"]

    # User 1 creates Booking 1 (stays PENDING)
    b1 = client.post("/api/bookings/", headers=h1, json={"show_id": show_id, "seat_ids": [avail[0]["seat_id"]]}).json()

    # User 1 creates Booking 2 and submits payment (CONFIRMED)
    b2 = client.post("/api/bookings/", headers=h1, json={"show_id": show_id, "seat_ids": [avail[1]["seat_id"]]}).json()
    pay_res = client.post("/api/payments/submit", headers=h1, json={
        "booking_id": b2["id"],
        "payment_method": "UPI",
        "transaction_reference": "TXN-CONFIRMED-MY"
    }).json()
    payment_id = pay_res["payment"]["id"]
    # Admin approves payment -> booking confirmed
    approve_res = client.post(f"/api/admin/payments/{payment_id}/verify", headers=h_admin, json={"action": "APPROVE"})
    assert approve_res.status_code == 200
    assert approve_res.json()["status"] == "CONFIRMED"

    # User 1 creates Booking 3 and cancels it (CANCELLED)
    b3 = client.post("/api/bookings/", headers=h1, json={"show_id": show_id, "seat_ids": [avail[2]["seat_id"]]}).json()
    client.post(f"/api/bookings/{b3['id']}/cancel", headers=h1)

    # Test ALL
    all_res = client.get("/api/bookings/my", headers=h1).json()
    all_ids = {b["id"] for b in all_res}
    assert b1["id"] in all_ids
    assert b2["id"] in all_ids
    assert b3["id"] in all_ids

    # Test PENDING filter
    pending_res = client.get("/api/bookings/my?status=PENDING", headers=h1).json()
    assert any(b["id"] == b1["id"] for b in pending_res)
    assert not any(b["id"] == b2["id"] for b in pending_res)
    assert not any(b["id"] == b3["id"] for b in pending_res)

    # Test CONFIRMED filter
    confirmed_res = client.get("/api/bookings/my?status=CONFIRMED", headers=h1).json()
    assert any(b["id"] == b2["id"] for b in confirmed_res)
    assert not any(b["id"] == b1["id"] for b in confirmed_res)
    assert not any(b["id"] == b3["id"] for b in confirmed_res)

    # Test CANCELLED filter
    cancelled_res = client.get("/api/bookings/my?status=CANCELLED", headers=h1).json()
    assert any(b["id"] == b3["id"] for b in cancelled_res)
    assert not any(b["id"] == b1["id"] for b in cancelled_res)
    assert not any(b["id"] == b2["id"] for b in cancelled_res)

    # User 2 cannot see User 1's bookings
    r2 = client.post("/api/auth/register", json={
        "name": "Another User",
        "email": "another_user_iso@example.com",
        "password": "password123"
    })
    token2 = r2.json()["access_token"]
    h2 = {"Authorization": f"Bearer {token2}"}
    user2_res = client.get("/api/bookings/my", headers=h2).json()
    assert user2_res == []


# ─── Feature 1 & 2: Admin Booking Details, QR Scanner & Check-in Tests ────────

def _create_test_show(h_admin: dict, days_offset: int = 1) -> int:
    """Helper to create an isolated show with fresh available seats."""
    movies = client.get("/api/movies/", headers=h_admin).json()
    movie_id = movies[0]["id"]
    screens = client.get("/api/admin/screens/", headers=h_admin).json()
    screen_id = screens[0]["id"]
    t_start = datetime.utcnow() + timedelta(days=days_offset, hours=2)
    t_end = t_start + timedelta(hours=2, minutes=30)
    res = client.post("/api/admin/shows/", headers=h_admin, json={
        "movie_id": movie_id,
        "screen_id": screen_id,
        "start_time": t_start.isoformat(),
        "end_time": t_end.isoformat(),
        "base_price": 100.0
    })
    return res.json()["id"]


def test_admin_booking_details_complete_customer_info_and_ist_separation():
    """
    Test Feature 1:
    - Customer information (full name, email, phone, user ID) is returned from linked user record.
    - Booking creation timestamp and movie show timestamp are distinct fields.
    - Multiple seats and snacks calculation are properly included.
    """
    admin_token = get_auth_token()
    h_admin = {"Authorization": f"Bearer {admin_token}"}

    # Register a specific customer with phone number
    c_res = client.post("/api/auth/register", json={
        "name": "Karthik Raja",
        "email": "karthik.raja@example.com",
        "phone": "+91-9840123456",
        "password": "customerpass123"
    })
    c_data = c_res.json()
    c_token = c_data["access_token"]
    c_user_id = c_data["user"]["id"]
    h_cust = {"Authorization": f"Bearer {c_token}"}

    show_id = _create_test_show(h_admin, days_offset=2)
    seats = client.get(f"/api/shows/{show_id}/seats", headers=h_cust).json()["seats"]
    avail = [s for s in seats if s["status"] == "AVAILABLE"]
    assert len(avail) >= 2
    seat_ids = [avail[0]["seat_id"], avail[1]["seat_id"]]

    # Create multi-seat booking
    b_res = client.post("/api/bookings/", headers=h_cust, json={
        "show_id": show_id,
        "seat_ids": seat_ids
    })
    assert b_res.status_code == 201
    booking = b_res.json()
    booking_id = booking["id"]

    # Add snacks
    snacks = client.get("/api/snacks/", headers=h_cust).json()
    if snacks:
        snack_id = snacks[0]["id"]
        client.post(f"/api/snacks/booking/{booking_id}", headers=h_cust, json={
            "items": [{"snack_id": snack_id, "quantity": 2}]
        })

    # Retrieve via admin booking endpoint
    admin_bookings = client.get("/api/admin/bookings/", headers=h_admin).json()
    target = next((b for b in admin_bookings if b["id"] == booking_id), None)
    assert target is not None, "Booking must appear in admin booking list immediately"

    # Verify customer info
    assert target["user"] is not None
    assert target["user"]["id"] == c_user_id
    assert target["user"]["name"] == "Karthik Raja"
    assert target["user"]["email"] == "karthik.raja@example.com"
    assert target["user"]["phone"] == "+91-9840123456"

    # Verify timestamps are distinct
    assert target["created_at"] is not None
    assert target["show"]["start_time"] is not None
    assert target["created_at"] != target["show"]["start_time"]

    # Verify seats and snacks subtotal
    assert target["total_seats"] == 2
    assert len(target["seats"]) == 2
    assert target["ticket_amount"] > 0
    if snacks:
        assert target["snacks_amount"] > 0
        assert len(target["snacks"]) >= 1


def test_admin_booking_search_and_filters():
    """Test searching by booking number, customer name, email, and status filters."""
    admin_token = get_auth_token()
    h_admin = {"Authorization": f"Bearer {admin_token}"}

    # Search by customer name
    res_name = client.get("/api/admin/bookings/?search=Karthik", headers=h_admin).json()
    assert any(b.get("user") and "Karthik" in b["user"]["name"] for b in res_name)

    # Search by email
    res_email = client.get("/api/admin/bookings/?search=karthik.raja@example.com", headers=h_admin).json()
    assert any(b.get("user") and "karthik.raja@example.com" == b["user"]["email"] for b in res_email)

    # Filter by status
    res_pending = client.get("/api/admin/bookings/?status_filter=PENDING", headers=h_admin).json()
    for b in res_pending:
        assert b["status"] == "PENDING" or (b["payment"] and b["payment"]["status"] in ["PENDING", "SUBMITTED"])


def test_admin_unauthorized_access():
    """Ensure non-admin users cannot access admin booking or verification endpoints."""
    # Register regular user
    u_res = client.post("/api/auth/register", json={
        "name": "Standard User",
        "email": "standard.user@example.com",
        "password": "standardpass123"
    })
    token = u_res.json()["access_token"]
    h_user = {"Authorization": f"Bearer {token}"}

    # Attempt to access admin endpoints
    assert client.get("/api/admin/bookings/", headers=h_user).status_code == 403
    assert client.get("/api/admin/tickets/verify/BK-NONEXISTENT", headers=h_user).status_code == 403
    assert client.post("/api/admin/tickets/check-in/BK-NONEXISTENT", headers=h_user).status_code == 403

    # Attempt unauthenticated
    assert client.get("/api/admin/bookings/").status_code in [401, 403]
    assert client.get("/api/admin/tickets/verify/BK-NONEXISTENT").status_code in [401, 403]


def test_admin_ticket_qr_verification_states():
    """
    Test Feature 2:
    - Valid ticket verification (PAID + CONFIRMED)
    - JSON embedded QR code decoding
    - Unpaid ticket verification
    - Cancelled ticket verification
    - Invalid ticket code 404
    """
    admin_token = get_auth_token()
    h_admin = {"Authorization": f"Bearer {admin_token}"}

    # 1. Test non-existent ticket -> 404
    err_res = client.get("/api/admin/tickets/verify/INVALID-TICKET-CODE-XYZ", headers=h_admin)
    assert err_res.status_code == 404

    # 2. Create customer and booking
    c_res = client.post("/api/auth/register", json={
        "name": "QR Customer",
        "email": "qr.customer@example.com",
        "phone": "+91-9988776655",
        "password": "qrpassword123"
    })
    c_token = c_res.json()["access_token"]
    h_cust = {"Authorization": f"Bearer {c_token}"}

    show_id = _create_test_show(h_admin, days_offset=3)
    seats = client.get(f"/api/shows/{show_id}/seats", headers=h_cust).json()["seats"]
    avail = [s for s in seats if s["status"] == "AVAILABLE"]
    seat_id = avail[0]["seat_id"]

    b_res = client.post("/api/bookings/", headers=h_cust, json={
        "show_id": show_id,
        "seat_ids": [seat_id]
    })
    assert b_res.status_code == 201
    booking_data = b_res.json()
    b_id = booking_data["id"]
    b_ref = booking_data["booking_reference"]

    # Submit payment to generate ticket
    pay_res = client.post("/api/payments/submit", headers=h_cust, json={
        "booking_id": b_id,
        "payment_method": "UPI",
        "transaction_reference": "UPI-VERIFY-TEST-001"
    }).json()
    t_code = pay_res["ticket"]["ticket_code"]
    p_id = pay_res["payment"]["id"]

    # Before admin approval: status is SUBMITTED -> ticket must be UNPAID / not valid admission
    unpaid_res = client.get(f"/api/admin/tickets/verify/{t_code}", headers=h_admin).json()
    assert unpaid_res["is_valid"] is False
    assert unpaid_res["validity_status"] == "UNPAID"
    assert unpaid_res["user_name"] == "QR Customer"
    assert unpaid_res["booking_reference"] == b_ref

    # Also test verification using booking_reference instead of ticket_code
    unpaid_ref_res = client.get(f"/api/admin/tickets/verify/{b_ref}", headers=h_admin).json()
    assert unpaid_ref_res["is_valid"] is False
    assert unpaid_ref_res["validity_status"] == "UNPAID"

    # Also test verification with JSON encoded QR payload
    import json
    qr_payload_str = json.dumps({"ticket_code": t_code, "booking_ref": b_ref})
    json_verify_res = client.get(f"/api/admin/tickets/verify/{qr_payload_str}", headers=h_admin).json()
    assert json_verify_res["ticket_code"] == t_code

    # Admin approves payment with remarks
    approve_res = client.post(f"/api/admin/payments/{p_id}/verify", headers=h_admin, json={
        "action": "APPROVE",
        "remarks": "Payment verified via bank ref"
    })
    assert approve_res.status_code == 200
    assert approve_res.json()["payment"]["remarks"] == "Payment verified via bank ref"
    assert approve_res.json()["payment"]["verified_by_name"] == "Admin"

    # Now verify ticket: must be VALID!
    valid_res = client.get(f"/api/admin/tickets/verify/{t_code}", headers=h_admin).json()
    assert valid_res["is_valid"] is True
    assert valid_res["validity_status"] == "VALID"
    assert valid_res["user_name"] == "QR Customer"
    assert valid_res["customer_email"] == "qr.customer@example.com"
    assert valid_res["customer_phone"] == "+91-9988776655"
    assert valid_res["ticket_count"] == 1
    assert len(valid_res["seats"]) == 1
    assert valid_res["is_used"] is False


def test_admin_ticket_atomic_check_in_and_duplicate_prevention():
    """
    Test Feature 2 C:
    - Atomically check in / admit patron using ticket code.
    - Subsequent attempt to check in the same ticket returns 409 Conflict.
    """
    admin_token = get_auth_token()
    h_admin = {"Authorization": f"Bearer {admin_token}"}

    c_res = client.post("/api/auth/register", json={
        "name": "Admit Patron",
        "email": "admit.patron@example.com",
        "password": "password123"
    })
    h_cust = {"Authorization": f"Bearer {c_res.json()['access_token']}"}

    show_id = _create_test_show(h_admin, days_offset=4)
    seats = client.get(f"/api/shows/{show_id}/seats", headers=h_cust).json()["seats"]
    avail = [s for s in seats if s["status"] == "AVAILABLE"]
    seat_id = avail[0]["seat_id"]

    b_res = client.post("/api/bookings/", headers=h_cust, json={
        "show_id": show_id,
        "seat_ids": [seat_id]
    })
    assert b_res.status_code == 201

    pay_res = client.post("/api/payments/submit", headers=h_cust, json={
        "booking_id": b_res.json()["id"],
        "payment_method": "UPI",
        "transaction_reference": "UPI-CHECKIN-001"
    }).json()

    t_code = pay_res["ticket"]["ticket_code"]
    p_id = pay_res["payment"]["id"]

    # Admin approves
    client.post(f"/api/admin/payments/{p_id}/verify", headers=h_admin, json={"action": "APPROVE"})

    # 1. First check-in: SUCCESS
    ci_res = client.post(f"/api/admin/tickets/check-in/{t_code}", headers=h_admin)
    assert ci_res.status_code == 200
    ci_data = ci_res.json()
    assert ci_data["is_used"] is True
    assert ci_data["validity_status"] == "ALREADY_USED"
    assert ci_data["used_at"] is not None

    # 2. Second check-in attempt: MUST FAIL with 409 Conflict to prevent duplicate entry!
    dup_res = client.post(f"/api/admin/tickets/check-in/{t_code}", headers=h_admin)
    assert dup_res.status_code == 409
    assert "already been used" in dup_res.json()["detail"].lower()

    # 3. Verify ticket status shows ALREADY_USED
    status_res = client.get(f"/api/admin/tickets/verify/{t_code}", headers=h_admin).json()
    assert status_res["is_valid"] is False
    assert status_res["validity_status"] == "ALREADY_USED"
    assert status_res["is_used"] is True


def test_get_my_bookings_invalid_status_filter_returns_400():
    """Verify that an invalid status filter value returns 400 Bad Request, never HTTP 500."""
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    res = client.get("/api/bookings/my?status=INVALID_STATUS_VALUE", headers=headers)
    assert res.status_code == 400
    assert "invalid status filter" in res.json()["detail"].lower()

    res_alias = client.get("/api/bookings/my?status_filter=NON_EXISTENT", headers=headers)
    assert res_alias.status_code == 400
    assert "invalid status filter" in res_alias.json()["detail"].lower()


def test_get_my_bookings_unauthorized_and_expired():
    """Verify that unauthenticated or invalid token requests return 401/403."""
    res_none = client.get("/api/bookings/my")
    assert res_none.status_code in [401, 403]

    res_bad = client.get("/api/bookings/my", headers={"Authorization": "Bearer invalid_token_123"})
    assert res_bad.status_code in [401, 403]


def test_get_booking_by_id_and_reference_and_user_isolation():
    """
    Test single booking retrieval:
    1. By integer ID
    2. By alphanumeric booking_reference (no 422 integer parsing error)
    3. User isolation: Customer cannot access another customer's booking
    4. Admin can access any customer's booking
    """
    admin_token = get_auth_token()
    h_admin = {"Authorization": f"Bearer {admin_token}"}

    # Register User A
    res_a = client.post("/api/auth/register", json={
        "name": "Customer User A",
        "email": "cust_a_iso@example.com",
        "password": "password123"
    })
    token_a = res_a.json()["access_token"]
    h_a = {"Authorization": f"Bearer {token_a}"}

    # Register User B
    res_b = client.post("/api/auth/register", json={
        "name": "Customer User B",
        "email": "cust_b_iso@example.com",
        "password": "password123"
    })
    token_b = res_b.json()["access_token"]
    h_b = {"Authorization": f"Bearer {token_b}"}

    # User A creates a booking
    show_id = _create_test_show(h_admin, days_offset=5)
    seats = client.get(f"/api/shows/{show_id}/seats", headers=h_a).json()["seats"]
    avail = [s for s in seats if s["status"] == "AVAILABLE"]
    b_res = client.post("/api/bookings/", headers=h_a, json={
        "show_id": show_id,
        "seat_ids": [avail[0]["seat_id"]]
    })
    assert b_res.status_code == 201
    booking_a = b_res.json()
    b_id = booking_a["id"]
    b_ref = booking_a["booking_reference"]

    # 1. User A retrieves by integer ID
    get_id_res = client.get(f"/api/bookings/{b_id}", headers=h_a)
    assert get_id_res.status_code == 200
    assert get_id_res.json()["id"] == b_id

    # 2. User A retrieves by booking reference (ensuring no 422 unable to parse string as integer)
    get_ref_res = client.get(f"/api/bookings/{b_ref}", headers=h_a)
    assert get_ref_res.status_code == 200
    assert get_ref_res.json()["booking_reference"] == b_ref

    # 3. User B attempts to access User A's booking by ID -> 404 (user isolation)
    iso_id_res = client.get(f"/api/bookings/{b_id}", headers=h_b)
    assert iso_id_res.status_code == 404

    # User B attempts to access User A's booking by ref -> 404
    iso_ref_res = client.get(f"/api/bookings/{b_ref}", headers=h_b)
    assert iso_ref_res.status_code == 404

    # 4. Admin CAN access User A's booking by ID and reference
    admin_get_id = client.get(f"/api/bookings/{b_id}", headers=h_admin)
    assert admin_get_id.status_code == 200
    assert admin_get_id.json()["id"] == b_id

    admin_get_ref = client.get(f"/api/bookings/{b_ref}", headers=h_admin)
    assert admin_get_ref.status_code == 200
    assert admin_get_ref.json()["booking_reference"] == b_ref



