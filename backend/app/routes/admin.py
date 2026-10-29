"""
Admin management routes: Movie CRUD, Show CRUD, Snack CRUD, Payment verification & Ticket scanner.
"""
from typing import List, Optional
from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy.orm import Session, joinedload
from app.config.database import get_db
from app.models.models import (
    Payment, Booking, BookingSeat, BookingSnack, Ticket, ShowSeat, Seat, Movie, Show, Screen, Snack, User, SystemSetting,
    PaymentStatus, BookingStatus, ShowSeatStatus
)
from app.schemas.schemas import (
    BookingResponse, BookingSeatResponse, BookingSnackResponse, ShowResponse, PaymentResponse, TicketResponse, UserResponse,
    MovieCreateRequest, MovieUpdateRequest, MovieResponse,
    ShowCreateRequest, ShowUpdateRequest,
    SnackCreateRequest, SnackUpdateRequest, SnackResponse,
    ScreenResponse, ScreenUpdateRequest, PaymentConfigResponse, PaymentConfigRequest,
)
from app.utils.auth import require_admin

router = APIRouter(prefix="/admin", tags=["Admin Management"])


# ─── Movie Management ─────────────────────────────────────────────────────────

@router.post("/movies/", response_model=MovieResponse, status_code=status.HTTP_201_CREATED)
def create_movie(
    request: MovieCreateRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    movie = Movie(
        title=request.title,
        description=request.description,
        genre=request.genre,
        language=request.language,
        duration_minutes=request.duration_minutes,
        rating=request.rating,
        imdb_rating=request.imdb_rating,
        cast=request.cast,
        director=request.director,
        poster_url=request.poster_url,
        audio_technology=request.audio_technology,
        display_technology=request.display_technology,
        is_upcoming=request.is_upcoming,
        is_active=True,
    )
    db.add(movie)
    db.commit()
    db.refresh(movie)
    return MovieResponse.model_validate(movie)


@router.put("/movies/{movie_id}", response_model=MovieResponse)
def update_movie(
    movie_id: int,
    request: MovieUpdateRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Movie not found")

    for field, val in request.model_dump(exclude_unset=True).items():
        setattr(movie, field, val)

    db.commit()
    db.refresh(movie)
    return MovieResponse.model_validate(movie)


@router.delete("/movies/{movie_id}", response_model=MovieResponse)
def delete_movie(
    movie_id: int,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    movie = db.query(Movie).filter(Movie.id == movie_id).first()
    if not movie:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Movie not found")

    movie.is_active = False
    db.commit()
    db.refresh(movie)
    return MovieResponse.model_validate(movie)


# ─── Show Management ──────────────────────────────────────────────────────────

@router.post("/shows/", response_model=ShowResponse, status_code=status.HTTP_201_CREATED)
def create_show(
    request: ShowCreateRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    movie = db.query(Movie).filter(Movie.id == request.movie_id, Movie.is_active == True).first()
    if not movie:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Movie not found")

    screen = db.query(Screen).filter(Screen.id == request.screen_id, Screen.is_active == True).first()
    if not screen:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Screen not found")

    end_time = request.end_time
    if not end_time:
        duration = movie.duration_minutes if movie.duration_minutes else 150
        end_time = request.start_time + timedelta(minutes=duration)

    show = Show(
        movie_id=request.movie_id,
        screen_id=request.screen_id,
        start_time=request.start_time,
        end_time=end_time,
        base_price=request.base_price,
        is_active=True,
    )
    db.add(show)
    db.flush()

    # Automatically generate ShowSeat records for all seats in the screen
    seats = db.query(Seat).filter(Seat.screen_id == screen.id, Seat.is_active == True).all()
    configured_price = show.base_price if (show.base_price is not None and show.base_price > 0) else None
    show_seats = [
        ShowSeat(
            show_id=show.id,
            seat_id=seat.id,
            status=ShowSeatStatus.AVAILABLE,
            price=configured_price if configured_price is not None else seat.base_price,
        )
        for seat in seats
    ]
    db.add_all(show_seats)

    db.commit()
    db.refresh(show)

    show = db.query(Show).options(joinedload(Show.movie), joinedload(Show.screen)).filter(Show.id == show.id).first()
    return ShowResponse.model_validate(show)


@router.put("/shows/{show_id}", response_model=ShowResponse)
def update_show(
    show_id: int,
    request: ShowUpdateRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    show = db.query(Show).filter(Show.id == show_id).first()
    if not show:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Show not found")

    update_data = request.model_dump(exclude_unset=True)
    for field, val in update_data.items():
        setattr(show, field, val)

    if "start_time" in update_data and "end_time" not in update_data:
        movie = show.movie or db.query(Movie).filter(Movie.id == show.movie_id).first()
        duration = movie.duration_minutes if (movie and movie.duration_minutes) else 150
        show.end_time = show.start_time + timedelta(minutes=duration)

    if request.base_price is not None and request.base_price > 0:
        db.query(ShowSeat).filter(
            ShowSeat.show_id == show.id,
            ShowSeat.status == ShowSeatStatus.AVAILABLE
        ).update({ShowSeat.price: request.base_price}, synchronize_session=False)

    db.commit()
    db.refresh(show)
    show = db.query(Show).options(joinedload(Show.movie), joinedload(Show.screen)).filter(Show.id == show.id).first()
    return ShowResponse.model_validate(show)


@router.delete("/shows/{show_id}", response_model=ShowResponse)
def delete_show(
    show_id: int,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    show = db.query(Show).filter(Show.id == show_id).first()
    if not show:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Show not found")

    show.is_active = False
    db.commit()
    db.refresh(show)
    show = db.query(Show).options(joinedload(Show.movie), joinedload(Show.screen)).filter(Show.id == show.id).first()
    return ShowResponse.model_validate(show)


# ─── Snack Management ─────────────────────────────────────────────────────────

@router.post("/snacks/", response_model=SnackResponse, status_code=status.HTTP_201_CREATED)
def create_snack(
    request: SnackCreateRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    snack = Snack(
        name=request.name,
        description=request.description,
        category=request.category,
        price=request.price,
        image_url=request.image_url,
        is_available=True,
    )
    db.add(snack)
    db.commit()
    db.refresh(snack)
    return SnackResponse.model_validate(snack)


@router.put("/snacks/{snack_id}", response_model=SnackResponse)
def update_snack(
    snack_id: int,
    request: SnackUpdateRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    snack = db.query(Snack).filter(Snack.id == snack_id).first()
    if not snack:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Snack not found")

    for field, val in request.model_dump(exclude_unset=True).items():
        setattr(snack, field, val)

    db.commit()
    db.refresh(snack)
    return SnackResponse.model_validate(snack)


@router.delete("/snacks/{snack_id}", response_model=SnackResponse)
def delete_snack(
    snack_id: int,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    snack = db.query(Snack).filter(Snack.id == snack_id).first()
    if not snack:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Snack not found")

    snack.is_available = False
    db.commit()
    db.refresh(snack)
    return SnackResponse.model_validate(snack)


# ─── Booking Management Helper ────────────────────────────────────────────────

def _build_booking_response(b: Booking) -> BookingResponse:
    """Builds a complete, consistent BookingResponse including customer identity, pricing, and audit fields."""
    seats_resp = [
        BookingSeatResponse(
            id=bs.id,
            seat_id=bs.seat_id,
            seat_code=bs.seat.seat_code if bs.seat else "",
            row_label=bs.seat.row_label if bs.seat else "",
            seat_number=bs.seat.seat_number if bs.seat else 0,
            price=bs.price,
        )
        for bs in b.booking_seats
    ]
    snacks_resp = [
        BookingSnackResponse(
            id=bsn.id,
            snack_id=bsn.snack_id,
            snack_name=bsn.snack.name if bsn.snack else "Snack",
            quantity=bsn.quantity,
            unit_price=bsn.unit_price,
            total_price=bsn.total_price,
        )
        for bsn in b.booking_snacks
    ]
    ticket_subtotal = sum(bs.price for bs in b.booking_seats)
    snacks_subtotal = sum(bsn.total_price for bsn in b.booking_snacks)

    payment_resp = None
    if b.payment:
        payment_resp = PaymentResponse.model_validate(b.payment)
        if b.payment.verified_by:
            payment_resp.verified_by_name = b.payment.verified_by.name

    return BookingResponse(
        id=b.id,
        user_id=b.user_id,
        show_id=b.show_id,
        booking_reference=b.booking_reference,
        status=b.status,
        total_amount=b.total_amount,
        convenience_fee=b.convenience_fee,
        total_seats=b.total_seats,
        created_at=b.created_at,
        cancelled_at=b.cancelled_at or (b.updated_at if b.status == BookingStatus.CANCELLED else None),
        user=UserResponse.model_validate(b.user) if b.user else None,
        show=ShowResponse.model_validate(b.show) if b.show else None,
        seats=seats_resp,
        snacks=snacks_resp,
        ticket_amount=ticket_subtotal,
        snacks_amount=snacks_subtotal,
        payment=payment_resp,
        ticket=TicketResponse.model_validate(b.ticket) if b.ticket else None,
    )


# ─── Booking Management ───────────────────────────────────────────────────────

@router.get("/bookings/", response_model=List[BookingResponse])
def list_all_bookings(
    status_filter: Optional[str] = None,
    search: Optional[str] = None,
    payment_status: Optional[str] = None,
    movie_id: Optional[int] = None,
    booking_date: Optional[str] = None,
    show_date: Optional[str] = None,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    """
    Admin endpoint: view all bookings with full customer information.
    Supports search (booking ref, customer name, email, phone) and filtering by status, payment status, movie, booking date, show date.
    Sorted by newest booking creation first by default.
    """
    query = (
        db.query(Booking)
        .options(
            joinedload(Booking.user),
            joinedload(Booking.show).joinedload(Show.movie),
            joinedload(Booking.show).joinedload(Show.screen),
            joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
            joinedload(Booking.booking_snacks).joinedload(BookingSnack.snack),
            joinedload(Booking.payment).joinedload(Payment.verified_by),
            joinedload(Booking.ticket),
        )
    )

    # Search filter
    if search and search.strip():
        term = f"%{search.strip()}%"
        query = query.outerjoin(Booking.user).filter(
            (Booking.booking_reference.ilike(term)) |
            (User.name.ilike(term)) |
            (User.email.ilike(term)) |
            (User.phone.ilike(term))
        )

    # Booking status filter
    if status_filter and status_filter.upper() != "ALL":
        s_upper = status_filter.upper()
        if s_upper == "PENDING":
            query = query.join(Payment, Booking.id == Payment.booking_id).filter(
                Payment.status.in_([PaymentStatus.PENDING, PaymentStatus.SUBMITTED])
            )
        elif s_upper in ["PAID", "CONFIRMED"]:
            query = query.filter(Booking.status.in_([BookingStatus.CONFIRMED, BookingStatus.COMPLETED]))
        elif s_upper in ["CANCELLED", "FAILED"]:
            query = query.filter(Booking.status == BookingStatus.CANCELLED)

    # Payment status filter
    if payment_status and payment_status.upper() != "ALL":
        p_upper = payment_status.upper()
        query = query.join(Payment, Booking.id == Payment.booking_id)
        if p_upper == "PENDING":
            query = query.filter(Payment.status == PaymentStatus.PENDING)
        elif p_upper == "SUBMITTED":
            query = query.filter(Payment.status == PaymentStatus.SUBMITTED)
        elif p_upper in ["PAID", "COMPLETED"]:
            query = query.filter(Payment.status.in_([PaymentStatus.PAID, PaymentStatus.COMPLETED]))
        elif p_upper == "FAILED":
            query = query.filter(Payment.status == PaymentStatus.FAILED)
        elif p_upper == "REFUNDED":
            query = query.filter(Payment.status == PaymentStatus.REFUNDED)

    # Movie filter
    if movie_id:
        query = query.join(Booking.show).filter(Show.movie_id == movie_id)

    # Booking date filter (YYYY-MM-DD)
    if booking_date and booking_date.strip():
        try:
            b_date = datetime.strptime(booking_date.strip(), "%Y-%m-%d").date()
            next_date = b_date + timedelta(days=1)
            query = query.filter(Booking.created_at >= datetime.combine(b_date, datetime.min.time()))
            query = query.filter(Booking.created_at < datetime.combine(next_date, datetime.min.time()))
        except ValueError:
            pass

    # Show date filter (YYYY-MM-DD)
    if show_date and show_date.strip():
        try:
            s_date = datetime.strptime(show_date.strip(), "%Y-%m-%d").date()
            next_s_date = s_date + timedelta(days=1)
            query = query.join(Booking.show).filter(Show.start_time >= datetime.combine(s_date, datetime.min.time()))
            query = query.filter(Show.start_time < datetime.combine(next_s_date, datetime.min.time()))
        except ValueError:
            pass

    bookings = query.order_by(Booking.created_at.desc()).all()
    return [_build_booking_response(b) for b in bookings]


# ─── Payment Verification & Ticket Scanner ─────────────────────────────────────

class PaymentVerificationRequest(BaseModel):
    action: str  # "APPROVE" or "REJECT"
    remarks: Optional[str] = None


class TicketVerificationResponse(BaseModel):
    is_valid: bool
    ticket_code: str
    booking_reference: str
    user_name: str
    customer_email: Optional[str] = None
    customer_phone: Optional[str] = None
    customer_id: Optional[int] = None
    movie_title: str
    movie_poster_url: Optional[str] = None
    language: Optional[str] = None
    format: Optional[str] = None
    screen_name: str
    show_date: Optional[str] = None
    show_time: str
    show_end_time: Optional[str] = None
    seats: List[str]
    ticket_count: int
    booking_status: str
    payment_status: str
    validity_status: str  # "VALID", "UNPAID", "ALREADY_USED", "CANCELLED", "EXPIRED", "INVALID"
    is_used: bool = False
    used_at: Optional[datetime] = None
    issued_at: datetime


@router.get("/payments/", response_model=List[BookingResponse])
def list_pending_payments(
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    """
    Admin endpoint: list all bookings with payment records for verification.
    """
    bookings = (
        db.query(Booking)
        .options(
            joinedload(Booking.user),
            joinedload(Booking.show).joinedload(Show.movie),
            joinedload(Booking.show).joinedload(Show.screen),
            joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
            joinedload(Booking.booking_snacks).joinedload(BookingSnack.snack),
            joinedload(Booking.payment).joinedload(Payment.verified_by),
            joinedload(Booking.ticket),
        )
        .join(Payment, Booking.id == Payment.booking_id)
        .order_by(Payment.created_at.desc())
        .all()
    )

    return [_build_booking_response(b) for b in bookings]


@router.post("/payments/{payment_id}/verify", response_model=BookingResponse)
def verify_payment(
    payment_id: int,
    request: PaymentVerificationRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    payment = db.query(Payment).filter(Payment.id == payment_id).first()
    if not payment:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Payment record not found")

    booking = db.query(Booking).filter(Booking.id == payment.booking_id).first()
    if not booking:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Booking record not found")

    action_upper = request.action.upper()
    payment.verified_by_id = admin_user.id
    payment.verified_at = datetime.utcnow()
    if request.remarks:
        payment.remarks = request.remarks

    if action_upper == "APPROVE":
        payment.status = PaymentStatus.PAID
        payment.paid_at = datetime.utcnow()
        booking.status = BookingStatus.CONFIRMED
    elif action_upper == "REJECT":
        payment.status = PaymentStatus.FAILED
        booking.status = BookingStatus.CANCELLED
        booking.cancelled_at = datetime.utcnow()

        # Release booked seats back to AVAILABLE
        seat_ids = [bs.seat_id for bs in booking.booking_seats]
        if seat_ids:
            db.query(ShowSeat).filter(
                ShowSeat.show_id == booking.show_id,
                ShowSeat.seat_id.in_(seat_ids)
            ).update({ShowSeat.status: ShowSeatStatus.AVAILABLE}, synchronize_session=False)
    else:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Action must be APPROVE or REJECT")

    db.commit()
    db.refresh(booking)

    # Reload booking with all relationships
    booking = (
        db.query(Booking)
        .options(
            joinedload(Booking.user),
            joinedload(Booking.show).joinedload(Show.movie),
            joinedload(Booking.show).joinedload(Show.screen),
            joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
            joinedload(Booking.booking_snacks).joinedload(BookingSnack.snack),
            joinedload(Booking.payment).joinedload(Payment.verified_by),
            joinedload(Booking.ticket),
        )
        .filter(Booking.id == booking.id)
        .first()
    )

    return _build_booking_response(booking)


@router.get("/tickets/verify/{code}", response_model=TicketVerificationResponse)
def verify_ticket(
    code: str,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    """
    Verify ticket code or booking reference.
    Supports scanned raw strings and embedded QR JSON payloads.
    Distinguishes VALID, UNPAID, ALREADY_USED, CANCELLED, and EXPIRED tickets.
    """
    clean_code = code.strip()
    if clean_code.startswith("{") and clean_code.endswith("}"):
        try:
            import json
            payload = json.loads(clean_code)
            clean_code = payload.get("ticket_code") or payload.get("booking_ref") or clean_code
        except Exception:
            pass

    ticket = db.query(Ticket).filter(Ticket.ticket_code == clean_code).first()
    booking = None
    if ticket:
        booking = (
            db.query(Booking)
            .options(
                joinedload(Booking.user),
                joinedload(Booking.show).joinedload(Show.movie),
                joinedload(Booking.show).joinedload(Show.screen),
                joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
                joinedload(Booking.payment),
                joinedload(Booking.ticket),
            )
            .filter(Booking.id == ticket.booking_id)
            .first()
        )
    else:
        booking = (
            db.query(Booking)
            .options(
                joinedload(Booking.user),
                joinedload(Booking.show).joinedload(Show.movie),
                joinedload(Booking.show).joinedload(Show.screen),
                joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
                joinedload(Booking.payment),
                joinedload(Booking.ticket),
            )
            .filter(Booking.booking_reference == clean_code)
            .first()
        )
        if booking:
            ticket = booking.ticket

    if not booking or not ticket:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Ticket or Booking reference code not found"
        )

    payment = booking.payment
    is_paid = payment is not None and (payment.status in [PaymentStatus.PAID, PaymentStatus.COMPLETED])
    is_cancelled = booking.status == BookingStatus.CANCELLED
    is_used = bool(ticket.is_used)

    # Expiry check: if show end_time has passed by more than 1 hour
    now_utc = datetime.utcnow()
    is_expired = False
    if booking.show and booking.show.end_time:
        if booking.show.end_time < now_utc - timedelta(hours=1):
            is_expired = True

    if is_cancelled:
        validity_status = "CANCELLED"
        is_valid = False
    elif not is_paid:
        validity_status = "UNPAID"
        is_valid = False
    elif is_used:
        validity_status = "ALREADY_USED"
        is_valid = False
    elif is_expired:
        validity_status = "EXPIRED"
        is_valid = False
    else:
        validity_status = "VALID"
        is_valid = True

    # Format show schedule in IST (UTC + 5:30)
    show_date_str = None
    show_time_str = ""
    show_end_time_str = None
    if booking.show and booking.show.start_time:
        ist_start = booking.show.start_time + timedelta(hours=5, minutes=30)
        show_date_str = ist_start.strftime("%d %b %Y")
        show_time_str = ist_start.strftime("%I:%M %p")
        if booking.show.end_time:
            ist_end = booking.show.end_time + timedelta(hours=5, minutes=30)
            show_end_time_str = ist_end.strftime("%I:%M %p")

    user = booking.user
    seats_list = [bs.seat.seat_code for bs in booking.booking_seats if bs.seat]

    movie = booking.show.movie if booking.show else None
    screen = booking.show.screen if booking.show else None
    format_spec = None
    if movie:
        format_spec = f"{movie.display_technology} • {movie.audio_technology}"

    return TicketVerificationResponse(
        is_valid=is_valid,
        ticket_code=ticket.ticket_code,
        booking_reference=booking.booking_reference,
        user_name=user.name if user else "Customer",
        customer_email=user.email if user else None,
        customer_phone=user.phone if user else None,
        customer_id=user.id if user else None,
        movie_title=movie.title if movie else "Movie",
        movie_poster_url=movie.poster_url if movie else None,
        language=movie.language if movie else None,
        format=format_spec,
        screen_name=screen.name if screen else "Screen",
        show_date=show_date_str,
        show_time=show_time_str or (str(booking.show.start_time) if booking.show else ""),
        show_end_time=show_end_time_str,
        seats=seats_list,
        ticket_count=len(seats_list) or booking.total_seats,
        booking_status=booking.status.value,
        payment_status=payment.status.value if payment else "UNPAID",
        validity_status=validity_status,
        is_used=is_used,
        used_at=ticket.used_at,
        issued_at=ticket.issued_at,
    )


@router.post("/tickets/check-in/{code}", response_model=TicketVerificationResponse)
def check_in_ticket(
    code: str,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    """
    Atomically marks a valid ticket as checked in / used at cinema entrance.
    Prevents duplicate or concurrent admissions on the same ticket code.
    """
    clean_code = code.strip()
    if clean_code.startswith("{") and clean_code.endswith("}"):
        try:
            import json
            payload = json.loads(clean_code)
            clean_code = payload.get("ticket_code") or payload.get("booking_ref") or clean_code
        except Exception:
            pass

    ticket = db.query(Ticket).filter(Ticket.ticket_code == clean_code).first()
    if not ticket:
        booking = db.query(Booking).filter(Booking.booking_reference == clean_code).first()
        if booking:
            ticket = booking.ticket

    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found")

    booking = (
        db.query(Booking)
        .options(
            joinedload(Booking.user),
            joinedload(Booking.payment),
        )
        .filter(Booking.id == ticket.booking_id)
        .first()
    )

    if not booking:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Booking not found")

    is_paid = booking.payment is not None and (booking.payment.status in [PaymentStatus.PAID, PaymentStatus.COMPLETED])
    if booking.status == BookingStatus.CANCELLED:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Cannot check in a cancelled booking")
    if not is_paid:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Cannot check in an unpaid ticket. Payment must be verified first.")

    # Atomic update on ticket.is_used to prevent simultaneous scans from admitting twice
    updated_rows = (
        db.query(Ticket)
        .filter(Ticket.id == ticket.id, Ticket.is_used == False)
        .update({Ticket.is_used: True, Ticket.used_at: datetime.utcnow()}, synchronize_session=False)
    )
    if updated_rows == 0:
        db.refresh(ticket)
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Ticket has already been used / checked in at {ticket.used_at}"
        )

    db.commit()
    db.refresh(ticket)

    return verify_ticket(code=ticket.ticket_code, db=db, admin_user=admin_user)


# ─── Screen Management ────────────────────────────────────────────────────────

@router.get("/screens/", response_model=List[ScreenResponse])
def list_screens(
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    """List all cinema screens with specifications."""
    screens = db.query(Screen).all()
    return [ScreenResponse.model_validate(s) for s in screens]


@router.put("/screens/{screen_id}", response_model=ScreenResponse)
def update_screen(
    screen_id: int,
    request: ScreenUpdateRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    """Update screen name, display specification, audio specification, and Dolby Atmos setting."""
    screen = db.query(Screen).filter(Screen.id == screen_id).first()
    if not screen:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Screen not found")

    if request.name is not None:
        screen.name = request.name
    if request.display_spec is not None:
        screen.display_spec = request.display_spec
    if request.audio_spec is not None:
        screen.audio_spec = request.audio_spec
    if request.has_dolby_atmos is not None:
        screen.has_dolby_atmos = request.has_dolby_atmos

    if request.seat_base_price is not None and request.seat_base_price > 0:
        db.query(Seat).filter(Seat.screen_id == screen_id).update(
            {Seat.base_price: request.seat_base_price}, synchronize_session=False
        )

    db.commit()
    db.refresh(screen)
    return ScreenResponse.model_validate(screen)


# ─── Payment Config Management ───────────────────────────────────────────────

@router.get("/payments/config", response_model=PaymentConfigResponse)
def get_admin_payment_config(
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    upi_id_item = db.query(SystemSetting).filter(SystemSetting.key == "upi_id").first()
    payee_name_item = db.query(SystemSetting).filter(SystemSetting.key == "payee_name").first()
    return PaymentConfigResponse(
        upi_id=upi_id_item.value if upi_id_item else "rnpscinema@upi",
        payee_name=payee_name_item.value if payee_name_item else "RNPS Home Cinema",
    )


@router.put("/payments/config", response_model=PaymentConfigResponse)
def update_payment_config(
    request: PaymentConfigRequest,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    upi_item = db.query(SystemSetting).filter(SystemSetting.key == "upi_id").first()
    if not upi_item:
        upi_item = SystemSetting(key="upi_id", value=request.upi_id)
        db.add(upi_item)
    else:
        upi_item.value = request.upi_id

    payee_item = db.query(SystemSetting).filter(SystemSetting.key == "payee_name").first()
    if not payee_item:
        payee_item = SystemSetting(key="payee_name", value=request.payee_name)
        db.add(payee_item)
    else:
        payee_item.value = request.payee_name

    db.commit()
    return PaymentConfigResponse(upi_id=request.upi_id, payee_name=request.payee_name)
