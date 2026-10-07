# CinemaApp — Single Theatre Booking System (Phase 1)

A brand-new Android cinema ticket booking application built from scratch with a Python FastAPI backend and SQLite database.

## Architecture

```text
Android App (Kotlin + Compose + Retrofit)
        │
        │ HTTPS REST API
        ▼
FastAPI Backend (Python + SQLAlchemy + JWT)
        │
        ▼
SQLite Database (cinema.db)
```

## Features (Phase 1 Foundation)
- **Single Theatre System**: Designed strictly for one theatre ("CinePlex Grand").
- **Shared Backend Database**: Centralized SQLite DB accessed via REST API.
- **Role-Based Auth**: USER and ADMIN roles using JWT tokens. Admin shares the same user booking flow.
- **Premium UI Foundation**: Cinematic dark theme with gold accents, custom typography, smooth navigation.
- **Robust Network State Handling**: Centralized `UiState` supporting `Loading`, `Success`, `Empty`, `Error`, and `Offline` states with retry logic.
- **Clean Architecture**: Separated layers for API, Repository, UseCase, ViewModel, and Composables.

## Backend Setup & Execution

```bash
cd backend
pip install -r requirements.txt
python run.py
```
- Server starts at `http://0.0.0.0:8000`
- API documentation available at `http://localhost:8000/docs`
- Health Check: `GET http://localhost:8000/api/health`

## Run Tests

```bash
cd backend
pytest tests/ -v
```

## Android Application Setup
- Open `android/CinemaApp` in Android Studio.
- Sync Gradle project.
- Configure `API_BASE_URL` in `app/build.gradle.kts` (defaults to `http://10.0.2.2:8000/` for Android Emulator).
- Build and run on device/emulator or assemble APK: `./gradlew assembleDebug`
