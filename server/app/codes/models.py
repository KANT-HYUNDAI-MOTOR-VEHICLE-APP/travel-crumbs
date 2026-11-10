"""cd_mst 코드마스터 — 통합 공통 코드 한 테이블(값이 적고 다른 속성이 없는 코드 · 결정 로그 D24 · D25)."""

from sqlalchemy import (
    CHAR,
    BigInteger,
    CheckConstraint,
    Identity,
    Integer,
    String,
    UniqueConstraint,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, CommonColumns, table_args


class CdMst(CommonColumns, Base):
    __tablename__ = "cd_mst"
    __table_args__ = table_args(
        "코드마스터 — 통합 공통 코드(코드그룹명 + 코드 중복 불가)",
        CheckConstraint("use_yn IN ('Y', 'N')", name="use_yn"),
        UniqueConstraint("cd_grp_nm", "cd", name="uq_cd_mst_cd_grp_nm_cd"),
    )

    cd_no: Mapped[int] = mapped_column(
        BigInteger, Identity(always=False), primary_key=True, comment="코드번호"
    )
    cd_grp_nm: Mapped[str] = mapped_column(
        String(100), comment="코드그룹명 — 코드 목록의 이름(예: REVIEW_POLARITY)"
    )
    cd: Mapped[str] = mapped_column(String(30), comment="코드")
    cd_nm: Mapped[str] = mapped_column(
        String(100), comment="코드명 — 화면에 보이는 글자"
    )
    sort_ord: Mapped[int] = mapped_column(Integer, comment="정렬순서")
    use_yn: Mapped[str] = mapped_column(CHAR(1), server_default="Y", comment="사용여부")
