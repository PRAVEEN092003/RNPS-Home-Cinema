"""
Test database schema migration on legacy SQLite databases.
"""
import sqlite3
import pytest
from sqlalchemy import create_engine, text
from app.config.migrations import run_migrations


def test_legacy_database_migration(tmp_path):
    db_file = tmp_path / "legacy.db"
    
    # 1. Create a legacy SQLite table without display_spec, audio_spec, has_dolby_atmos, or is_upcoming
    conn = sqlite3.connect(str(db_file))
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE screens (
            id INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            total_seats INTEGER NOT NULL,
            is_active INTEGER DEFAULT 1,
            created_at TEXT
        );
    """)
    cursor.execute("""
        INSERT INTO screens (id, name, total_seats, is_active) VALUES (1, 'Screen 1', 8, 1);
    """)
    cursor.execute("""
        INSERT INTO screens (id, name, total_seats, is_active) VALUES (2, 'Screen 2', 6, 1);
    """)
    cursor.execute("""
        CREATE TABLE movies (
            id INTEGER PRIMARY KEY,
            title TEXT NOT NULL,
            duration_minutes INTEGER NOT NULL,
            is_active INTEGER DEFAULT 1
        );
    """)
    cursor.execute("""
        INSERT INTO movies (id, title, duration_minutes) VALUES (1, 'Inception', 148);
    """)
    conn.commit()
    conn.close()

    # 2. Run migrations on legacy database
    engine = create_engine(f"sqlite:///{db_file}")
    run_migrations(engine)

    # 3. Verify columns and default values were added without data loss
    conn2 = sqlite3.connect(str(db_file))
    c2 = conn2.cursor()
    
    c2.execute("SELECT id, name, display_spec, audio_spec, has_dolby_atmos FROM screens WHERE id = 1")
    s1 = c2.fetchone()
    assert s1[0] == 1
    assert s1[1] == "Screen 1"
    assert s1[2] == "4K Ultra HD"
    assert s1[3] == "13-Channel Dolby Atmos"
    assert s1[4] == 1

    c2.execute("SELECT id, name, display_spec, audio_spec, has_dolby_atmos FROM screens WHERE id = 2")
    s2 = c2.fetchone()
    assert s2[0] == 2
    assert s2[1] == "Screen 2"
    assert s2[2] == "Full HD 1080p"
    assert s2[3] == "7.1 Surround Sound"
    assert s2[4] == 0

    c2.execute("SELECT id, title, is_upcoming FROM movies WHERE id = 1")
    m1 = c2.fetchone()
    assert m1[0] == 1
    assert m1[1] == "Inception"
    assert m1[2] == 0

    conn2.close()
