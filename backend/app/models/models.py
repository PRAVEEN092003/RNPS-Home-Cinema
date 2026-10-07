"""
SQLAlchemy ORM models for all database tables.
All tables for the CinemaApp are defined here.
"""
import enum
from datetime import datetime
from sqlalchemy import (
    Column, Integer, String, Boolean, DateTime, Float,
    ForeignKey, Enum, Text, UniqueConstraint
)
from sqlalchemy.orm import relationship
from app.config.database import Base


# ─── Enums ────────────────────────────────────────────────────────────────────

class UserRole(str, enum.Enum):
    USER = "USER"
    ADMIN = "ADMIN"


class BookingStatus(str, enum.Enum):
    PENDING = "PENDING"
    CONFIRMED = "CONFIRMED"
    CANCELLED = "CANCELLED"
    COMPLETED = "COMPLETED"


class PaymentStatus(str, enum.Enum):
    PENDING = "PENDING"
    SUBMITTED = "SUBMITTED"
    COMPLETED = "COMPLETED"
    PAID = "PAID"
    FAILED = "FAILED"
    REFUNDED = "REFUNDED"


class PaymentMethod(str, enum.Enum):
    CASH = "CASH"
    CARD = "CARD"
    UPI = "UPI"
    WALLET = "WALLET"


class SeatType(str, enum.Enum):
    STANDARD = "STANDARD"
    PREMIUM = "PREMIUM"
    RECLINER = "RECLINER"


class ShowSeatStatus(str, enum.Enum):
    AVAILABLE = "AVAILABLE"
    BOOKED = "BOOKED"
    BLOCKED = "BLOCKED"


# ─── Models ───────────────────────────────────────────────────────────────────

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False)
    email = Column(String(255), unique=True, index=True, nullable=False)
    phone = Column(String(20), nullable=True)
    hashed_password = Column(String(255), nullable=False)
    role = Column(Enum(UserRole), default=UserRole.USER, nullable=False)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    bookings = relationship("Booking", back_populates="user")

    def __repr__(self):
        return f"<User id={self.id} email={self.email} role={self.role}>"


class Movie(Base):
    __tablename__ = "movies"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    genre = Column(String(100), nullable=True)
    language = Column(String(50), nullable=False, default="English")
    duration_minutes = Column(Integer, nullable=False)
    rating = Column(String(10), nullable=True)      # e.g. "U/A", "A", "PG-13"
    imdb_rating = Column(Float, nullable=True)
    cast = Column(Text, nullable=True)               # comma-separated
    director = Column(String(100), nullable=True)
    poster_url = Column(String(500), nullable=True)
    banner_url = Column(String(500), nullable=True)
    trailer_url = Column(String(500), nullable=True)
    release_date = Column(DateTime, nullable=True)
    audio_technology = Column(String(50), nullable=False, default="Dolby Atmos")
    display_technology = Column(String(50), nullable=False, default="4K")
    is_upcoming = Column(Boolean, default=False, nullable=False)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    shows = relationship("Show", back_populates="movie")

    def __repr__(self):
        return f"<Movie id={self.id} title={self.title}>"


class Screen(Base):
    __tablename__ = "screens"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(50), nullable=False)         # e.g. "Screen 1", "Screen 2"
    display_spec = Column(String(100), nullable=False, default="4K Ultra HD")
    audio_spec = Column(String(100), nullable=False, default="13-Channel Dolby Atmos")
    has_dolby_atmos = Column(Boolean, default=True, nullable=False)
    total_seats = Column(Integer, nullable=False)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    seats = relationship("Seat", back_populates="screen")
    shows = relationship("Show", back_populates="screen")

    def __repr__(self):
        return f"<Screen id={self.id} name={self.name}>"


class Seat(Base):
    __tablename__ = "seats"

    id = Column(Integer, primary_key=True, index=True)
    screen_id = Column(Integer, ForeignKey("screens.id"), nullable=False)
    row_label = Column(String(5), nullable=False)     # e.g. "A", "B"
    seat_number = Column(Integer, nullable=False)     # e.g. 1, 2, 3
    seat_code = Column(String(10), nullable=False)    # e.g. "A1", "B12"
    seat_type = Column(Enum(SeatType), default=SeatType.STANDARD, nullable=False)
    base_price = Column(Float, nullable=False)
    is_active = Column(Boolean, default=True)

    __table_args__ = (
        UniqueConstraint("screen_id", "seat_code", name="uq_screen_seat"),
    )

    # Relationships
    screen = relationship("Screen", back_populates="seats")
    show_seats = relationship("ShowSeat", back_populates="seat")
    booking_seats = relationship("BookingSeat", back_populates="seat")

    def __repr__(self):
        return f"<Seat id={self.id} screen={self.screen_id} code={self.seat_code}>"


class Show(Base):
    __tablename__ = "shows"

    id = Column(Integer, primary_key=True, index=True)
    movie_id = Column(Integer, ForeignKey("movies.id"), nullable=False)
    screen_id = Column(Integer, ForeignKey("screens.id"), nullable=False)
    start_time = Column(DateTime, nullable=False)
    end_time = Column(DateTime, nullable=False)
    base_price = Column(Float, nullable=False)        # Override pricing per show
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    movie = relationship("Movie", back_populates="shows")
    screen = relationship("Screen", back_populates="shows")
    show_seats = relationship("ShowSeat", back_populates="show")
    bookings = relationship("Booking", back_populates="show")

    def __repr__(self):
        return f"<Show id={self.id} movie={self.movie_id} screen={self.screen_id}>"


class ShowSeat(Base):
    """Represents the real-time status of each seat for a specific show."""
    __tablename__ = "show_seats"

    id = Column(Integer, primary_key=True, index=True)
    show_id = Column(Integer, ForeignKey("shows.id"), nullable=False)
    seat_id = Column(Integer, ForeignKey("seats.id"), nullable=False)
    status = Column(Enum(ShowSeatStatus), default=ShowSeatStatus.AVAILABLE, nullable=False)
    price = Column(Float, nullable=False)             # Final price for this seat

    __table_args__ = (
        UniqueConstraint("show_id", "seat_id", name="uq_show_seat"),
    )

    # Relationships
    show = relationship("Show", back_populates="show_seats")
    seat = relationship("Seat", back_populates="show_seats")

    def __repr__(self):
        return f"<ShowSeat show={self.show_id} seat={self.seat_id} status={self.status}>"


class Booking(Base):
    __tablename__ = "bookings"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    show_id = Column(Integer, ForeignKey("shows.id"), nullable=False)
    booking_reference = Column(String(20), unique=True, nullable=False, index=True)
    status = Column(Enum(BookingStatus), default=BookingStatus.PENDING, nullable=False)
    total_amount = Column(Float, nullable=False)
    convenience_fee = Column(Float, default=0.0)
    total_seats = Column(Integer, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    user = relationship("User", back_populates="bookings")
    show = relationship("Show", back_populates="bookings")
    booking_seats = relationship("BookingSeat", back_populates="booking")
    booking_snacks = relationship("BookingSnack", back_populates="booking")
    payment = relationship("Payment", back_populates="booking", uselist=False)
    ticket = relationship("Ticket", back_populates="booking", uselist=False)

    def __repr__(self):
        return f"<Booking id={self.id} ref={self.booking_reference} status={self.status}>"


class BookingSeat(Base):
    __tablename__ = "booking_seats"

    id = Column(Integer, primary_key=True, index=True)
    booking_id = Column(Integer, ForeignKey("bookings.id"), nullable=False)
    seat_id = Column(Integer, ForeignKey("seats.id"), nullable=False)
    price = Column(Float, nullable=False)

    # Relationships
    booking = relationship("Booking", back_populates="booking_seats")
    seat = relationship("Seat", back_populates="booking_seats")

    def __repr__(self):
        return f"<BookingSeat booking={self.booking_id} seat={self.seat_id}>"


class Payment(Base):
    __tablename__ = "payments"

    id = Column(Integer, primary_key=True, index=True)
    booking_id = Column(Integer, ForeignKey("bookings.id"), unique=True, nullable=False)
    amount = Column(Float, nullable=False)
    method = Column(Enum(PaymentMethod), nullable=True)
    status = Column(Enum(PaymentStatus), default=PaymentStatus.PENDING, nullable=False)
    transaction_id = Column(String(100), nullable=True)
    paid_at = Column(DateTime, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    booking = relationship("Booking", back_populates="payment")

    def __repr__(self):
        return f"<Payment id={self.id} booking={self.booking_id} status={self.status}>"


class Ticket(Base):
    __tablename__ = "tickets"

    id = Column(Integer, primary_key=True, index=True)
    booking_id = Column(Integer, ForeignKey("bookings.id"), unique=True, nullable=False)
    ticket_code = Column(String(50), unique=True, nullable=False, index=True)
    qr_data = Column(Text, nullable=True)             # JSON or string for QR generation
    issued_at = Column(DateTime, default=datetime.utcnow)
    is_used = Column(Boolean, default=False)

    # Relationships
    booking = relationship("Booking", back_populates="ticket")

    def __repr__(self):
        return f"<Ticket id={self.id} code={self.ticket_code}>"


class SnackCategory(str, enum.Enum):
    POPCORN = "POPCORN"
    DRINKS = "DRINKS"
    COMBOS = "COMBOS"
    OTHER = "OTHER"


class Snack(Base):
    __tablename__ = "snacks"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100), nullable=False)
    description = Column(Text, nullable=True)
    category = Column(Enum(SnackCategory), default=SnackCategory.POPCORN, nullable=False)
    price = Column(Float, nullable=False)
    image_url = Column(String(500), nullable=True)
    is_available = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)

    # Relationships
    booking_snacks = relationship("BookingSnack", back_populates="snack")

    def __repr__(self):
        return f"<Snack id={self.id} name={self.name} price={self.price}>"


class BookingSnack(Base):
    __tablename__ = "booking_snacks"

    id = Column(Integer, primary_key=True, index=True)
    booking_id = Column(Integer, ForeignKey("bookings.id"), nullable=False)
    snack_id = Column(Integer, ForeignKey("snacks.id"), nullable=False)
    quantity = Column(Integer, nullable=False, default=1)
    unit_price = Column(Float, nullable=False)
    total_price = Column(Float, nullable=False)

    # Relationships
    booking = relationship("Booking", back_populates="booking_snacks")
    snack = relationship("Snack", back_populates="booking_snacks")

    def __repr__(self):
        return f"<BookingSnack booking={self.booking_id} snack={self.snack_id} qty={self.quantity}>"


class SystemSetting(Base):
    __tablename__ = "system_settings"

    key = Column(String(50), primary_key=True)
    value = Column(Text, nullable=False)

    def __repr__(self):
        return f"<SystemSetting key={self.key} value={self.value}>"

