"""여행 — 기록 중인 여행 조회 · 여행 시작(A-04)."""

from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_session
from app.trips import service
from app.trips.schemas import CurrentTripResponse, TripCreateRequest, TripResponse
from app.users.models import UserMst
from app.users.service import current_user

router = APIRouter(prefix="/trips", tags=["trips"])


@router.get("/current", response_model=CurrentTripResponse)
async def read_current(
    user: UserMst = Depends(current_user),
    session: AsyncSession = Depends(get_session),
) -> CurrentTripResponse:
    return await service.current(session, user.user_no)


@router.post("", response_model=TripResponse, status_code=status.HTTP_201_CREATED)
async def create(
    req: TripCreateRequest,
    user: UserMst = Depends(current_user),
    session: AsyncSession = Depends(get_session),
) -> TripResponse:
    return await service.start(session, user.user_no, req)
