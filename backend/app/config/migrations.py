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

    print("[MIGRATION] Database schema migration check complete.")