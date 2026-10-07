"""
Pydantic schemas for request/response validation.
"""
from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, EmailStr, Field
from app.models.models import UserRole, SeatType, ShowSeatStatus, BookingStatus, PaymentStatus, PaymentMethod, SnackCategory


# ─── Auth Schemas ─────────────────────────────────────────────────────────────

class RegisterRequest(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    email: EmailStr
    phone: Optional[str] = Field(None, max_length=20)
    password: str = Field(..., min_length=6)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=1)


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: "UserResponse"


# ─── User Schemas ─────────────────────────────────────────────────────────────

class UserResponse(BaseModel):
    id: int
    name: str
    email: str
    phone: Optional[str]
    role: UserRole
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True


class UserUpdateRequest(BaseModel):
    name: Optional[str] = Field(None, min_length=2, max_length=100)
    phone: Optional[str] = Field(None, max_length=20)


# ─── Movie Schemas ────────────────────────────────────────────────────────────

class MovieCreateRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = None
    genre: Optional[str] = None
    language: str = "English"
    duration_minutes: int = Field(..., gt=0)
    rating: Optional[str] = "U/A"
    imdb_rating: Optional[float] = None
    cast: Optional[str] = None
    director: Optional[str] = None
    poster_url: Optional[str] = None
    audio_technology: str = "Dolby Atmos"
    display_technology: str = "4K"
    is_upcoming: bool = False


class MovieUpdateRequest(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    genre: Optional[str] = None
    language: Optional[str] = None
    duration_minutes: Optional[int] = None
    rating: Optional[str] = None
    imdb_rating: Optional[float] = None
    poster_url: Optional[str] = None
    audio_technology: Optional[str] = None
    display_technology: Optional[str] = None
    is_upcoming: Optional[bool] = None
    is_active: Optional[bool] = None


class MovieResponse(BaseModel):
    id: int
    title: str
    description: Optional[str]
    genre: Optional[str]
    language: str
    duration_minutes: int
    rating: Optional[str]
    imdb_rating: Optional[float]
    cast: Optional[str]
    director: Optional[str]
    poster_url: Optional[str]
    banner_url: Optional[str]
    trailer_url: Optional[str]
    release_date: Optional[datetime]
    audio_technology: str = "Dolby Atmos"
    display_technology: str = "4K"
    is_upcoming: bool = False
    is_active: bool
    created_at: datetime

    class Config:
        from_attributes = True


# ─── Screen Schemas ───────────────────────────────────────────────────────────

class ScreenUpdateRequest(BaseModel):
    name: Optional[str] = None
    display_spec: Optional[str] = None
    audio_spec: Optional[str] = None
    has_dolby_atmos: Optional[bool] = None
    seat_base_price: Optional[float] = None


class ScreenResponse(BaseModel):
    id: int
    name: str
    display_spec: str = "4K Ultra HD"
    audio_spec: str = "13-Channel Dolby Atmos"
    has_dolby_atmos: bool = True
    total_seats: int
    is_active: bool

    class Config:
        from_attributes = True


# ─── Show Schemas ─────────────────────────────────────────────────────────────

class ShowCreateRequest(BaseModel):
    movie_id: int
    screen_id: int
    start_time: datetime
    end_time: datetime
    base_price: float = Field(..., gt=0)


class ShowUpdateRequest(BaseModel):
    movie_id: Optional[int] = None
    screen_id: Optional[int] = None
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    base_price: Optional[float] = None
    is_active: Optional[bool] = None


class ShowResponse(BaseModel):
    id: int
    movie_id: int
    screen_id: int
    start_time: datetime
    end_time: datetime
    base_price: float
    is_active: bool
    movie: Optional[MovieResponse] = None
    screen: Optional[ScreenResponse] = None

    class Config:
        from_attributes = True


# ─── Seat & ShowSeat Schemas ──────────────────────────────────────────────────

class ShowSeatResponse(BaseModel):
    id: int
    show_id: int
    seat_id: int
    row_label: str
    seat_number: int
    seat_code: str
    seat_type: SeatType
    status: ShowSeatStatus
    price: float

    class Config:
        from_attributes = True


class ShowDetailsWithSeatsResponse(BaseModel):
    show: ShowResponse
    seats: List[ShowSeatResponse]


# ─── Snack Schemas ───────────────────────────────────────────────────────────

class SnackCreateRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    description: Optional[str] = None
    category: SnackCategory = SnackCategory.POPCORN
    price: float = Field(..., gt=0)
    image_url: Optional[str] = None


class SnackUpdateRequest(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    category: Optional[SnackCategory] = None
    price: Optional[float] = None
    image_url: Optional[str] = None
    is_available: Optional[bool] = None


class SnackResponse(BaseModel):
    id: int
    name: str
    description: Optional[str]
    category: str
    price: float
    image_url: Optional[str]
    is_available: bool

    class Config:
        from_attributes = True


class SnackItemRequest(BaseModel):
    snack_id: int
    quantity: int = Field(..., ge=1)


class AddSnacksRequest(BaseModel):
    items: List[SnackItemRequest]


class BookingSnackResponse(BaseModel):
    id: int
    snack_id: int
    snack_name: str
    quantity: int
    unit_price: float
    total_price: float


# ─── Payment & Ticket Schemas ──────────────────────────────────────────────────

class TicketResponse(BaseModel):
    id: int
    ticket_code: str
    qr_data: Optional[str] = None
    issued_at: datetime
    is_used: bool

    class Config:
        from_attributes = True


class PaymentResponse(BaseModel):
    id: int
    booking_id: int
    amount: float
    method: Optional[PaymentMethod] = None
    status: PaymentStatus
    transaction_id: Optional[str] = None
    paid_at: Optional[datetime] = None
    created_at: datetime

    class Config:
        from_attributes = True


class SubmitPaymentRequest(BaseModel):
    booking_id: int
    payment_method: str = "UPI"
    transaction_reference: Optional[str] = None


class PaymentConfigResponse(BaseModel):
    upi_id: str
    payee_name: str


class PaymentConfigRequest(BaseModel):
    upi_id: str
    payee_name: str


# ─── Booking Schemas ──────────────────────────────────────────────────────────

class CreateBookingRequest(BaseModel):
    show_id: int
    seat_ids: List[int] = Field(..., min_items=1)


class BookingSeatResponse(BaseModel):
    id: int
    seat_id: int
    seat_code: str
    row_label: str
    seat_number: int
    price: float


class BookingResponse(BaseModel):
    id: int
    user_id: int
    show_id: int
    booking_reference: str
    status: BookingStatus
    total_amount: float
    convenience_fee: float
    total_seats: int
    created_at: datetime
    show: Optional[ShowResponse] = None
    seats: List[BookingSeatResponse] = []
    snacks: List[BookingSnackResponse] = []
    ticket_amount: float = 0.0
    snacks_amount: float = 0.0
    payment: Optional[PaymentResponse] = None
    ticket: Optional[TicketResponse] = None

    class Config:
        from_attributes = True


# ─── Health Schema ────────────────────────────────────────────────────────────

class HealthResponse(BaseModel):
    status: str
    app_name: str
    version: str
    database: str
    timestamp: datetime


# ─── Error Schema ─────────────────────────────────────────────────────────────

class ErrorResponse(BaseModel):
    detail: str
    error_code: Optional[str] = None


# ─── Update forward refs ──────────────────────────────────────────────────────
TokenResponse.model_rebuild()
