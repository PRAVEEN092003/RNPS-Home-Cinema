"""
Entry point to run the FastAPI development server.
Usage: python run.py
"""
import uvicorn
from app.config.settings import get_settings

settings = get_settings()

if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        reload=settings.debug,
        log_level="info",
    )
