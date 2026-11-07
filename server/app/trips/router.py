"""여행 — 기록 중인 여행 조회 · 여행 시작(A-04) · 경로점 · 장소 기록(A-05)."""

from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_session
from app.trips import service
from app.trips.schemas import (
    CurrentTripResponse,
    RoutePointsCreateRequest,
    RoutePointsCreateResponse,
    TripCreateRequest,
    TripPlaceCreateRequest,
    TripPlaceResponse,
    TripResponse,
)
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


@router.post(
    "/{trip_uuid}/route-points",
    response_model=RoutePointsCreateResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_route_points(
    trip_uuid: str,
    req: RoutePointsCreateRequest,
    user: UserMst = Depends(current_user),
    session: AsyncSession = Depends(get_session),
) -> RoutePointsCreateResponse:
    return await service.add_route_points(session, user.user_no, trip_uuid, req)


@router.post(
    "/{trip_uuid}/places",
    response_model=TripPlaceResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_place(
    trip_uuid: str,
    req: TripPlaceCreateRequest,
    user: UserMst = Depends(current_user),
    session: AsyncSession = Depends(get_session),
) -> TripPlaceResponse:
    return await service.record_place(session, user.user_no, trip_uuid, req)
