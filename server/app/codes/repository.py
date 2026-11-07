"""cd_mst 읽기 — SQL 은 이 파일에만."""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.codes.models import CdMst


async def find_by_group(session: AsyncSession, cd_grp_nm: str) -> list[CdMst]:
    """사용 중인 코드만, 정렬순서대로."""
    stmt = (
        select(CdMst)
        .where(
            CdMst.cd_grp_nm == cd_grp_nm, CdMst.use_yn == "Y", CdMst.deleted_yn == "N"
        )
        .order_by(CdMst.sort_ord)
    )
    return list((await session.execute(stmt)).scalars())


async def find_valid_codes(
    session: AsyncSession, cd_grp_nm: str, cds: list[str]
) -> set[str]:
    """cds 중 cd_mst 에 있고 사용 중인 코드만."""
    stmt = select(CdMst.cd).where(
        CdMst.cd_grp_nm == cd_grp_nm,
        CdMst.cd.in_(cds),
        CdMst.use_yn == "Y",
        CdMst.deleted_yn == "N",
    )
    return set((await session.execute(stmt)).scalars())
