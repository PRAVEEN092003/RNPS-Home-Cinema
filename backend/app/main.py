"""
CinemaApp FastAPI Application — Main entry point.
"""
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
from app.config.settings import get_settings
from app.config.database import engine, SessionLocal
from app.config.migrations import run_migrations
from app.models.models import Base
from app.routes import (
    health_router, auth_router, users_router,
    movies_router, shows_router, bookings_router,
    snacks_router, payments_router, admin_router,
)
from app.services.seeder import run_seeds

settings = get_settings()


def init_db():
    """Initializes tables, migrations, and seed data when needed."""
    from app.models.models import SystemSetting
    
    # Quick check if database is already at current schema version
    db = SessionLocal()
    is_ready = False
    try:
        ver = db.query(SystemSetting.value).filter(SystemSetting.key == "schema_version").first()
        is_ready = (ver is not None and ver[0] == "2")
    except Exception:
        is_ready = False
    finally:
        db.close()

    if is_ready:
        print("[APP] Database schema is up to date (v2). Fast startup path active.")
        return

    print("[APP] Applying schema migrations and initial seeds...")
    Base.metadata.create_all(bind=engine)
    run_migrations(engine)
    db = SessionLocal()
    try:
        run_seeds(db)
        ver_setting = db.query(SystemSetting).filter(SystemSetting.key == "schema_version").first()
        if not ver_setting:
            db.add(SystemSetting(key="schema_version", value="2"))
        else:
            ver_setting.value = "2"
        db.commit()
    finally:
        db.close()
    print("[APP] Database initialization completed.")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup: ensure database is initialized without redundant delay."""
    init_db()
    print(f"[APP] {settings.app_name} v{settings.app_version} started")
    yield
    print("[APP] Application shutting down")


# ─── App Instance ─────────────────────────────────────────────────────────────

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Backend API for CinemaApp — single theatre ticket booking system.",
    lifespan=lifespan,
)

# ─── CORS ─────────────────────────────────────────────────────────────────────

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ─── Global Exception Handler ─────────────────────────────────────────────────

@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"detail": "An internal server error occurred", "error_code": "INTERNAL_ERROR"},
    )


# ─── Routers ──────────────────────────────────────────────────────────────────

API_PREFIX = "/api"

app.include_router(health_router, prefix=API_PREFIX)
app.include_router(auth_router, prefix=API_PREFIX)
app.include_router(users_router, prefix=API_PREFIX)
app.include_router(movies_router, prefix=API_PREFIX)
app.include_router(shows_router, prefix=API_PREFIX)
app.include_router(bookings_router, prefix=API_PREFIX)
app.include_router(snacks_router, prefix=API_PREFIX)
app.include_router(payments_router, prefix=API_PREFIX)
app.include_router(admin_router, prefix=API_PREFIX)


# ─── Root ─────────────────────────────────────────────────────────────────────

@app.get("/")
def root():
    return {
        "app": settings.app_name,
        "version": settings.app_version,
        "docs": "/docs",
        "health": "/api/health",
    }
