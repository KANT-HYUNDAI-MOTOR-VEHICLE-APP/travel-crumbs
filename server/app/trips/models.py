"""trip_mst 여행마스터 · trip_companion_dtl 여행동행자내역 · trip_status_hist 여행상태이력 — ERD v0.3 그대로."""

from datetime import datetime

from sqlalchemy import CHAR, BigInteger, DateTime, ForeignKey, Identity, Integer, String
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
