"""여행 시작 규칙(A-04).

- 기록 중인 여행이 있으면 새로 만들지 않음(409) — 종료(A-08)는 묶음 6
- 동행자: COMPANION_TYPE 코드만, 같은 코드는 한 번만, 「혼자」는 다른 동행자와 함께 고를 수 없음(가안 · 결정 로그 D12)
- 한 번에 저장: trip_mst 1행 + trip_companion_dtl n행 + trip_status_hist(RECORDING) 1행

여행 기록 중 규칙(A-05).

- 경로점 · 장소는 내 여행이고 기록 중(RECORDING)일 때만 저장(아니면 404 · 409)
- 장소는 위치 · 시각만 임시 저장(draft_yn = Y) + trip_mst.place_cnt +1
"""

from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.trips import repository
from app.trips.models import TripMst, TripStatus
from app.trips.schemas import (
    CurrentTripResponse,
    RoutePointsCreateRequest,
    RoutePointsCreateResponse,
    TripCreateRequest,
    TripPlaceCreateRequest,
    TripPlaceResponse,
    TripResponse,
)

COMPANION_TYPE = "COMPANION_TYPE"
SOLO = "SOLO"


async def current(session: AsyncSession, user_no: int) -> CurrentTripResponse:
    trip = await repository.find_recording(session, user_no)
    return CurrentTripResponse(trip=await _to_response(session, trip) if trip else None)


async def start(
    session: AsyncSession, user_no: int, req: TripCreateRequest
) -> TripResponse:
    if await repository.find_recording(session, user_no) is not None:
        raise HTTPException(status.HTTP_409_CONFLICT, detail="TRIP_ALREADY_RECORDING")

    cds = list(dict.fromkeys(req.companion_type_cds))  # 순서 유지 · 중복 제거
    if await repository.find_valid_codes(session, COMPANION_TYPE, cds) != set(cds):
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY, detail="INVALID_COMPANION_TYPE"
        )
    if SOLO in cds and len(cds) > 1:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY, detail="SOLO_WITH_COMPANION"
        )

    trip = await repository.add_trip(session, user_no, req.trip_nm)
    await repository.add_companions(session, trip.trip_no, user_no, cds)
    await repository.add_status_hist(
        session, trip.trip_no, user_no, TripStatus.RECORDING
    )
    await session.commit()
    return await _to_response(session, trip)


async def add_route_points(
    session: AsyncSession, user_no: int, trip_uuid: str, req: RoutePointsCreateRequest
) -> RoutePointsCreateResponse:
    trip = await _recording_trip(session, user_no, trip_uuid)
    points = [(p.lat, p.lng, p.recorded_at) for p in req.points]
    saved = await repository.add_route_points(session, trip.trip_no, user_no, points)
    await session.commit()
    return RoutePointsCreateResponse(saved_cnt=saved)


async def record_place(
    session: AsyncSession, user_no: int, trip_uuid: str, req: TripPlaceCreateRequest
) -> TripPlaceResponse:
    trip = await _recording_trip(session, user_no, trip_uuid)
    row = await repository.add_place(
        session, trip.trip_no, user_no, req.lat, req.lng, req.recorded_at
    )
    place_cnt = await repository.increase_place_cnt(session, trip.trip_no)
    await session.commit()
    return TripPlaceResponse(
        trip_place_no=row.trip_place_no,
        lat=float(row.lat),
        lng=float(row.lng),
        recorded_at=row.recorded_at,
        draft_yn=row.draft_yn,
        place_cnt=place_cnt,
    )


async def _recording_trip(
    session: AsyncSession, user_no: int, trip_uuid: str
) -> TripMst:
    trip = await repository.find_by_uuid(session, user_no, trip_uuid)
    if trip is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="TRIP_NOT_FOUND")
    if trip.trip_status_cd != TripStatus.RECORDING:
        raise HTTPException(status.HTTP_409_CONFLICT, detail="TRIP_NOT_RECORDING")
    return trip


async def _to_response(session: AsyncSession, trip: TripMst) -> TripResponse:
    return TripResponse(
        trip_no=trip.trip_no,
        trip_uuid=trip.trip_uuid,
        trip_nm=trip.trip_nm,
        trip_status_cd=trip.trip_status_cd,
        visibility_cd=trip.visibility_cd,
        start_at=trip.start_at,
        place_cnt=trip.place_cnt,
        companion_type_cds=await repository.find_companion_cds(session, trip.trip_no),
    )
