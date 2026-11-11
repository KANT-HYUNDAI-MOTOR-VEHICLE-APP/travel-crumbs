"""동의 규칙 — 같은 값이면 이력을 또 쌓지 않음(동의 상태에서 또 동의 → 그대로)."""

from sqlalchemy.ext.asyncio import AsyncSession

from app.consents import repository
from app.consents.schemas import ConsentCreateRequest, ConsentLatestResponse


async def latest(session: AsyncSession, user_no: int, consent_type_cd: str) -> ConsentLatestResponse:
    row = await repository.find_latest(session, user_no, consent_type_cd)
    return ConsentLatestResponse(
        consent_type_cd=consent_type_cd,  # type: ignore[arg-type]
        consent_yn=row.consent_yn if row else None,  # type: ignore[arg-type]
        created_at=row.created_at if row else None,
    )


async def record(
    session: AsyncSession, user_no: int, req: ConsentCreateRequest
) -> ConsentLatestResponse:
    current = await repository.find_latest(session, user_no, req.consent_type_cd)
    if current is None or current.consent_yn != req.consent_yn:
        await repository.add(session, user_no, req.consent_type_cd, req.consent_yn)
        await session.commit()
    return await latest(session, user_no, req.consent_type_cd)
