"""
Database migrations module for SQLite schema updates.
Safely adds new columns to existing tables without data loss.
"""
from sqlalchemy import text
from sqlalchemy.engine import Engine


def run_migrations(engine: Engine) -> None:
    """Safely apply column additions for existing SQLite databases."""
    with engine.connect() as conn:
        # 1. Check screens table
        res = conn.execute(text("PRAGMA table_info(screens)")).fetchall()
        existing_screen_cols = [row[1] for row in res]

        if existing_screen_cols:  # Table exists
            if "display_spec" not in existing_screen_cols:
                print("[MIGRATION] Adding display_spec column to screens table...")
                conn.execute(text("ALTER TABLE screens ADD COLUMN display_spec VARCHAR(100) NOT NULL DEFAULT '4K Ultra HD'"))
            
            if "audio_spec" not in existing_screen_cols:
                print("[MIGRATION] Adding audio_spec column to screens table...")
                conn.execute(text("ALTER TABLE screens ADD COLUMN audio_spec VARCHAR(100) NOT NULL DEFAULT '13-Channel Dolby Atmos'"))

            if "has_dolby_atmos" not in existing_screen_cols:
                print("[MIGRATION] Adding has_dolby_atmos column to screens table...")
                conn.execute(text("ALTER TABLE screens ADD COLUMN has_dolby_atmos BOOLEAN NOT NULL DEFAULT 1"))

            # Ensure Screen 1 and Screen 2 have correct initial values if defaults were applied
            conn.execute(text(
                "UPDATE screens SET display_spec = '4K Ultra HD', audio_spec = '13-Channel Dolby Atmos', has_dolby_atmos = 1 WHERE id = 1"
            ))
            conn.execute(text(
                "UPDATE screens SET display_spec = 'Full HD 1080p', audio_spec = '7.1 Surround Sound', has_dolby_atmos = 0 WHERE id = 2 AND (display_spec = '4K Ultra HD' OR display_spec IS NULL)"
            ))

        # 2. Check movies table
        res_movies = conn.execute(text("PRAGMA table_info(movies)")).fetchall()
        existing_movie_cols = [row[1] for row in res_movies]

        if existing_movie_cols:
            if "is_upcoming" not in existing_movie_cols:
                print("[MIGRATION] Adding is_upcoming column to movies table...")
                conn.execute(text("ALTER TABLE movies ADD COLUMN is_upcoming BOOLEAN NOT NULL DEFAULT 0"))

            if "audio_technology" not in existing_movie_cols:
                print("[MIGRATION] Adding audio_technology column to movies table...")
                conn.execute(text("ALTER TABLE movies ADD COLUMN audio_technology VARCHAR(50) NOT NULL DEFAULT 'Dolby Atmos'"))

            if "display_technology" not in existing_movie_cols:
                print("[MIGRATION] Adding display_technology column to movies table...")
                conn.execute(text("ALTER TABLE movies ADD COLUMN display_technology VARCHAR(50) NOT NULL DEFAULT '4K'"))

        conn.commit()
    print("[MIGRATION] Database schema migration check complete.")
