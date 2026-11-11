"""로그인된 사용자 찾기 — 지금은 로그인 대역(샘플 계정). Pleos 로그인이 붙으면 이 함수만 바뀜."""

from fastapi import Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.settings import settings
from app.db.session import get_session
from app.users import repository
from app.users.models import UserMst


async def current_user(session: AsyncSession = Depends(get_session)) -> UserMst:
    user = await repository.find_by_login_id(session, settings.dev_login_id)
    if user is None:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, detail="USER_NOT_FOUND")
    return user
