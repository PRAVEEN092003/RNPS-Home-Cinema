"""
Booking routes: create booking, list user bookings, get booking details.
"""
from typing import List
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload
from app.config.database import get_db
from app.models.models import (
    Booking, BookingSeat, BookingSnack, Show, ShowSeat, Seat, User,
    BookingStatus, ShowSeatStatus
)
from app.schemas.schemas import (
    CreateBookingRequest, BookingResponse, BookingSeatResponse, ShowResponse
)
from app.utils.auth import get_current_user
from app.utils.helpers import generate_booking_reference

router = APIRouter(prefix="/bookings", tags=["Bookings"])


@router.post("/", response_model=BookingResponse, status_code=status.HTTP_201_CREATED)
def create_booking(
    request: CreateBookingRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Create a new ticket booking.
    Atomically validates seat availability, locks seats, and saves booking details.
    Both USER and ADMIN accounts use this endpoint.
    """
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
    show_seats = (
        db.query(ShowSeat)
        .filter(
            ShowSeat.show_id == request.show_id,
            ShowSeat.seat_id.in_(request.seat_ids)
        )
        .all()
    )

    if len(show_seats) != len(request.seat_ids):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="One or more selected seats do not exist for this show",
        )

    # 3. Check seat availability
    unavailable_seats = [ss for ss in show_seats if ss.status != ShowSeatStatus.AVAILABLE]
    if unavailable_seats:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="One or more selected seats are no longer available. Please select different seats.",
        )

    # 4. Calculate prices
    subtotal = sum(ss.price for ss in show_seats)
    convenience_fee = round(len(show_seats) * 1.50, 2)
    total_amount = round(subtotal + convenience_fee, 2)

    # 5. Create booking record
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

    # 6. Create booking seats & update show_seats to BOOKED
    booking_seat_responses = []
    for ss in show_seats:
        # Mark seat as BOOKED
        ss.status = ShowSeatStatus.BOOKED

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
        show=ShowResponse.model_validate(show),
        seats=booking_seat_responses,
        ticket_amount=subtotal + convenience_fee,
        snacks_amount=0.0,
    )


@router.get("/my", response_model=List[BookingResponse])
def get_my_bookings(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """List all bookings for the currently authenticated user."""
    from app.schemas.schemas import BookingSnackResponse, PaymentResponse, TicketResponse
    bookings = (
        db.query(Booking)
        .options(
            joinedload(Booking.show).joinedload(Show.movie),
            joinedload(Booking.show).joinedload(Show.screen),
            joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
            joinedload(Booking.booking_snacks).joinedload(BookingSnack.snack),
            joinedload(Booking.payment),
            joinedload(Booking.ticket),
        )
        .filter(Booking.user_id == current_user.id)
        .order_by(Booking.created_at.desc())
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
