"""
Database seeding: creates default admin user, system settings, and initial dataset for RNPS Home Cinema.
Fully idempotent and compatible with both SQLite and PostgreSQL (Neon).
"""
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from app.models.models import (
    User, UserRole, Screen, Seat, SeatType, Movie, Show, ShowSeat, ShowSeatStatus, Snack, SnackCategory, SystemSetting
)
from app.utils.auth import hash_password


def seed_admin(db: Session) -> None:
    """Create default admin account if it does not exist."""
    existing = db.query(User).filter(User.email == "admin@rnps.com").first()
    if not existing:
        admin = User(
            name="Admin",
            email="admin@rnps.com",
            phone="+91-9876543210",
            hashed_password=hash_password("admin123"),
            role=UserRole.ADMIN,
            is_active=True,
        )
        db.add(admin)
        db.commit()
        print("[SEED] Default admin created: admin@rnps.com / admin123")


def seed_system_settings(db: Session) -> None:
    """Seed default UPI payment settings."""
    settings_dict = {
        "upi_id": "rnpscinema@upi",
        "payee_name": "RNPS Home Cinema",
    }
    for key, val in settings_dict.items():
        existing = db.query(SystemSetting).filter(SystemSetting.key == key).first()
        if not existing:
            db.add(SystemSetting(key=key, value=val))
    db.commit()


def seed_cinema_data(db: Session) -> None:
    """Seed screens, seats, movies, shows, and show_seats for RNPS Home Cinema."""
    # 1. Screens (Screen 1: Dolby Atmos 8 seats, Screen 2: Admin editable 6 seats)
    screen1 = db.query(Screen).filter(Screen.name == "Screen 1").first()
    if not screen1:
        screen1 = Screen(
            name="Screen 1",
            display_spec="4K Ultra HD",
            audio_spec="13-Channel Dolby Atmos",
            has_dolby_atmos=True,
            total_seats=8,
            is_active=True,
        )
        db.add(screen1)
        db.commit()
        db.refresh(screen1)

    screen2 = db.query(Screen).filter(Screen.name == "Screen 2").first()
    if not screen2:
        screen2 = Screen(
            name="Screen 2",
            display_spec="Full HD 1080p",
            audio_spec="7.1 Surround Sound",
            has_dolby_atmos=False,
            total_seats=6,
            is_active=True,
        )
        db.add(screen2)
        db.commit()
        db.refresh(screen2)

    # 2. Seats for Screen 1 (Exactly 8 seats: A1..A4, B1..B4)
    existing_seats_s1 = db.query(Seat).filter(Seat.screen_id == screen1.id).count()
    if existing_seats_s1 == 0:
        seats_screen1 = []
        for row_label in ["A", "B"]:
            for num in range(1, 5):
                seat_code = f"{row_label}{num}"
                seats_screen1.append(Seat(
                    screen_id=screen1.id,
                    row_label=row_label,
                    seat_number=num,
                    seat_code=seat_code,
                    seat_type=SeatType.PREMIUM,
                    base_price=250.0,
                    is_active=True,
                ))
        db.add_all(seats_screen1)
        db.commit()

    # Seats for Screen 2 (Exactly 6 seats: A1..A3, B1..B3)
    existing_seats_s2 = db.query(Seat).filter(Seat.screen_id == screen2.id).count()
    if existing_seats_s2 == 0:
        seats_screen2 = []
        for row_label in ["A", "B"]:
            for num in range(1, 4):
                seat_code = f"{row_label}{num}"
                seats_screen2.append(Seat(
                    screen_id=screen2.id,
                    row_label=row_label,
                    seat_number=num,
                    seat_code=seat_code,
                    seat_type=SeatType.STANDARD,
                    base_price=180.0,
                    is_active=True,
                ))
        db.add_all(seats_screen2)
        db.commit()

    # 3. Movies
    movie1 = db.query(Movie).filter(Movie.title == "Inception").first()
    if not movie1:
        movie1 = Movie(
            title="Inception",
            description="A thief who steals corporate secrets through dream-sharing technology is given the inverse task of planting an idea into a CEO's mind.",
            genre="Sci-Fi / Action",
            language="English",
            duration_minutes=148,
            rating="U/A",
            imdb_rating=8.8,
            director="Christopher Nolan",
            cast="Leonardo DiCaprio, Joseph Gordon-Levitt, Elliot Page",
            poster_url="https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&q=80",
            audio_technology="Dolby Atmos",
            display_technology="4K",
            is_upcoming=False,
            is_active=True,
        )
        db.add(movie1)
        db.commit()
        db.refresh(movie1)

    movie2 = db.query(Movie).filter(Movie.title == "The Dark Knight").first()
    if not movie2:
        movie2 = Movie(
            title="The Dark Knight",
            description="When the menace known as the Joker wreaks havoc on Gotham, Batman must face his greatest test to fight injustice.",
            genre="Action / Crime",
            language="English",
            duration_minutes=152,
            rating="U/A",
            imdb_rating=9.0,
            director="Christopher Nolan",
            cast="Christian Bale, Heath Ledger, Aaron Eckhart",
            poster_url="https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&q=80",
            audio_technology="Dolby Atmos",
            display_technology="4K",
            is_upcoming=False,
            is_active=True,
        )
        db.add(movie2)
        db.commit()
        db.refresh(movie2)

    movie3 = db.query(Movie).filter(Movie.title == "Interstellar").first()
    if not movie3:
        movie3 = Movie(
            title="Interstellar",
            description="When Earth becomes uninhabitable, a team of ex-NASA researchers travels through a wormhole in search of a new home.",
            genre="Sci-Fi / Drama",
            language="English",
            duration_minutes=169,
            rating="U/A",
            imdb_rating=8.7,
            director="Christopher Nolan",
            cast="Matthew McConaughey, Anne Hathaway, Jessica Chastain",
            poster_url="https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&q=80",
            audio_technology="Dolby Digital 5.1",
            display_technology="4K",
            is_upcoming=True,
            is_active=True,
        )
        db.add(movie3)
        db.commit()
        db.refresh(movie3)

    # 4. Shows & Show Seats
    if db.query(Show).first() is None and movie1 and movie2 and screen1 and screen2:
        now = datetime.utcnow().replace(minute=0, second=0, microsecond=0)
        shows = [
            Show(
                movie_id=movie1.id,
                screen_id=screen1.id,
                start_time=now + timedelta(hours=2),
                end_time=now + timedelta(hours=4, minutes=30),
                base_price=250.0,
                is_active=True,
            ),
            Show(
                movie_id=movie1.id,
                screen_id=screen2.id,
                start_time=now + timedelta(hours=6),
                end_time=now + timedelta(hours=8, minutes=30),
                base_price=180.0,
                is_active=True,
            ),
            Show(
                movie_id=movie2.id,
                screen_id=screen1.id,
                start_time=now + timedelta(days=1, hours=3),
                end_time=now + timedelta(days=1, hours=5, minutes=30),
                base_price=250.0,
                is_active=True,
            ),
        ]
        db.add_all(shows)
        db.commit()

        for show in shows:
            seats = db.query(Seat).filter(Seat.screen_id == show.screen_id).all()
            for idx, seat in enumerate(seats):
                status = ShowSeatStatus.BOOKED if idx == 1 else ShowSeatStatus.AVAILABLE
                db.add(ShowSeat(
                    show_id=show.id,
                    seat_id=seat.id,
                    status=status,
                    price=seat.base_price,
                ))
        db.commit()

    print("[SEED] RNPS Home Cinema dataset verified / seeded successfully!")


def seed_snacks(db: Session) -> None:
    """Seed snacks menu into database with INR pricing."""
    default_snacks = [
        {
            "name": "Large Caramel Popcorn",
            "description": "Crispy, freshly popped corn coated in rich golden caramel sauce.",
            "category": SnackCategory.POPCORN,
            "price": 240.0,
            "image_url": "https://images.unsplash.com/photo-1585647347384-2593bc35786b?w=400&q=80",
            "is_available": True,
        },
        {
            "name": "Medium Butter Popcorn",
            "description": "Classic movie theater popcorn tossed with melted real butter.",
            "category": SnackCategory.POPCORN,
            "price": 180.0,
            "image_url": "https://images.unsplash.com/photo-1578849278619-e73505e9610f?w=400&q=80",
            "is_available": True,
        },
        {
            "name": "Coca-Cola Zero Sugar (500ml)",
            "description": "Chilled 500ml bottle of refreshing Coca-Cola Zero.",
            "category": SnackCategory.DRINKS,
            "price": 120.0,
            "image_url": "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400&q=80",
            "is_available": True,
        },
        {
            "name": "Signature Iced Mocha",
            "description": "Chilled espresso with dark chocolate sauce and whipped cream.",
            "category": SnackCategory.DRINKS,
            "price": 160.0,
            "image_url": "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=400&q=80",
            "is_available": True,
        },
        {
            "name": "RNPS VIP Movie Combo",
            "description": "1 Large Caramel Popcorn + 2 Drinks + 1 Cheese Nachos platter.",
            "category": SnackCategory.COMBOS,
            "price": 450.0,
            "image_url": "https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=400&q=80",
            "is_available": True,
        },
        {
            "name": "Loaded Cheese Nachos",
            "description": "Tortilla chips served with hot jalapeño cheese dip and salsa.",
            "category": SnackCategory.OTHER,
            "price": 200.0,
            "image_url": "https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?w=400&q=80",
            "is_available": True,
        },
    ]

    for item in default_snacks:
        existing = db.query(Snack).filter(Snack.name == item["name"]).first()
        if not existing:
            db.add(Snack(**item))
        elif existing.price < 50:
            existing.price = item["price"]
    db.commit()
    print("[SEED] Snacks menu verified / seeded with INR prices!")


def run_seeds(db: Session) -> None:
    """Run all seed functions."""
    seed_admin(db)
    seed_system_settings(db)
    seed_cinema_data(db)
    seed_snacks(db)

