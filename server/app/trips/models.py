"""trip_mst 여행마스터 · trip_companion_dtl 여행동행자내역 · trip_status_hist 여행상태이력 — ERD v0.3 그대로."""

from datetime import datetime
from decimal import Decimal

from sqlalchemy import (
    CHAR,
    BigInteger,
    CheckConstraint,
    DateTime,
    ForeignKey,
    Identity,
    Integer,
    Numeric,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, CommonColumns, table_args


class TripStatus:
    """TRIP_STATUS 코드 — 앱 로직과 묶인 상태 코드는 상수로도 둠(표준정의서 R10)."""

    RECORDING = "RECORDING"
    COMPLETED = "COMPLETED"
    INTERRUPTED = "INTERRUPTED"
    CANCELED = "CANCELED"


class Visibility:
    """VISIBILITY 코드 — 새 여행은 나만 보기로 시작(결정 로그 D13)."""

    PRIVATE = "PRIVATE"


class TripMst(CommonColumns, Base):
    __tablename__ = "trip_mst"
    __table_args__ = table_args("여행마스터 — 기록 시작~종료 한 번의 이동")

    trip_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="여행번호"
    )
    trip_uuid: Mapped[str] = mapped_column(
        CHAR(36), unique=True, comment="여행UUID — 외부 노출 · 동기화"
    )
    user_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("user_mst.user_no"), comment="사용자번호 — 소유자"
    )
    trip_nm: Mapped[str] = mapped_column(
        String(100), comment="여행명 — 날짜 · 지역 자동 생성 후 수정 가능"
    )
    trip_status_cd: Mapped[str] = mapped_column(
        String(30), comment="여행상태코드 (TRIP_STATUS)"
    )
    visibility_cd: Mapped[str] = mapped_column(
        String(30), comment="공개범위코드 (VISIBILITY)"
    )
    start_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), comment="시작일시"
    )
    end_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), comment="종료일시"
    )
    dist: Mapped[int | None] = mapped_column(
        Integer, comment="거리(m) — 반정규화: 완료 시 계산"
    )
    duration: Mapped[int | None] = mapped_column(
        Integer, comment="소요시간(초) — 반정규화: 완료 시 계산"
    )
    place_cnt: Mapped[int] = mapped_column(
        Integer, server_default="0", comment="장소개수 — 반정규화: 여행장소내역 건수"
    )


class TripCompanionDtl(CommonColumns, Base):
    __tablename__ = "trip_companion_dtl"
    __table_args__ = table_args("여행동행자내역 — 여행 1건의 동행자 유형 목록")

    trip_companion_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="여행동행자번호"
    )
    trip_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("trip_mst.trip_no"), comment="여행번호"
    )
    companion_type_cd: Mapped[str] = mapped_column(
        String(30), comment="동행자유형코드 (COMPANION_TYPE)"
    )


class TripStatusHist(CommonColumns, Base):
    __tablename__ = "trip_status_hist"
    __table_args__ = table_args("여행상태이력 — 여행 상태 변경 기록")

    trip_status_hist_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="여행상태이력번호"
    )
    trip_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("trip_mst.trip_no"), comment="여행번호"
    )
    trip_status_cd: Mapped[str] = mapped_column(
        String(30), comment="여행상태코드 — 변경 후 상태. 변경 시각 = created_at"
    )


class TripRoutePointDtl(CommonColumns, Base):
    __tablename__ = "trip_route_point_dtl"
    __table_args__ = table_args("여행경로점내역 — 앱이 자동 저장한 원본 좌표(R13)")

    trip_route_point_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="여행경로점번호"
    )
    trip_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("trip_mst.trip_no"), comment="여행번호"
    )
    lat: Mapped[Decimal] = mapped_column(Numeric(10, 7), comment="위도")
    lng: Mapped[Decimal] = mapped_column(Numeric(10, 7), comment="경도")
    recorded_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), comment="기록일시 — 좌표 저장 시각"
    )


class TripPlaceDtl(CommonColumns, Base):
    __tablename__ = "trip_place_dtl"
    __table_args__ = table_args(
        "여행장소내역 — 여행 중 기록한 장소(방문 순서는 recorded_at · 결정 로그 D09)",
        CheckConstraint("draft_yn IN ('Y', 'N')", name="draft_yn"),
    )

    trip_place_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="여행장소번호"
    )
    trip_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("trip_mst.trip_no"), comment="여행번호"
    )
    place_no: Mapped[int | None] = mapped_column(
        BigInteger,
        ForeignKey("place_mst.place_no"),
        comment="장소번호 — 공식 장소(place_mst)로 확인 전에는 NULL",
    )
    user_place_nm: Mapped[str | None] = mapped_column(
        String(100), comment="사용자장소명 — 사용자가 부르는 이름(결정 로그 D19)"
    )
    place_type_cd: Mapped[str | None] = mapped_column(
        String(30),
        ForeignKey("place_type_mst.place_type_cd"),
        comment="장소유형코드 — 사용자가 고른 유형(결정 로그 D22)",
    )
    lat: Mapped[Decimal] = mapped_column(Numeric(10, 7), comment="위도 — 기록한 위치")
    lng: Mapped[Decimal] = mapped_column(Numeric(10, 7), comment="경도 — 기록한 위치")
    recorded_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        comment="기록일시 — 사용자가 기록한 업무 시각(타임라인 표시)",
    )
    draft_yn: Mapped[str] = mapped_column(
        CHAR(1), server_default="Y", comment="임시저장여부 — 상세 입력 전이면 Y"
    )
    memo: Mapped[str | None] = mapped_column(
        String(500), comment="메모 — 경험 메모(정차 후 입력 · NULL 허용 결정 로그 D18)"
    )
    review_polarity_cd: Mapped[str | None] = mapped_column(
        String(30), comment="후기성격코드 (REVIEW_POLARITY · NULL 허용 결정 로그 D18)"
    )


class TripPlaceTagRel(CommonColumns, Base):
    __tablename__ = "trip_place_tag_rel"
    __table_args__ = table_args(
        "여행장소태그관계 — 여행장소내역과 태그의 N:M",
        UniqueConstraint(
            "trip_place_no", "tag_no", name="uq_trip_place_tag_rel_trip_place_no_tag_no"
        ),
    )

    trip_place_tag_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="여행장소태그번호"
    )
    trip_place_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("trip_place_dtl.trip_place_no"), comment="여행장소번호"
    )
    tag_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("tag_mst.tag_no"), comment="태그번호"
    )


class TripRouteDtl(CommonColumns, Base):
    __tablename__ = "trip_route_dtl"
    __table_args__ = table_args(
        "여행경로내역 — 여행 종료 때 원본 경로점을 간소화한 경로(여행과 1:1)",
        UniqueConstraint("trip_no", name="uq_trip_route_dtl_trip_no"),
    )

    trip_route_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="여행경로번호"
    )
    trip_no: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("trip_mst.trip_no"), comment="여행번호"
    )
    route_data: Mapped[str] = mapped_column(
        Text, comment="경로데이터 — 인코딩 폴리라인(구글 · 정밀도 1e-5)"
    )
