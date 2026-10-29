"""
Database migrations module.
Safely adds new columns to existing database tables.
Supports both SQLite and PostgreSQL/Neon.
"""

from sqlalchemy import inspect, text
from sqlalchemy.engine import Engine


def run_migrations(engine: Engine) -> None:
    """Safely apply required schema migrations."""

    inspector = inspect(engine)

    # ============================================================
    # 1. SCREENS TABLE
    # ============================================================
    if "screens" in inspector.get_table_names():
        existing_screen_cols = {
            column["name"] for column in inspector.get_columns("screens")
        }

        if "display_spec" not in existing_screen_cols:
            print("[MIGRATION] Adding display_spec column...")
            with engine.begin() as conn:
                conn.execute(
                    text(
                        """
                        ALTER TABLE screens
                        ADD COLUMN display_spec VARCHAR(100)
                        DEFAULT '4K Ultra HD'
                        """
                    )
                )

        if "audio_spec" not in existing_screen_cols:
            print("[MIGRATION] Adding audio_spec column...")
            with engine.begin() as conn:
                conn.execute(
                    text(
                        """
                        ALTER TABLE screens
                        ADD COLUMN audio_spec VARCHAR(100)
                        DEFAULT '13-Channel Dolby Atmos'
                        """
                    )
                )

        if "has_dolby_atmos" not in existing_screen_cols:
            print("[MIGRATION] Adding has_dolby_atmos column...")
            with engine.begin() as conn:
                conn.execute(
                    text(
                        """
                        ALTER TABLE screens
                        ADD COLUMN has_dolby_atmos BOOLEAN
                        DEFAULT TRUE
                        """
                    )
                )

        # Ensure initial screen values
        with engine.begin() as conn:
            conn.execute(
                text(
                    """
                    UPDATE screens
                    SET
                        display_spec = '4K Ultra HD',
                        audio_spec = '13-Channel Dolby Atmos',
                        has_dolby_atmos = TRUE
                    WHERE id = 1
                    """
                )
            )

            conn.execute(
                text(
                    """
                    UPDATE screens
                    SET
                        display_spec = 'Full HD 1080p',
                        audio_spec = '7.1 Surround Sound',
                        has_dolby_atmos = FALSE
                    WHERE id = 2
                    AND (
                        display_spec = '4K Ultra HD'
                        OR display_spec IS NULL
                    )
                    """
                )
            )

    # ============================================================
    # 2. MOVIES TABLE
    # ============================================================
    if "movies" in inspector.get_table_names():
        existing_movie_cols = {
            column["name"] for column in inspector.get_columns("movies")
        }

        if "is_upcoming" not in existing_movie_cols:
            print("[MIGRATION] Adding is_upcoming column...")
            with engine.begin() as conn:
                conn.execute(
                    text(
                        """
                        ALTER TABLE movies
                        ADD COLUMN is_upcoming BOOLEAN
                        DEFAULT FALSE
                        """
                    )
                )

        if "audio_technology" not in existing_movie_cols:
            print("[MIGRATION] Adding audio_technology column...")
            with engine.begin() as conn:
                conn.execute(
                    text(
                        """
                        ALTER TABLE movies
                        ADD COLUMN audio_technology VARCHAR(50)
                        DEFAULT 'Dolby Atmos'
                        """
                    )
                )

        if "display_technology" not in existing_movie_cols:
            print("[MIGRATION] Adding display_technology column...")
            with engine.begin() as conn:
                conn.execute(
                    text(
                        """
                        ALTER TABLE movies
                        ADD COLUMN display_technology VARCHAR(50)
                        DEFAULT '4K'
                        """
                    )
                )

    # ============================================================
    # 3. SYNC AVAILABLE SHOW SEATS WITH AUTHORITATIVE SHOW BASE PRICE
    # ============================================================
    if "show_seats" in inspector.get_table_names() and "shows" in inspector.get_table_names():
        print("[MIGRATION] Syncing AVAILABLE show_seats with authoritative show base_price...")
        try:
            with engine.begin() as conn:
                if engine.dialect.name == "postgresql":
                    conn.execute(
                        text(
                            """
                            UPDATE show_seats
                            SET price = shows.base_price
                            FROM shows
                            WHERE show_seats.show_id = shows.id
                              AND show_seats.status = 'AVAILABLE'
                              AND shows.base_price > 0
                              AND show_seats.price != shows.base_price
                            """
                        )
                    )
                else:
                    conn.execute(
                        text(
                            """
                            UPDATE show_seats
                            SET price = (
                                SELECT shows.base_price
                                FROM shows
                                WHERE shows.id = show_seats.show_id
                            )
                            WHERE show_seats.status = 'AVAILABLE'
                              AND EXISTS (
                                SELECT 1
                                FROM shows
                                WHERE shows.id = show_seats.show_id
                                  AND shows.base_price > 0
                                  AND show_seats.price != shows.base_price
                            )
                            """
                        )
                    )
            print("[MIGRATION] Successfully synced AVAILABLE show_seats prices.")
        except Exception as e:
            print(f"[MIGRATION] Warning: show_seats price sync encountered error: {e}")

    # ============================================================
    # 4. POSTGRESQL ENUM SHOWSEATSTATUS: ENSURE 'HELD' VALUE EXISTS
    # ============================================================
    if engine.dialect.name == "postgresql":
        try:
            with engine.connect() as conn:
                has_held = conn.execute(
                    text(
                        """
                        SELECT 1 
                        FROM pg_enum e 
                        JOIN pg_type t ON e.enumtypid = t.oid 
                        JOIN pg_namespace n ON t.typnamespace = n.oid 
                        WHERE n.nspname = 'public' 
                          AND t.typname = 'showseatstatus' 
                          AND e.enumlabel = 'HELD'
                        """
                    )
                ).scalar()

            if not has_held:
                print("[MIGRATION] Adding 'HELD' value to public.showseatstatus enum...")
                with engine.connect().execution_options(isolation_level="AUTOCOMMIT") as conn:
                    conn.execute(
                        text("ALTER TYPE public.showseatstatus ADD VALUE IF NOT EXISTS 'HELD'")
                    )
                print("[MIGRATION] Successfully added 'HELD' to public.showseatstatus enum.")
            else:
                print("[MIGRATION] Enum public.showseatstatus already contains 'HELD'.")
        except Exception as e:
            print(f"[MIGRATION] Warning: Enum migration check encountered: {e}")

    # ============================================================
    # 5. BOOKINGS, PAYMENTS & TICKETS NEW AUDIT COLUMNS
    # ============================================================
    if "bookings" in inspector.get_table_names():
        existing_booking_cols = {col["name"] for col in inspector.get_columns("bookings")}
        if "cancelled_at" not in existing_booking_cols:
            print("[MIGRATION] Adding cancelled_at column to bookings...")
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE bookings ADD COLUMN cancelled_at TIMESTAMP NULL"))

    if "payments" in inspector.get_table_names():
        existing_payment_cols = {col["name"] for col in inspector.get_columns("payments")}
        if "verified_at" not in existing_payment_cols:
            print("[MIGRATION] Adding verified_at column to payments...")
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE payments ADD COLUMN verified_at TIMESTAMP NULL"))
        if "verified_by_id" not in existing_payment_cols:
            print("[MIGRATION] Adding verified_by_id column to payments...")
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE payments ADD COLUMN verified_by_id INTEGER NULL"))
        if "remarks" not in existing_payment_cols:
            print("[MIGRATION] Adding remarks column to payments...")
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE payments ADD COLUMN remarks TEXT NULL"))

    if "tickets" in inspector.get_table_names():
        existing_ticket_cols = {col["name"] for col in inspector.get_columns("tickets")}
        if "used_at" not in existing_ticket_cols:
            print("[MIGRATION] Adding used_at column to tickets...")
            with engine.begin() as conn:
                conn.execute(text("ALTER TABLE tickets ADD COLUMN used_at TIMESTAMP NULL"))

    print("[MIGRATION] Database schema migration check complete.")