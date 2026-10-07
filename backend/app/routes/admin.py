"""
Admin management routes: Movie CRUD, Show CRUD, Snack CRUD, Payment verification & Ticket scanner.
"""
from typing import List, Optional
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel
from sqlalchemy.orm import Session, joinedload
from app.config.database import get_db
from app.models.models import (
    Payment, Booking, BookingSeat, BookingSnack, Ticket, ShowSeat, Seat, Movie, Show, Screen, Snack, User, SystemSetting,
    PaymentStatus, BookingStatus, ShowSeatStatus
)
from app.schemas.schemas import (
    BookingResponse, BookingSeatResponse, BookingSnackResponse, ShowResponse, PaymentResponse, TicketResponse,
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

    show = Show(
        movie_id=request.movie_id,
        screen_id=request.screen_id,
        start_time=request.start_time,
        end_time=request.end_time,
        base_price=request.base_price,
        is_active=True,
    )
    db.add(show)
    db.flush()

    # Automatically generate ShowSeat records for all seats in the screen
    seats = db.query(Seat).filter(Seat.screen_id == screen.id, Seat.is_active == True).all()
    show_seats = [
        ShowSeat(
            show_id=show.id,
            seat_id=seat.id,
            status=ShowSeatStatus.AVAILABLE,
            price=seat.base_price,
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

    for field, val in request.model_dump(exclude_unset=True).items():
        setattr(show, field, val)

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


# ─── Booking Management ───────────────────────────────────────────────────────

@router.get("/bookings/", response_model=List[BookingResponse])
def list_all_bookings(
    status_filter: Optional[str] = None,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    """
    Admin endpoint: view all bookings (optionally filter by status: PENDING, PAID, CANCELLED).
    """
    query = (
        db.query(Booking)
        .options(
            joinedload(Booking.user),
            joinedload(Booking.show).joinedload(Show.movie),
            joinedload(Booking.show).joinedload(Show.screen),
            joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
            joinedload(Booking.booking_snacks).joinedload(BookingSnack.snack),
            joinedload(Booking.payment),
            joinedload(Booking.ticket),
        )
        .order_by(Booking.created_at.desc())
    )

    if status_filter:
        s_upper = status_filter.upper()
        if s_upper == "PENDING":
            query = query.join(Payment, Booking.id == Payment.booking_id).filter(Payment.status == PaymentStatus.PENDING)
        elif s_upper in ["PAID", "CONFIRMED"]:
            query = query.filter(Booking.status == BookingStatus.CONFIRMED)
        elif s_upper in ["CANCELLED", "FAILED"]:
            query = query.filter(Booking.status == BookingStatus.CANCELLED)

    bookings = query.all()

    result = []
    for b in bookings:
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

        result.append(
            BookingResponse(
                id=b.id,
                user_id=b.user_id,
                show_id=b.show_id,
                booking_reference=b.booking_reference,
                status=b.status,
                total_amount=b.total_amount,
                convenience_fee=b.convenience_fee,
                total_seats=b.total_seats,
                created_at=b.created_at,
                show=ShowResponse.model_validate(b.show) if b.show else None,
                seats=seats_resp,
                snacks=snacks_resp,
                ticket_amount=ticket_subtotal + b.convenience_fee,
                snacks_amount=snacks_subtotal,
                payment=PaymentResponse.model_validate(b.payment) if b.payment else None,
                ticket=TicketResponse.model_validate(b.ticket) if b.ticket else None,
            )
        )
    return result


# ─── Payment Verification & Ticket Scanner ─────────────────────────────────────

class PaymentVerificationRequest(BaseModel):
    action: str  # "APPROVE" or "REJECT"


class TicketVerificationResponse(BaseModel):
    is_valid: bool
    ticket_code: str
    booking_reference: str
    user_name: str
    movie_title: str
    screen_name: str
    show_time: str
    seats: List[str]
    booking_status: str
    payment_status: str
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
            joinedload(Booking.payment),
            joinedload(Booking.ticket),
        )
        .join(Payment, Booking.id == Payment.booking_id)
        .order_by(Payment.created_at.desc())
        .all()
    )

    result = []
    for b in bookings:
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

        result.append(
            BookingResponse(
                id=b.id,
                user_id=b.user_id,
                show_id=b.show_id,
                booking_reference=b.booking_reference,
                status=b.status,
                total_amount=b.total_amount,
                convenience_fee=b.convenience_fee,
                total_seats=b.total_seats,
                created_at=b.created_at,
                show=ShowResponse.model_validate(b.show) if b.show else None,
                seats=seats_resp,
                snacks=snacks_resp,
                ticket_amount=ticket_subtotal + b.convenience_fee,
                snacks_amount=snacks_subtotal,
                payment=PaymentResponse.model_validate(b.payment) if b.payment else None,
                ticket=TicketResponse.model_validate(b.ticket) if b.ticket else None,
            )
        )
    return result


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
    if action_upper == "APPROVE":
        payment.status = PaymentStatus.PAID
        payment.paid_at = datetime.utcnow()
        booking.status = BookingStatus.CONFIRMED
    elif action_upper == "REJECT":
        payment.status = PaymentStatus.FAILED
        booking.status = BookingStatus.CANCELLED

        # Release booked seats back to AVAILABLE
        seat_ids = [bs.seat_id for bs in booking.booking_seats]
        db.query(ShowSeat).filter(
            ShowSeat.show_id == booking.show_id,
            ShowSeat.seat_id.in_(seat_ids)
        ).update({ShowSeat.status: ShowSeatStatus.AVAILABLE}, synchronize_session=False)
    else:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Action must be APPROVE or REJECT")

    db.commit()
    db.refresh(booking)

    return list_pending_payments(db, admin_user)[0]


@router.get("/tickets/verify/{code}", response_model=TicketVerificationResponse)
def verify_ticket(
    code: str,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_admin),
):
    ticket = db.query(Ticket).filter(Ticket.ticket_code == code.strip()).first()
    booking = None
    if ticket:
        booking = db.query(Booking).filter(Booking.id == ticket.booking_id).first()
    else:
        booking = db.query(Booking).filter(Booking.booking_reference == code.strip()).first()
        if booking:
            ticket = db.query(Ticket).filter(Ticket.booking_id == booking.id).first()

    if not booking or not ticket:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Ticket or Booking reference code not found"
        )

    payment = db.query(Payment).filter(Payment.booking_id == booking.id).first()
    is_paid = payment is not None and (payment.status in [PaymentStatus.PAID, PaymentStatus.COMPLETED])
    is_booking_active = booking.status != BookingStatus.CANCELLED

    is_valid = is_paid and is_booking_active

    user = db.query(User).filter(User.id == booking.user_id).first()
    seats_list = [bs.seat.seat_code for bs in booking.booking_seats if bs.seat]

    return TicketVerificationResponse(
        is_valid=is_valid,
        ticket_code=ticket.ticket_code,
        booking_reference=booking.booking_reference,
        user_name=user.name if user else "Customer",
        movie_title=booking.show.movie.title if booking.show and booking.show.movie else "Movie",
        screen_name=booking.show.screen.name if booking.show and booking.show.screen else "Screen",
        show_time=str(booking.show.start_time) if booking.show else "",
        seats=seats_list,
        booking_status=booking.status.value,
        payment_status=payment.status.value if payment else "UNPAID",
        issued_at=ticket.issued_at,
    )


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
