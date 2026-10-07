"""
User profile routes.
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from app.config.database import get_db
from app.models.models import User
from app.schemas.schemas import UserResponse, UserUpdateRequest
from app.utils.auth import get_current_user

router = APIRouter(prefix="/users", tags=["Users"])


@router.get("/me", response_model=UserResponse)
def get_profile(current_user: User = Depends(get_current_user)):
    """Get current user profile."""
    return UserResponse.model_validate(current_user)


@router.patch("/me", response_model=UserResponse)
def update_profile(
    request: UserUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Update current user's name or phone."""
    if request.name is not None:
        current_user.name = request.name
    if request.phone is not None:
        current_user.phone = request.phone
    db.commit()
    db.refresh(current_user)
    return UserResponse.model_validate(current_user)
