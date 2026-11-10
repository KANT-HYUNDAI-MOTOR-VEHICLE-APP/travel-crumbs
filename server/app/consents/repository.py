"""user_consent_hist 읽기 · 쓰기 — SQL 은 이 파일에만."""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.consents.models import UserConsentHist


async def find_latest(
    session: AsyncSession, user_no: int, consent_type_cd: str
) -> UserConsentHist | None:
    """가장 최근 이력 1건 = 지금의 동의 상태."""
    stmt = (
        select(UserConsentHist)
        .where(
            UserConsentHist.user_no == user_no,
            UserConsentHist.consent_type_cd == consent_type_cd,
            UserConsentHist.deleted_yn == "N",
        )
        .order_by(UserConsentHist.user_consent_hist_no.desc())
        .limit(1)
    )
    return (await session.execute(stmt)).scalar_one_or_none()


async def add(
    session: AsyncSession, user_no: int, consent_type_cd: str, consent_yn: str
) -> UserConsentHist:
    row = UserConsentHist(
        user_no=user_no,
        consent_type_cd=consent_type_cd,
        consent_yn=consent_yn,
        created_user_no=user_no,
    )
    session.add(row)
    await session.flush()
    await session.refresh(row)
    return row
