"""공통 코드 조회 — 예: GET /codes/COMPANION_TYPE (여행 시작 설정 A-04 동행자 버튼)."""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.codes import repository
from app.codes.schemas import CodeResponse
from app.db.session import get_session

router = APIRouter(prefix="/codes", tags=["codes"])


@router.get("/{cd_grp_cd}", response_model=list[CodeResponse])
async def read_group(
    cd_grp_cd: str, session: AsyncSession = Depends(get_session)
) -> list[CodeResponse]:
    rows = await repository.find_by_group(session, cd_grp_cd)
    if not rows:
        raise HTTPException(status.HTTP_404_NOT_FOUND, detail="CODE_GROUP_NOT_FOUND")
    return [CodeResponse(cd=r.cd, cd_nm=r.cd_nm) for r in rows]
