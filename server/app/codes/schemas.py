"""코드 응답 모양 — 화면이 버튼 글자(cd_nm)를 서버에서 받아 씀."""

from pydantic import BaseModel, Field


class CodeResponse(BaseModel):
    cd: str = Field(description="코드")
    cd_nm: str = Field(description="코드명 — 화면에 보이는 글자")
