"""여행 시작 규칙(A-04).

- 기록 중인 여행이 있으면 새로 만들지 않음(409) — 종료(A-08)는 묶음 6
- 동행자: COMPANION_TYPE 코드만, 같은 코드는 한 번만, 「혼자」는 다른 동행자와 함께 고를 수 없음(가안 · 결정 로그 D12)
- 한 번에 저장: trip_mst 1행 + trip_companion_dtl n행 + trip_status_hist(RECORDING) 1행
"""

from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.trips import repository
from app.trips.models import TripMst, TripStatus
from app.trips.schemas import CurrentTripResponse, TripCreateRequest, TripResponse

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


async def _to_response(session: AsyncSession, trip: TripMst) -> TripResponse:
    return TripResponse(
        trip_no=trip.trip_no,
        trip_uuid=trip.trip_uuid,
        trip_nm=trip.trip_nm,
        trip_status_cd=trip.trip_status_cd,
        visibility_cd=trip.visibility_cd,
        start_at=trip.start_at,
        companion_type_cds=await repository.find_companion_cds(session, trip.trip_no),
    )
