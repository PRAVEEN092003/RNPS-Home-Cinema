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

    # Seat should now be BOOKED
    seats_after = client.get(f"/api/shows/{show_id}/seats", headers=headers).json()
    s_state = [s for s in seats_after["seats"] if s["seat_id"] == target_seat["seat_id"]][0]
    assert s_state["status"] == "BOOKED"

    # Submit payment
    pay_res = client.post(
        "/api/payments/submit",
        headers=headers,
        json={"booking_id": b_id, "payment_method": "UPI", "transaction_reference": "TXN-TEST-REJECT"}
    )
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

    from app.config.database import SessionLocal
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
    db = SessionLocal()
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
    db = SessionLocal()
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
