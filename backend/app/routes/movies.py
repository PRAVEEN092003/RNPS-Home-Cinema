"""
Movie listing routes. Read-only for users.
"""
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.config.database import get_db
from app.models.models import Movie
from app.schemas.schemas import MovieResponse
from app.utils.auth import get_current_user

router = APIRouter(prefix="/movies", tags=["Movies"])


@router.get("/", response_model=List[MovieResponse])
def list_movies(
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    """List all currently active movies."""
    movies = db.query(Movie).filter(Movie.is_active == True).all()
    return [MovieResponse.model_validate(m) for m in movies]


@router.get("/{movie_id}", response_model=MovieResponse)
def get_movie(
    movie_id: int,
    db: Session = Depends(get_db),
    current_user=Depends(get_current_user),
):
    """Get details of a specific movie."""
    movie = db.query(Movie).filter(Movie.id == movie_id, Movie.is_active == True).first()
    if not movie:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Movie not found",
        )
    return MovieResponse.model_validate(movie)
