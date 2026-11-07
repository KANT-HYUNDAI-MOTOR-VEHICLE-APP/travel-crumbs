"""여행 요청 · 응답 모양 — JSON 키는 표준 컬럼명 그대로."""

from datetime import datetime
from typing import Annotated

from pydantic import BaseModel, Field, StringConstraints

TripName = Annotated[
    str, StringConstraints(strip_whitespace=True, min_length=1, max_length=100)
]


class TripCreateRequest(BaseModel):
    """여행 시작 설정(A-04) 「여행 시작」."""

    trip_nm: TripName = Field(
        description="여행명 — 앱이 날짜로 만든 기본값, 사용자가 고칠 수 있음"
    )
    companion_type_cds: list[str] = Field(
        min_length=1, description="동행자유형코드 목록 (COMPANION_TYPE)"
    )


class TripResponse(BaseModel):
    trip_no: int = Field(description="여행번호")
    trip_uuid: str = Field(description="여행UUID")
    trip_nm: str = Field(description="여행명")
    trip_status_cd: str = Field(description="여행상태코드")
    visibility_cd: str = Field(description="공개범위코드")
    start_at: datetime = Field(description="시작일시")
    companion_type_cds: list[str] = Field(description="동행자유형코드 목록")


class CurrentTripResponse(BaseModel):
    """기록 중인 여행 — 없으면 trip 이 null(앱이 빈 응답을 다루기 쉽게 한 겹 감쌈)."""

    trip: TripResponse | None = Field(
        description="기록 중(RECORDING)인 여행, 없으면 null"
    )
