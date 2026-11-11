"""user_mst 읽기 — SQL 은 이 파일에만."""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.users.models import UserMst


async def find_by_login_id(session: AsyncSession, login_id: str) -> UserMst | None:
    stmt = select(UserMst).where(UserMst.login_id == login_id, UserMst.deleted_yn == "N")
    return (await session.execute(stmt)).scalar_one_or_none()
