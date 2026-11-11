"""place_mst 장소마스터 · tag_mst 태그마스터 — ERD v0.3 + 결정 로그 D20 · D22."""

from decimal import Decimal

from sqlalchemy import (
    CHAR,
    BigInteger,
    CheckConstraint,
    ForeignKey,
    Identity,
    Integer,
    Numeric,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, CommonColumns, table_args


class DataSrc:
    """DATA_SRC 코드 — 내비 POI 로 확인된 장소(결정 로그 D20)."""

    NAVI = "NAVI"


class PlaceMst(CommonColumns, Base):
    __tablename__ = "place_mst"
    __table_args__ = table_args(
        "장소마스터 — 내비 POI · 공공데이터로 확인된 공식 장소(1곳 1행 · 결정 로그 D19)",
        UniqueConstraint(
            "poi_provider", "poi_id", name="uq_place_mst_poi_provider_poi_id"
        ),
    )

    place_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="장소번호"
    )
    place_nm: Mapped[str] = mapped_column(String(100), comment="장소명 — 공식 명칭")
    place_type_cd: Mapped[str | None] = mapped_column(
        String(30),
        ForeignKey("place_type_mst.place_type_cd"),
        comment="장소유형코드 — 내비 장소는 모름 → NULL(결정 로그 D22)",
    )
    lat: Mapped[Decimal] = mapped_column(Numeric(10, 7), comment="위도")
    lng: Mapped[Decimal] = mapped_column(Numeric(10, 7), comment="경도")
    data_src_cd: Mapped[str] = mapped_column(
        String(30), comment="데이터출처코드 (DATA_SRC)"
    )
    poi_id: Mapped[str | None] = mapped_column(
        String(50), comment="POI번호 — 지도 회사의 장소 고유번호(D20)"
    )
    poi_provider: Mapped[str | None] = mapped_column(
        String(30), comment="POI제공자 — POI번호를 만든 지도 회사(D20)"
    )


class TagMst(CommonColumns, Base):
    __tablename__ = "tag_mst"
    __table_args__ = table_args(
        "태그마스터 — 경험 키워드(주의점 · 팁 포함 · 결정 로그 D21)",
        CheckConstraint("use_yn IN ('Y', 'N')", name="use_yn"),
    )

    tag_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="태그번호"
    )
    tag_nm: Mapped[str] = mapped_column(String(100), comment="태그명")
    use_yn: Mapped[str] = mapped_column(CHAR(1), server_default="Y", comment="사용여부")


class PlaceTypeMst(CommonColumns, Base):
    __tablename__ = "place_type_mst"
    __table_args__ = table_args(
        "장소유형마스터 — 값이 많고 카테고리 속성이 있어 공통 코드에서 분리(결정 로그 D25)",
        CheckConstraint("use_yn IN ('Y', 'N')", name="use_yn"),
    )

    # 개별 코드 테이블 — 코드 자체가 PK, 다른 테이블은 코드로 가리킴(결정 로그 D25)
    place_type_cd: Mapped[str] = mapped_column(
        String(30), primary_key=True, comment="장소유형코드 — 예: BEACH"
    )
    place_type_nm: Mapped[str] = mapped_column(
        String(100), comment="장소유형명 — 예: 바다·해변"
    )
    place_category_cd: Mapped[str] = mapped_column(
        String(30), comment="장소카테고리코드 — 예: NATURE"
    )
    place_category_nm: Mapped[str] = mapped_column(
        String(100), comment="장소카테고리명 — 예: 자연"
    )
    sort_ord: Mapped[int] = mapped_column(Integer, comment="정렬순서")
    use_yn: Mapped[str] = mapped_column(CHAR(1), server_default="Y", comment="사용여부")
