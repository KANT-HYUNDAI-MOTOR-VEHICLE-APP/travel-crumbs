"""trip_mst · trip_companion_dtl · trip_status_hist 읽기 · 쓰기 — SQL 은 이 파일에만."""

import uuid

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.codes.models import CdDtl
from app.trips.models import (
    TripCompanionDtl,
    TripMst,
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
