"""위치정보 동의(A-03-1) — 최신 동의 조회 · 동의(또는 철회) 기록."""

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.consents import service
from app.consents.schemas import ConsentCreateRequest, ConsentLatestResponse, ConsentTypeCd
from app.db.session import get_session
from app.users.models import UserMst
from app.users.service import current_user

router = APIRouter(prefix="/consents", tags=["consents"])


@router.get("/latest", response_model=ConsentLatestResponse)
async def read_latest(
    consent_type_cd: ConsentTypeCd = Query(description="동의유형코드"),
    user: UserMst = Depends(current_user),
    session: AsyncSession = Depends(get_session),
) -> ConsentLatestResponse:
    return await service.latest(session, user.user_no, consent_type_cd)


@router.post("", response_model=ConsentLatestResponse, status_code=status.HTTP_201_CREATED)
async def create(
    req: ConsentCreateRequest,
    user: UserMst = Depends(current_user),
    session: AsyncSession = Depends(get_session),
) -> ConsentLatestResponse:
    return await service.record(session, user.user_no, req)
