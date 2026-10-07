"""
Health check endpoint.
"""
from datetime import datetime
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.config.database import get_db
from app.config.settings import get_settings
from app.schemas.schemas import HealthResponse

router = APIRouter(tags=["Health"])
settings = get_settings()


@router.get("/health", response_model=HealthResponse)
def health_check(db: Session = Depends(get_db)):
    """
    Health check endpoint.
    Verifies that the API and database are reachable.
    """
    # Verify DB is accessible
    try:
        db.execute(__import__("sqlalchemy").text("SELECT 1"))
        db_status = "ok"
    except Exception:
        db_status = "error"

    return HealthResponse(
        status="ok",
        app_name=settings.app_name,
        version=settings.app_version,
        database=db_status,
        timestamp=datetime.utcnow(),
    )
