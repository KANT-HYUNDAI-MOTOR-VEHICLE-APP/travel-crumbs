"""동의 요청 · 응답 모양 — JSON 키는 표준 컬럼명 그대로."""

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field

# 표준정의서 v0.35 코드 CONSENT_TYPE — 코드 테이블(cd_dtl)이 생기면 그쪽 값으로 검사
ConsentTypeCd = Literal["LOCATION_COLLECT", "RECO_PUBLIC"]


class ConsentCreateRequest(BaseModel):
    consent_type_cd: ConsentTypeCd = Field(description="동의유형코드")
    consent_yn: Literal["Y", "N"] = Field(description="동의여부 — Y 동의 / N 철회")


class ConsentLatestResponse(BaseModel):
    consent_type_cd: ConsentTypeCd = Field(description="동의유형코드")
    consent_yn: Literal["Y", "N"] | None = Field(description="최신 동의여부 — 이력이 없으면 null")
    created_at: datetime | None = Field(description="최신 이력의 생성일시")
