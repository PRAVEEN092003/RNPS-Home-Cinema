"""
Payment submission and ticket issuing routes.
"""
from datetime import datetime
import json
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload
from app.config.database import get_db
from app.models.models import (
    Booking, Payment, Ticket, User, Show, BookingSeat, BookingSnack,
    PaymentStatus, PaymentMethod
)
from app.schemas.schemas import (
    SubmitPaymentRequest, BookingResponse, BookingSeatResponse,
    BookingSnackResponse, ShowResponse, PaymentResponse, TicketResponse
)
from app.utils.auth import get_current_user
from app.utils.helpers import generate_ticket_code

router = APIRouter(prefix="/payments", tags=["Payments"])


@router.get("/config", response_model=dict)
def get_payment_config(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get configurable UPI payment details."""
    from app.models.models import SystemSetting
    upi_id_item = db.query(SystemSetting).filter(SystemSetting.key == "upi_id").first()
    payee_name_item = db.query(SystemSetting).filter(SystemSetting.key == "payee_name").first()

    return {
        "upi_id": upi_id_item.value if upi_id_item else "rnpscinema@upi",
        "payee_name": payee_name_item.value if payee_name_item else "RNPS Home Cinema",
    }


@router.post("/submit", response_model=BookingResponse)
def submit_payment(
    request: SubmitPaymentRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Process payment submission for a booking.
    Saves Payment record with status SUBMITTED / PENDING in SQLite database.
    Generates official Ticket record and locks booking from further edits.
    Does NOT auto-approve/auto-mark as PAID.
    """
    booking = (
        db.query(Booking)
        .options(
            joinedload(Booking.show).joinedload(Show.movie),
            joinedload(Booking.show).joinedload(Show.screen),
            joinedload(Booking.booking_seats).joinedload(BookingSeat.seat),
            joinedload(Booking.booking_snacks).joinedload(BookingSnack.snack),
            joinedload(Booking.payment),
            joinedload(Booking.ticket),
        )
        .filter(Booking.id == request.booking_id, Booking.user_id == current_user.id)
        .first()
    )

    if not booking:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Booking not found",
        )

    # Prevent duplicate submission if already PAID or SUBMITTED
    existing_payment = db.query(Payment).filter(Payment.booking_id == booking.id).first()
    if existing_payment and existing_payment.status in [PaymentStatus.PAID, PaymentStatus.SUBMITTED]:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Payment has already been submitted for this booking.",
        )

    # 1. Create / update Payment record
    if not existing_payment:
        payment = Payment(
            booking_id=booking.id,
            amount=booking.total_amount,
            method=PaymentMethod.UPI,
            status=PaymentStatus.SUBMITTED,
            transaction_id=request.transaction_reference or f"UPI-TXN-{int(datetime.utcnow().timestamp())}",
            created_at=datetime.utcnow(),
        )
        db.add(payment)
    else:
        existing_payment.status = PaymentStatus.SUBMITTED
        existing_payment.transaction_id = request.transaction_reference or existing_payment.transaction_id
        payment = existing_payment

    # 2. Create Ticket record if not already issued
    existing_ticket = db.query(Ticket).filter(Ticket.booking_id == booking.id).first()
    if not existing_ticket:
        ticket_code = generate_ticket_code()
        qr_payload = {
            "ticket_code": ticket_code,
            "booking_ref": booking.booking_reference,
            "theatre": "RNPS Home Cinema",
            "movie": booking.show.movie.title if booking.show and booking.show.movie else "",
            "screen": booking.show.screen.name if booking.show and booking.show.screen else "",
            "seats": [bs.seat.seat_code for bs in booking.booking_seats if bs.seat],
            "total_amount": booking.total_amount,
        }
        ticket = Ticket(
            booking_id=booking.id,
            ticket_code=ticket_code,
            qr_data=json.dumps(qr_payload),
            issued_at=datetime.utcnow(),
            is_used=False,
        )
        db.add(ticket)
    else:
        ticket = existing_ticket

    db.commit()
    db.refresh(booking)

    # Prepare response models
    ticket_subtotal = sum(bs.price for bs in booking.booking_seats)
    snacks_subtotal = sum(bsn.total_price for bsn in booking.booking_snacks)

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
        snacks=snacks_resp,
        ticket_amount=ticket_subtotal + booking.convenience_fee,
        snacks_amount=snacks_subtotal,
        payment=PaymentResponse.model_validate(payment) if payment else None,
        ticket=TicketResponse.model_validate(ticket) if ticket else None,
    )
