"""화면으로 보내는 모양 — JSON 키는 표준 컬럼명 그대로(화면 ↔ 컬럼 대조가 쉽게)."""

from pydantic import BaseModel, Field


class MeResponse(BaseModel):
    user_no: int = Field(description="사용자번호")
    nickname_nm: str = Field(max_length=100, description="닉네임명")
