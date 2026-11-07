"""GET /me — 홈(A-03) 「안녕하세요, [닉네임]님」 에 쓰는 내 정보."""

from fastapi import APIRouter, Depends

from app.users.models import UserMst
from app.users.schemas import MeResponse
from app.users.service import current_user

router = APIRouter(tags=["users"])


@router.get("/me", response_model=MeResponse)
async def read_me(user: UserMst = Depends(current_user)) -> MeResponse:
    return MeResponse(user_no=user.user_no, nickname_nm=user.nickname_nm)
