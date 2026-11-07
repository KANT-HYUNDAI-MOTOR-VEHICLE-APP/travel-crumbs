"""여행 — 기록 중인 여행 조회 · 여행 시작(A-04) · 경로점 · 장소 기록(A-05) · 장소 경험 입력(A-06)."""

from typing import Literal

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_session
from app.trips import service
from app.trips.schemas import (
    CurrentTripResponse,
    RoutePointsCreateRequest,
    RoutePointsCreateResponse,
    TripCreateRequest,
    TripPlaceCreateRequest,
    TripPlaceItem,
    TripPlaceResponse,
    TripPlaceUpdateRequest,
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


@router.get("/{trip_uuid}/places", response_model=list[TripPlaceItem])
async def read_places(
    trip_uuid: str,
    draft_yn: Literal["Y", "N"] | None = Query(
        default=None, description="Y = 입력 안 한 임시 기록만"
    ),
    user: UserMst = Depends(current_user),
    session: AsyncSession = Depends(get_session),
) -> list[TripPlaceItem]:
    return await service.list_places(session, user.user_no, trip_uuid, draft_yn)


@router.put("/{trip_uuid}/places/{trip_place_no}", response_model=TripPlaceItem)
async def update_place(
    trip_uuid: str,
    trip_place_no: int,
    req: TripPlaceUpdateRequest,
    user: UserMst = Depends(current_user),
    session: AsyncSession = Depends(get_session),
) -> TripPlaceItem:
    return await service.complete_place(
        session, user.user_no, trip_uuid, trip_place_no, req
    )
