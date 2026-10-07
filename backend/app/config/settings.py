"""
Application configuration settings loaded from environment variables.
"""
from pydantic_settings import BaseSettings
from typing import List
from functools import lru_cache


class Settings(BaseSettings):
    # Application
    app_name: str = "RNPS Home Cinema API"
    app_version: str = "1.0.0"
    debug: bool = False

    # Server
    host: str = "0.0.0.0"
    port: int = 8000

    # Security
    secret_key: str = "cinema-app-secret-key-change-in-production"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 10080  # 7 days

    # Database
    database_url: str = "sqlite:///./database/cinema.db"

    # CORS
    allowed_origins: List[str] = [
        "http://localhost",
        "http://10.0.2.2",  # Android emulator localhost
        "http://127.0.0.1",
        "*",
    ]

    # Theatre Info (Single Theatre)
    theatre_name: str = "RNPS Home Cinema"
    theatre_address: str = "Main Boulevard, RNPS Complex"
    theatre_phone: str = "+1-555-0100"

    class Config:
        env_file = ".env"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    return Settings()
