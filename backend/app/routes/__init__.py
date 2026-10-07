from app.routes.health import router as health_router
from app.routes.auth import router as auth_router
from app.routes.users import router as users_router
from app.routes.movies import router as movies_router
from app.routes.shows import router as shows_router
from app.routes.bookings import router as bookings_router
from app.routes.snacks import router as snacks_router
from app.routes.payments import router as payments_router
from app.routes.admin import router as admin_router

__all__ = [
    "health_router",
    "auth_router",
    "users_router",
    "movies_router",
    "shows_router",
    "bookings_router",
    "snacks_router",
    "payments_router",
    "admin_router",
]
