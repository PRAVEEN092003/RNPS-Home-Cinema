"""
SQLAlchemy database engine and session setup.
"""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from app.config.settings import get_settings

settings = get_settings()

database_url = settings.database_url

# SQLite-specific configuration
if database_url.startswith("sqlite"):
    engine = create_engine(
        database_url,
        connect_args={
            "check_same_thread": False,
            "timeout": 15,
        },
        echo=settings.debug,
    )

# PostgreSQL / Neon configuration
else:
    engine = create_engine(
        database_url,
        pool_pre_ping=True,
        echo=settings.debug,
    )

SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine,
)

Base = declarative_base()


def get_db():
    """Dependency: yields a database session and closes it after use."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()