"""
Show listing and seat layout routes.
"""
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload
from app.config.database import get_db
from app.models.models import Show, ShowSeat, Seat
from app.schemas.schemas import ShowResponse, ShowDetailsWithSeatsResponse, ShowSeatResponse
from app.utils.auth import get_current_user

router = APIRouter(prefix="/shows", tags=["Shows"])


@router.get("/", response_model=List[ShowResponse])
def list_shows(
    movie_id: int = None,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    """List upcoming active shows, optionally filtered by movie."""
    query = (
        db.query(Show)
        .options(joinedload(Show.movie), joinedload(Show.screen))
        .filter(Show.is_active == True)
    )
    if movie_id:
        query = query.filter(Show.movie_id == movie_id)
    shows = query.all()
    return [ShowResponse.model_validate(s) for s in shows]


@router.get("/{show_id}", response_model=ShowResponse)
def get_show(
    show_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    """Get details of a specific show."""
    show = (
        db.query(Show)
        .options(joinedload(Show.movie), joinedload(Show.screen))
        .filter(Show.id == show_id, Show.is_active == True)
        .first()
    )
    if not show:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Show not found",
        )
    return ShowResponse.model_validate(show)


@router.get("/{show_id}/seats", response_model=ShowDetailsWithSeatsResponse)
def get_show_seats(
    show_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    """Get complete layout and seat availability for a specific show."""
    show = (
        db.query(Show)
        .options(joinedload(Show.movie), joinedload(Show.screen))
        .filter(Show.id == show_id, Show.is_active == True)
        .first()
    )
    if not show:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Show not found",
        )

    # Join ShowSeat with Seat to get row_label, seat_number, seat_code, seat_type
    results = (
        db.query(ShowSeat, Seat)
        .join(Seat, ShowSeat.seat_id == Seat.id)
        .filter(ShowSeat.show_id == show_id)
        .order_by(Seat.row_label.asc(), Seat.seat_number.asc())
        .all()
    )

    seat_responses = []
    for show_seat, seat in results:
        seat_responses.append(
            ShowSeatResponse(
                id=show_seat.id,
                show_id=show_seat.show_id,
                seat_id=show_seat.seat_id,
                row_label=seat.row_label,
                seat_number=seat.seat_number,
                seat_code=seat.seat_code,
                seat_type=seat.seat_type,
                status=show_seat.status,
                price=show_seat.price,
            )
        )

    return ShowDetailsWithSeatsResponse(
        show=ShowResponse.model_validate(show),
        seats=seat_responses,
    )
