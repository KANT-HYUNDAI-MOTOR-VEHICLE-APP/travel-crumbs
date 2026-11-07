"""cd_dtl 읽기 — SQL 은 이 파일에만."""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.codes.models import CdDtl


async def find_by_group(session: AsyncSession, cd_grp_cd: str) -> list[CdDtl]:
    """사용 중인 코드만, 정렬순서대로."""
    stmt = (
        select(CdDtl)
        .where(
            CdDtl.cd_grp_cd == cd_grp_cd, CdDtl.use_yn == "Y", CdDtl.deleted_yn == "N"
        )
        .order_by(CdDtl.sort_ord)
    )
    return list((await session.execute(stmt)).scalars())
