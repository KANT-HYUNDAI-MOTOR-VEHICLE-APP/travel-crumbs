"""trip_mst · trip_companion_dtl · trip_status_hist 읽기 · 쓰기 — SQL 은 이 파일에만."""

import uuid
from datetime import datetime
from decimal import Decimal

from sqlalchemy import func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.codes.models import CdDtl
from app.trips.models import (
    TripCompanionDtl,
    TripMst,
    TripPlaceDtl,
    TripRoutePointDtl,
    TripStatus,
    TripStatusHist,
    Visibility,
)


async def find_recording(session: AsyncSession, user_no: int) -> TripMst | None:
    """기록 중인 여행 — 한 사용자에 1건만 있도록 서비스가 지킴."""
    stmt = (
        select(TripMst)
        .where(
            TripMst.user_no == user_no,
            TripMst.trip_status_cd == TripStatus.RECORDING,
            TripMst.deleted_yn == "N",
        )
        .order_by(TripMst.trip_no.desc())
        .limit(1)
    )
    return (await session.execute(stmt)).scalar_one_or_none()


async def find_companion_cds(session: AsyncSession, trip_no: int) -> list[str]:
    stmt = (
        select(TripCompanionDtl.companion_type_cd)
        .where(TripCompanionDtl.trip_no == trip_no, TripCompanionDtl.deleted_yn == "N")
        .order_by(TripCompanionDtl.trip_companion_no)
    )
    return list((await session.execute(stmt)).scalars())


async def find_valid_codes(
    session: AsyncSession, cd_grp_cd: str, cds: list[str]
) -> set[str]:
    """cds 중 cd_dtl 에 있고 사용 중인 코드만."""
    stmt = select(CdDtl.cd).where(
        CdDtl.cd_grp_cd == cd_grp_cd,
        CdDtl.cd.in_(cds),
        CdDtl.use_yn == "Y",
        CdDtl.deleted_yn == "N",
    )
    return set((await session.execute(stmt)).scalars())


async def add_trip(session: AsyncSession, user_no: int, trip_nm: str) -> TripMst:
    """기록 중 상태로 바로 생성 — 시작일시는 DB 시각(created_at 과 같은 기준)."""
    trip = TripMst(
        trip_uuid=str(uuid.uuid4()),
        user_no=user_no,
        trip_nm=trip_nm,
        trip_status_cd=TripStatus.RECORDING,
        visibility_cd=Visibility.PRIVATE,
        start_at=func.now(),
        created_user_no=user_no,
    )
    session.add(trip)
    await session.flush()
    await session.refresh(trip)
    return trip


async def add_companions(
    session: AsyncSession, trip_no: int, user_no: int, cds: list[str]
) -> None:
    session.add_all(
        TripCompanionDtl(trip_no=trip_no, companion_type_cd=cd, created_user_no=user_no)
        for cd in cds
    )
    await session.flush()


async def add_status_hist(
    session: AsyncSession, trip_no: int, user_no: int, status_cd: str
) -> None:
    session.add(
        TripStatusHist(
            trip_no=trip_no, trip_status_cd=status_cd, created_user_no=user_no
        )
    )
    await session.flush()


def coord(value: float) -> Decimal:
    """위도 · 경도 → decimal(10,7)."""
    return round(Decimal(str(value)), 7)


async def find_by_uuid(
    session: AsyncSession, user_no: int, trip_uuid: str
) -> TripMst | None:
    """내 여행만 — 다른 사용자의 여행 UUID 면 없는 것으로."""
    stmt = select(TripMst).where(
        TripMst.trip_uuid == trip_uuid,
        TripMst.user_no == user_no,
        TripMst.deleted_yn == "N",
    )
    return (await session.execute(stmt)).scalar_one_or_none()


async def add_route_points(
    session: AsyncSession,
    trip_no: int,
    user_no: int,
    points: list[tuple[float, float, datetime]],
) -> int:
    session.add_all(
        TripRoutePointDtl(
            trip_no=trip_no,
            lat=coord(lat),
            lng=coord(lng),
            recorded_at=at,
            created_user_no=user_no,
        )
        for lat, lng, at in points
    )
    await session.flush()
    return len(points)


async def add_place(
    session: AsyncSession,
    trip_no: int,
    user_no: int,
    lat: float,
    lng: float,
    recorded_at: datetime,
) -> TripPlaceDtl:
    """임시 기록(draft_yn = Y) — 장소 연결 · 메모 · 후기는 정차 후(A-06)."""
    row = TripPlaceDtl(
        trip_no=trip_no,
        lat=coord(lat),
        lng=coord(lng),
        recorded_at=recorded_at,
        created_user_no=user_no,
    )
    session.add(row)
    await session.flush()
    await session.refresh(row)
    return row


async def increase_place_cnt(session: AsyncSession, trip_no: int) -> int:
    """반정규화 장소개수 +1 — 장소 저장과 같은 트랜잭션."""
    stmt = (
        update(TripMst)
        .where(TripMst.trip_no == trip_no)
        .values(place_cnt=TripMst.place_cnt + 1)
        .returning(TripMst.place_cnt)
    )
    return (await session.execute(stmt)).scalar_one()
