"""
Booking routes: create booking, list user bookings, get booking details.
"""
from datetime import datetime, timedelta
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload
from app.config.database import get_db
from app.models.models import (
    Booking, BookingSeat, BookingSnack, Show, ShowSeat, Seat, User,
    BookingStatus, ShowSeatStatus, PaymentStatus
)
from app.schemas.schemas import (
    CreateBookingRequest, BookingResponse, BookingSeatResponse, BookingSnackResponse,
    ShowResponse, PaymentResponse, TicketResponse, UserResponse
)
from app.utils.auth import get_current_user
from app.utils.helpers import generate_booking_reference

router = APIRouter(prefix="/bookings", tags=["Bookings"])


def release_expired_holds(db: Session, show_id: Optional[int] = None, timeout_minutes: int = 10):
    """
    Automatically release seats held by pending bookings that have expired without payment submission.
    Never releases seats associated with submitted or completed payments.
    """
    cutoff = datetime.utcnow() - timedelta(minutes=timeout_minutes)
    query = (
        db.query(Booking)
        .options(joinedload(Booking.booking_seats), joinedload(Booking.payment))
        .filter(
            Booking.status == BookingStatus.PENDING,
            Booking.created_at < cutoff,
        )
    )
    if show_id:
        query = query.filter(Booking.show_id == show_id)
    expired_bookings = query.all()
    for b in expired_bookings:
        # Never release seats if payment was submitted or paid
        if b.payment and b.payment.status in [PaymentStatus.SUBMITTED, PaymentStatus.PAID, PaymentStatus.COMPLETED]:
            continue
        seat_ids = [bs.seat_id for bs in b.booking_seats]
        if seat_ids:
            db.query(ShowSeat).filter(
                ShowSeat.show_id == b.show_id,
                ShowSeat.seat_id.in_(seat_ids),
                ShowSeat.status.in_([ShowSeatStatus.HELD, ShowSeatStatus.BOOKED]),
            ).update({ShowSeat.status: ShowSeatStatus.AVAILABLE}, synchronize_session=False)
        b.status = BookingStatus.CANCELLED

    try:
        # Also release any orphan HELD seats that are not part of an active unexpired pending booking
        active_pending_subquery = (
            db.query(BookingSeat.seat_id)
            .join(Booking, BookingSeat.booking_id == Booking.id)
            .filter(
                Booking.status == BookingStatus.PENDING,
                Booking.created_at >= cutoff,
            )
        )
        if show_id:
            active_pending_subquery = active_pending_subquery.filter(Booking.show_id == show_id)

        orphan_query = db.query(ShowSeat).filter(
            ShowSeat.status == ShowSeatStatus.HELD,
            ~ShowSeat.seat_id.in_(active_pending_subquery)
        )
        if show_id:
            orphan_query = orphan_query.filter(ShowSeat.show_id == show_id)
        orphan_query.update({ShowSeat.status: ShowSeatStatus.AVAILABLE}, synchronize_session=False)

        db.commit()
    except Exception:
        db.rollback()


@router.post("/", response_model=BookingResponse, status_code=status.HTTP_201_CREATED)
def create_booking(
    request: CreateBookingRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Create a new ticket booking.
    Atomically validates seat availability, places a temporary hold on seats, and saves booking details.
    Prevents concurrent double-booking using atomic status validation and updates.
    """
    # 0. Clean up any expired holds first so available seats can be booked
    release_expired_holds(db, show_id=request.show_id)

    # 1. Validate show exists
    show = (
        db.query(Show)
        .options(joinedload(Show.movie), joinedload(Show.screen))
        .filter(Show.id == request.show_id, Show.is_active == True)
        .first()
    )
    if not show:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Show not found",
        )

    # 2. Fetch requested show_seats with row-level lock check
    query = (
        db.query(ShowSeat)
        .filter(
            ShowSeat.show_id == request.show_id,
            ShowSeat.seat_id.in_(request.seat_ids)
        )
    )
    if db.bind and db.bind.dialect.name == "postgresql":
        query = query.with_for_update()

    show_seats = query.all()

    if len(show_seats) != len(request.seat_ids):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="One or more selected seats do not exist for this show",
        )

    # 3. Check seat availability: Must be AVAILABLE
    unavailable_seats = [ss for ss in show_seats if ss.status != ShowSeatStatus.AVAILABLE]
    if unavailable_seats:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="One or more selected seats are no longer available. Please select different seats.",
        )

    # Atomically hold the seats to prevent concurrent double-booking
    updated_count = (
        db.query(ShowSeat)
        .filter(
            ShowSeat.show_id == request.show_id,
            ShowSeat.seat_id.in_(request.seat_ids),
            ShowSeat.status == ShowSeatStatus.AVAILABLE
        )
        .update({ShowSeat.status: ShowSeatStatus.HELD}, synchronize_session="fetch")
    )
    if updated_count != len(request.seat_ids):
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="One or more selected seats are no longer available. Please select different seats.",
        )

    # 4. Calculate prices with trusted server-side pricing
    for ss in show_seats:
        if show.base_price is not None and show.base_price > 0:
            ss.price = show.base_price
        elif ss.price is None or ss.price <= 0:
            seat_obj = db.query(Seat).filter(Seat.id == ss.seat_id).first()
            if seat_obj and seat_obj.base_price and seat_obj.base_price > 0:
                ss.price = seat_obj.base_price
            else:
                db.rollback()
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail="Ticket price for one or more selected seats is invalid or not configured."
                )

    subtotal = round(sum(ss.price for ss in show_seats), 2)
    convenience_fee = 0.0
    total_amount = subtotal

    # 5. Create booking record with PENDING status
    booking_ref = generate_booking_reference()
    booking = Booking(
        user_id=current_user.id,
        show_id=request.show_id,
        booking_reference=booking_ref,
        status=BookingStatus.PENDING,
        total_amount=total_amount,
        convenience_fee=convenience_fee,
        total_seats=len(show_seats),
        created_at=datetime.utcnow(),
    )
    db.add(booking)
    db.flush()  # get booking.id

    # 6. Create booking seats
    booking_seat_responses = []
    for ss in show_seats:
        b_seat = BookingSeat(
            booking_id=booking.id,
            seat_id=ss.seat_id,
            price=ss.price,
        )
        db.add(b_seat)

        # Get seat metadata
        seat_info = db.query(Seat).filter(Seat.id == ss.seat_id).first()
        if seat_info:
            booking_seat_responses.append(
                BookingSeatResponse(
                    id=ss.id,
                    seat_id=seat_info.id,
                    seat_code=seat_info.seat_code,
                    row_label=seat_info.row_label,
                    seat_number=seat_info.seat_number,
                    price=ss.price,
                )
            )

    db.commit()
    db.refresh(booking)

    return BookingResponse(
        id=booking.id,
        user_id=booking.user_id,
        show_id=booking.show_id,
        booking_reference=booking.booking_reference,
        status=booking.status,
        total_amount=booking.total_amount,
        convenience_fee=booking.convenience_fee,
        total_seats=booking.total_seats,
        created_at=booking.created_at,
        user=UserResponse.model_validate(current_user),
        show=ShowResponse.model_validate(show),
        seats=booking_seat_responses,
        ticket_amount=subtotal,
        snacks_amount=0.0,
    )


@router.get("/my", response_model=List[BookingResponse])
def get_my_bookings(
    status: Optional[str] = None,
    status_filter: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    List all bookings for the currently authenticated user.
    Optionally filter by booking status (e.g., PENDING, CONFIRMED, CANCELLED).
    """
    from app.schemas.schemas import BookingSnackResponse, PaymentResponse, TicketResponse
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
        .filter(Booking.user_id == current_user.id)
    )

    filter_val = status or status_filter
    if filter_val:
        val = filter_val.upper().strip()
        if val in ["PENDING"]:
            query = query.filter(Booking.status == BookingStatus.PENDING)
        elif val in ["CONFIRMED", "BOOKED", "PAID"]:
            query = query.filter(Booking.status.in_([BookingStatus.CONFIRMED, BookingStatus.COMPLETED]))
        elif val in ["CANCELLED", "FAILED", "REJECTED"]:
            query = query.filter(Booking.status == BookingStatus.CANCELLED)
        else:
            query = query.filter(Booking.status == val)

    bookings = query.order_by(Booking.created_at.desc()).all()

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
                cancelled_at=b.cancelled_at or (b.updated_at if b.status == BookingStatus.CANCELLED else None),
                user=UserResponse.model_validate(b.user) if b.user else None,
                show=ShowResponse.model_validate(b.show) if b.show else None,
                seats=seats_resp,
                snacks=snacks_resp,
                ticket_amount=ticket_subtotal,
                snacks_amount=snacks_subtotal,
                payment=PaymentResponse.model_validate(b.payment) if b.payment else None,
                ticket=TicketResponse.model_validate(b.ticket) if b.ticket else None,
            )
        )
    return result


@router.get("/{booking_id}", response_model=BookingResponse)
def get_booking(
    booking_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get single booking details for the currently authenticated user."""
    booking = (
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
        .filter(Booking.id == booking_id, Booking.user_id == current_user.id)
        .first()
    )
    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found",
        )

    seats_resp = [
        BookingSeatResponse(
            id=bs.id,
            seat_id=bs.seat_id,
            seat_code=bs.seat.seat_code if bs.seat else "",
            row_label=bs.seat.row_label if bs.seat else "",
            seat_number=bs.seat.seat_number if bs.seat else 0,
            price=bs.price,
        )
        for bs in booking.booking_seats
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
        for bsn in booking.booking_snacks
    ]
    ticket_subtotal = sum(bs.price for bs in booking.booking_seats)
    snacks_subtotal = sum(bsn.total_price for bsn in booking.booking_snacks)

    return BookingResponse(
        id=booking.id,
        user_id=booking.user_id,
        show_id=booking.show_id,
        booking_reference=booking.booking_reference,
        status=booking.status,
        total_amount=booking.total_amount,
        convenience_fee=booking.convenience_fee,
        total_seats=booking.total_seats,
        created_at=booking.created_at,
        cancelled_at=booking.cancelled_at or (booking.updated_at if booking.status == BookingStatus.CANCELLED else None),
        user=UserResponse.model_validate(booking.user) if booking.user else None,
        show=ShowResponse.model_validate(booking.show) if booking.show else None,
        seats=seats_resp,
        snacks=snacks_resp,
        ticket_amount=ticket_subtotal,
        snacks_amount=snacks_subtotal,
        payment=PaymentResponse.model_validate(booking.payment) if booking.payment else None,
        ticket=TicketResponse.model_validate(booking.ticket) if booking.ticket else None,
    )


@router.post("/{booking_id}/cancel", response_model=dict)
def cancel_booking(
    booking_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Cancel a pending booking and release its held seats.
    Only the owning user can cancel.
    Never cancels a booking with submitted or completed payment.
    """
    booking = (
        db.query(Booking)
        .options(joinedload(Booking.booking_seats), joinedload(Booking.payment))
        .filter(Booking.id == booking_id, Booking.user_id == current_user.id)
        .first()
    )
    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found",
        )

    # Protect against cancelling already submitted / completed payments or confirmed bookings
    if booking.status in [BookingStatus.CONFIRMED, BookingStatus.COMPLETED] or (
        booking.payment and booking.payment.status in [PaymentStatus.SUBMITTED, PaymentStatus.PAID, PaymentStatus.COMPLETED]
    ):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot cancel booking with submitted or confirmed payment.",
        )

    # If already cancelled, ensure any held seats are released idempotently
    if booking.status == BookingStatus.CANCELLED:
        seat_ids = [bs.seat_id for bs in booking.booking_seats]
        if seat_ids:
            try:
                db.query(ShowSeat).filter(
                    ShowSeat.show_id == booking.show_id,
                    ShowSeat.seat_id.in_(seat_ids),
                    ShowSeat.status.in_([ShowSeatStatus.HELD, ShowSeatStatus.BOOKED]),
                ).update({ShowSeat.status: ShowSeatStatus.AVAILABLE}, synchronize_session=False)
                db.commit()
            except Exception:
                db.rollback()
        return {"status": "ok", "message": "Booking is already cancelled and seats released"}

    # Release held / booked seats belonging to this unsubmitted pending booking
    seat_ids = [bs.seat_id for bs in booking.booking_seats]
    try:
        if seat_ids:
            db.query(ShowSeat).filter(
                ShowSeat.show_id == booking.show_id,
                ShowSeat.seat_id.in_(seat_ids),
                ShowSeat.status.in_([ShowSeatStatus.HELD, ShowSeatStatus.BOOKED]),
            ).update({ShowSeat.status: ShowSeatStatus.AVAILABLE}, synchronize_session=False)

        booking.status = BookingStatus.CANCELLED
        booking.cancelled_at = datetime.utcnow()
        db.commit()
    except Exception:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to cancel booking and release seats.",
        )

    return {"status": "ok", "message": "Booking cancelled and seats released successfully"}
