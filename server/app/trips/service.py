"""여행 시작 규칙(A-04).

- 기록 중인 여행이 있으면 새로 만들지 않음(409) — 종료(A-08)는 묶음 6
- 동행자: COMPANION_TYPE 코드만, 같은 코드는 한 번만, 「혼자」는 다른 동행자와 함께 고를 수 없음(가안 · 결정 로그 D12)
- 한 번에 저장: trip_mst 1행 + trip_companion_dtl n행 + trip_status_hist(RECORDING) 1행

여행 기록 중 규칙(A-05).

- 경로점은 기록 중(RECORDING)일 때만 · 장소는 기록 중 또는 일시정지일 때 저장(아니면 404 · 409)
- 일시정지(PAUSED) ↔ 기록 중 전환마다 상태 이력 1행
- 장소는 위치 · 시각만 임시 저장(draft_yn = Y) + trip_mst.place_cnt +1

장소 경험 입력 규칙(A-06).

- 내 여행이고 취소되지 않았으면 입력 가능(여행 중 · 종료 뒤 모두)
- 「저장」은 임시 기록을 완성(draft_yn = N) — 사용자장소명 필수, 유형 · 후기 · 메모 · 태그는 선택
- 내비 후보를 골랐으면 같은 POI 의 공식 장소(place_mst)에 연결, 없으면 새로 만듦(결정 로그 D19 · D20)
"""

from datetime import UTC, datetime

from fastapi import HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.history import repository as history_repository
from app.places import repository as place_repository
from app.trips import repository
from app.trips import route as route_calc
from app.trips.models import TripMst, TripPlaceDtl, TripStatus
from app.trips.schemas import (
    CurrentTripResponse,
    RoutePointsCreateRequest,
    RoutePointsCreateResponse,
    TimelinePlace,
    TripCreateRequest,
    TripDetailResponse,
    TripListItem,
    TripPlaceCreateRequest,
    TripPlaceItem,
    TripPlaceResponse,
    TripPlaceUpdateRequest,
    TripResponse,
    TripUpdateRequest,
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
    trip = await _active_trip(session, user_no, trip_uuid)
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


async def list_places(
    session: AsyncSession, user_no: int, trip_uuid: str, draft_yn: str | None
) -> list[TripPlaceItem]:
    trip = await _my_trip(session, user_no, trip_uuid)
    rows = await repository.find_places(session, trip.trip_no, draft_yn)
    tags = await repository.find_tag_nos(session, [r.trip_place_no for r in rows])
    return [_to_place_item(r, tags[r.trip_place_no]) for r in rows]


async def complete_place(
    session: AsyncSession,
    user_no: int,
    trip_uuid: str,
    trip_place_no: int,
    req: TripPlaceUpdateRequest,
) -> TripPlaceItem:
    trip = await _my_trip(session, user_no, trip_uuid)
    if trip.trip_status_cd == TripStatus.CANCELED:
        raise HTTPException(status.HTTP_409_CONFLICT, detail="TRIP_CANCELED")
    row = await repository.find_place(session, trip.trip_no, trip_place_no)
    if row is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="TRIP_PLACE_NOT_FOUND")

    if req.place_type_cd is not None and not await repository.is_valid_place_type(
        session, req.place_type_cd
    ):
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY, detail="INVALID_PLACE_TYPE"
        )
    cd = req.review_polarity_cd
    if cd is not None and await repository.find_valid_codes(
        session, "REVIEW_POLARITY", [cd]
    ) != {cd}:
        raise HTTPException(
            status.HTTP_422_UNPROCESSABLE_ENTITY, detail="INVALID_REVIEW_POLARITY"
        )
    tag_nos = list(dict.fromkeys(req.tag_nos))
    if tag_nos and await place_repository.find_valid_tag_nos(session, tag_nos) != set(
        tag_nos
    ):
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, detail="INVALID_TAG")

    if req.navi_place is not None:
        place = await place_repository.find_or_add_navi_place(
            session,
            user_no,
            place_nm=req.navi_place.place_nm,
            lat=repository.coord(req.navi_place.lat),
            lng=repository.coord(req.navi_place.lng),
            poi_id=req.navi_place.poi_id,
            poi_provider=req.navi_place.poi_provider,
        )
        row.place_no = place.place_no
    row.user_place_nm = req.user_place_nm
    row.place_type_cd = req.place_type_cd
    row.review_polarity_cd = req.review_polarity_cd
    row.memo = req.memo or None
    row.draft_yn = "N"
    row.updated_at = datetime.now(UTC)
    row.updated_user_no = user_no
    await repository.replace_tags(session, row.trip_place_no, user_no, tag_nos)
    await session.commit()
    await session.refresh(row)
    return _to_place_item(row, tag_nos)


async def end_trip(
    session: AsyncSession, user_no: int, trip_uuid: str
) -> TripDetailResponse:
    """여행 종료(A-08) — 완료 · 종료 시각 · 거리 · 소요시간 · 간소화 경로 · 상태 이력을 한 번에.

    입력 안 한 임시 기록(draft_yn = Y)은 지우지 않고 남김(나중에 입력 가능).
    소요시간 = 기록한 시간(일시정지 제외 · 결정 로그 D27). 전체 경과는 end_at - start_at.
    """
    trip = await _active_trip(session, user_no, trip_uuid)
    coords = await repository.find_route_coords(session, trip.trip_no)
    now = datetime.now(UTC)
    trip.trip_status_cd = TripStatus.COMPLETED
    trip.end_at = now
    trip.dist = route_calc.total_distance_m(coords)
    trip.duration = recorded_seconds(
        await repository.find_status_hist(session, trip.trip_no), now
    )
    trip.updated_at = now
    trip.updated_user_no = user_no
    if coords:
        await repository.save_route(
            session,
            trip.trip_no,
            user_no,
            route_calc.encode_polyline(route_calc.simplify(coords)),
        )
    await repository.add_status_hist(
        session, trip.trip_no, user_no, TripStatus.COMPLETED
    )
    await session.commit()
    return await detail(session, user_no, trip_uuid)


async def detail(
    session: AsyncSession, user_no: int, trip_uuid: str
) -> TripDetailResponse:
    trip = await _my_trip(session, user_no, trip_uuid)
    rows = await repository.find_places(session, trip.trip_no, None)
    tag_names, type_names = await repository.find_timeline_extras(
        session,
        [r.trip_place_no for r in rows],
        [r.place_type_cd for r in rows if r.place_type_cd],
    )
    return TripDetailResponse(
        trip_no=trip.trip_no,
        trip_uuid=trip.trip_uuid,
        trip_nm=trip.trip_nm,
        trip_status_cd=trip.trip_status_cd,
        start_at=trip.start_at,
        end_at=trip.end_at,
        dist=trip.dist,
        duration=trip.duration,
        place_cnt=trip.place_cnt,
        route_data=await repository.find_route_data(session, trip.trip_no),
        places=[
            TimelinePlace(
                trip_place_no=r.trip_place_no,
                recorded_at=r.recorded_at,
                draft_yn=r.draft_yn,
                user_place_nm=r.user_place_nm,
                place_type_nm=type_names.get(r.place_type_cd)
                if r.place_type_cd
                else None,
                review_polarity_cd=r.review_polarity_cd,
                memo=r.memo,
                tag_nms=tag_names[r.trip_place_no],
            )
            for r in rows
        ],
    )


async def rename(
    session: AsyncSession, user_no: int, trip_uuid: str, req: TripUpdateRequest
) -> TripDetailResponse:
    """「제목 수정」 — 바뀌면 trip_nm 수정 + 데이터변경이력 1행(결정 로그 D17 · D26)."""
    trip = await _my_trip(session, user_no, trip_uuid)
    if trip.trip_nm != req.trip_nm:
        await history_repository.add_change(
            session,
            user_no,
            table_nm="trip_mst",
            column_nm="trip_nm",
            row_no=trip.trip_no,
            before=trip.trip_nm,
            after=req.trip_nm,
        )
        trip.trip_nm = req.trip_nm
        trip.updated_at = datetime.now(UTC)
        trip.updated_user_no = user_no
        await session.commit()
    return await detail(session, user_no, trip_uuid)


async def my_trips(session: AsyncSession, user_no: int) -> list[TripListItem]:
    """여행 목록(A-07 간단판) — 취소한 여행은 뺌."""
    return [
        TripListItem(
            trip_uuid=t.trip_uuid,
            trip_nm=t.trip_nm,
            trip_status_cd=t.trip_status_cd,
            start_at=t.start_at,
            dist=t.dist,
            place_cnt=t.place_cnt,
        )
        for t in await repository.find_my_trips(session, user_no)
    ]


async def _my_trip(session: AsyncSession, user_no: int, trip_uuid: str) -> TripMst:
    trip = await repository.find_by_uuid(session, user_no, trip_uuid)
    if trip is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="TRIP_NOT_FOUND")
    return trip


def _to_place_item(row: TripPlaceDtl, tag_nos: list[int]) -> TripPlaceItem:
    return TripPlaceItem(
        trip_place_no=row.trip_place_no,
        lat=float(row.lat),
        lng=float(row.lng),
        recorded_at=row.recorded_at,
        draft_yn=row.draft_yn,
        place_no=row.place_no,
        user_place_nm=row.user_place_nm,
        place_type_cd=row.place_type_cd,
        review_polarity_cd=row.review_polarity_cd,
        memo=row.memo,
        tag_nos=tag_nos,
    )


async def pause(session: AsyncSession, user_no: int, trip_uuid: str) -> TripResponse:
    """일시정지 — 기록 중 → 일시정지 + 상태 이력. 이미 일시정지면 그대로."""
    trip = await _active_trip(session, user_no, trip_uuid)
    if trip.trip_status_cd == TripStatus.RECORDING:
        await _change_status(session, trip, user_no, TripStatus.PAUSED)
    return await _to_response(session, trip)


async def resume(session: AsyncSession, user_no: int, trip_uuid: str) -> TripResponse:
    """다시 기록 — 일시정지 → 기록 중 + 상태 이력. 이미 기록 중이면 그대로."""
    trip = await _active_trip(session, user_no, trip_uuid)
    if trip.trip_status_cd == TripStatus.PAUSED:
        await _change_status(session, trip, user_no, TripStatus.RECORDING)
    return await _to_response(session, trip)


def recorded_seconds(history: list[tuple[str, datetime]], until: datetime) -> int:
    """기록 중(RECORDING)이었던 구간의 합(초) — 일시정지 구간은 뺌."""
    total = 0.0
    started: datetime | None = None
    for status_cd, at in history:
        if status_cd == TripStatus.RECORDING:
            started = started or at
        elif started is not None:
            total += (at - started).total_seconds()
            started = None
    if started is not None:
        total += (until - started).total_seconds()
    return int(total)


async def _change_status(
    session: AsyncSession, trip: TripMst, user_no: int, status_cd: str
) -> None:
    trip.trip_status_cd = status_cd
    trip.updated_at = datetime.now(UTC)
    trip.updated_user_no = user_no
    await repository.add_status_hist(session, trip.trip_no, user_no, status_cd)
    await session.commit()
    await session.refresh(trip)


async def _active_trip(session: AsyncSession, user_no: int, trip_uuid: str) -> TripMst:
    """진행 중(기록 중 또는 일시정지)인 내 여행."""
    trip = await _my_trip(session, user_no, trip_uuid)
    if trip.trip_status_cd not in (TripStatus.RECORDING, TripStatus.PAUSED):
        raise HTTPException(status.HTTP_409_CONFLICT, detail="TRIP_NOT_RECORDING")
    return trip


async def _recording_trip(
    session: AsyncSession, user_no: int, trip_uuid: str
) -> TripMst:
    trip = await _my_trip(session, user_no, trip_uuid)
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
