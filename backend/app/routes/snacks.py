"""
Snacks endpoints: list available snacks and attach snacks to a booking.
"""
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload
from app.config.database import get_db
from app.models.models import Snack, Booking, BookingSnack, Show, BookingSeat, User
from app.schemas.schemas import (
    SnackResponse, AddSnacksRequest, BookingResponse,
    BookingSeatResponse, BookingSnackResponse, ShowResponse
)
from app.utils.auth import get_current_user

router = APIRouter(prefix="/snacks", tags=["Snacks"])


@router.get("/", response_model=List[SnackResponse])
def get_snacks(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """List all available snacks."""
    snacks = db.query(Snack).filter(Snack.is_available == True).all()
    return [SnackResponse.model_validate(s) for s in snacks]


@router.post("/booking/{booking_id}", response_model=BookingResponse)
def add_snacks_to_booking(
    booking_id: int,
    request: AddSnacksRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Attach or update snacks for an existing booking.
    Recalculates total payable amount in SQLite database.
    """
    booking = (
        db.query(Booking)
        .options(
            joinedload(Booking.show).joinedload(Show.movie),
            joinedload(Booking.show).joinedload(Show.screen),
            joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
            joinedload(Booking.booking_snacks).joinedload(BookingSnack.snack),
        )
        .filter(Booking.id == booking_id, Booking.user_id == current_user.id)
        .first()
    )

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found",
        )

    # Clear previous snacks if re-submitting snacks
    db.query(BookingSnack).filter(BookingSnack.booking_id == booking_id).delete()

    snacks_total = 0.0
    booking_snack_responses = []

    for item in request.items:
        snack = db.query(Snack).filter(Snack.id == item.snack_id, Snack.is_available == True).first()
        if not snack:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Snack ID {item.snack_id} not available",
            )

        line_total = round(snack.price * item.quantity, 2)
        snacks_total += line_total

        b_snack = BookingSnack(
            booking_id=booking_id,
            snack_id=snack.id,
            quantity=item.quantity,
            unit_price=snack.price,
            total_price=line_total,
        )
        db.add(b_snack)

        booking_snack_responses.append(
            BookingSnackResponse(
                id=0,
                snack_id=snack.id,
                snack_name=snack.name,
                quantity=item.quantity,
                unit_price=snack.price,
                total_price=line_total,
            )
        )

    # Recalculate ticket subtotal from booking_seats
    ticket_subtotal = sum(bs.price for bs in booking.booking_seats)
    total_payable = round(ticket_subtotal + booking.convenience_fee + snacks_total, 2)

    booking.total_amount = total_payable
    db.commit()
    db.refresh(booking)

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
        show=ShowResponse.model_validate(booking.show) if booking.show else None,
        seats=seats_resp,
        snacks=booking_snack_responses,
        ticket_amount=ticket_subtotal + booking.convenience_fee,
        snacks_amount=snacks_total,
    )
