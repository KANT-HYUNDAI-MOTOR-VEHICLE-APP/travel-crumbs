"""여행 요청 · 응답 모양 — JSON 키는 표준 컬럼명 그대로."""

from datetime import datetime
from typing import Annotated

from pydantic import AwareDatetime, BaseModel, Field, StringConstraints

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
    place_cnt: int = Field(
        description="장소개수 — 기록한 장소 수(A-05 「기록된 장소 N」)"
    )
    companion_type_cds: list[str] = Field(description="동행자유형코드 목록")


class CurrentTripResponse(BaseModel):
    """기록 중인 여행 — 없으면 trip 이 null(앱이 빈 응답을 다루기 쉽게 한 겹 감쌈)."""

    trip: TripResponse | None = Field(
        description="기록 중(RECORDING)인 여행, 없으면 null"
    )


Lat = Annotated[float, Field(ge=-90, le=90, description="위도")]
Lng = Annotated[float, Field(ge=-180, le=180, description="경도")]


class RoutePoint(BaseModel):
    lat: Lat
    lng: Lng
    recorded_at: AwareDatetime = Field(
        description="기록일시 — 앱이 좌표를 받은 시각(시간대 포함)"
    )


class RoutePointsCreateRequest(BaseModel):
    """여행 기록 중(A-05) 경로점 묶음 — 앱이 모았다가 한 번에 보냄."""

    points: list[RoutePoint] = Field(
        min_length=1, max_length=500, description="경로점 목록(시간 순)"
    )


class RoutePointsCreateResponse(BaseModel):
    saved_cnt: int = Field(description="저장한 경로점 수")


class TripPlaceCreateRequest(BaseModel):
    """「이 장소 기록」(A-05) — 위치 · 시각만 임시 저장."""

    lat: Lat
    lng: Lng
    recorded_at: AwareDatetime = Field(
        description="기록일시 — 사용자가 버튼을 누른 시각(시간대 포함)"
    )


class TripPlaceResponse(BaseModel):
    trip_place_no: int = Field(description="여행장소번호")
    lat: float = Field(description="위도")
    lng: float = Field(description="경도")
    recorded_at: datetime = Field(description="기록일시")
    draft_yn: str = Field(description="임시저장여부 — 상세 입력 전이면 Y")
    place_cnt: int = Field(description="이 여행의 장소개수(저장 후)")


class TripPlaceItem(BaseModel):
    """여행에 기록한 장소 1건 — 임시(draft_yn = Y)면 장소명 · 유형 · 후기가 비어 있음."""

    trip_place_no: int = Field(description="여행장소번호")
    lat: float = Field(description="위도")
    lng: float = Field(description="경도")
    recorded_at: datetime = Field(description="기록일시")
    draft_yn: str = Field(description="임시저장여부")
    place_no: int | None = Field(description="장소번호 — 공식 장소에 연결됐으면 값")
    user_place_nm: str | None = Field(description="사용자장소명")
    place_type_cd: str | None = Field(description="장소유형코드 (PLACE_TYPE)")
    review_polarity_cd: str | None = Field(description="후기성격코드 (REVIEW_POLARITY)")
    memo: str | None = Field(description="메모")
    tag_nos: list[int] = Field(description="경험 태그 번호 목록")


class NaviPlaceIn(BaseModel):
    """내비 후보(목적지 · 경유지 · 최근 목적지 · 즐겨찾기)에서 고른 장소."""

    place_nm: Annotated[
        str, StringConstraints(strip_whitespace=True, min_length=1, max_length=100)
    ]
    lat: Lat
    lng: Lng
    poi_id: Annotated[str, StringConstraints(min_length=1, max_length=50)]
    poi_provider: Annotated[str, StringConstraints(min_length=1, max_length=30)]


class TripPlaceUpdateRequest(BaseModel):
    """장소 경험 입력(A-06) 「저장」 — 임시 기록을 완성(draft_yn = N)."""

    user_place_nm: Annotated[
        str, StringConstraints(strip_whitespace=True, min_length=1, max_length=100)
    ] = Field(description="사용자장소명")
    place_type_cd: str | None = Field(
        default=None, description="장소유형코드 (PLACE_TYPE)"
    )
    review_polarity_cd: str | None = Field(
        default=None, description="후기성격코드 (POSITIVE · NEGATIVE)"
    )
    memo: (
        Annotated[str, StringConstraints(strip_whitespace=True, max_length=500)] | None
    ) = Field(default=None, description="메모")
    tag_nos: list[int] = Field(
        default_factory=list, max_length=20, description="경험 태그 번호 목록"
    )
    navi_place: NaviPlaceIn | None = Field(
        default=None, description="내비 후보에서 골랐으면 그 장소 — 공식 장소로 연결"
    )


class TripUpdateRequest(BaseModel):
    """여행 상세(A-08) 「제목 수정」."""

    trip_nm: TripName = Field(description="여행명")


class TimelinePlace(BaseModel):
    """여행 상세 타임라인 한 줄 — 기록 순서(recorded_at)."""

    trip_place_no: int = Field(description="여행장소번호")
    recorded_at: datetime = Field(description="기록일시")
    draft_yn: str = Field(description="임시저장여부 — Y 면 아직 입력 안 함")
    user_place_nm: str | None = Field(description="사용자장소명")
    place_type_nm: str | None = Field(description="장소유형명")
    review_polarity_cd: str | None = Field(description="후기성격코드")
    memo: str | None = Field(description="메모")
    tag_nms: list[str] = Field(description="경험 태그 이름")


class TripDetailResponse(BaseModel):
    """여행 상세(A-08) — 요약 · 간소화 경로 · 장소 타임라인."""

    trip_no: int = Field(description="여행번호")
    trip_uuid: str = Field(description="여행UUID")
    trip_nm: str = Field(description="여행명")
    trip_status_cd: str = Field(description="여행상태코드")
    start_at: datetime = Field(description="시작일시")
    end_at: datetime | None = Field(description="종료일시")
    dist: int | None = Field(description="거리(m)")
    duration: int | None = Field(description="소요시간(초)")
    place_cnt: int = Field(description="장소개수")
    route_data: str | None = Field(
        description="간소화 경로(인코딩 폴리라인) — 종료 전이면 null"
    )
    places: list[TimelinePlace] = Field(description="장소 타임라인")
