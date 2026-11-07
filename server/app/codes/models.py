"""cd_grp_mst 코드그룹마스터 · cd_dtl 코드내역 — ERD v0.3 그대로(표준정의서 R10 통합 공통 코드)."""

from sqlalchemy import CHAR, CheckConstraint, ForeignKey, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base, CommonColumns, table_args


class CdGrpMst(CommonColumns, Base):
    __tablename__ = "cd_grp_mst"
    __table_args__ = table_args("코드그룹마스터 — 코드 그룹(R10)")

    # 업무키 PK 예외(M07) — 일련번호 대신 코드 자체가 PK
    cd_grp_cd: Mapped[str] = mapped_column(
        String(30), primary_key=True, comment="코드그룹코드"
    )
    cd_grp_nm: Mapped[str] = mapped_column(String(100), comment="코드그룹명")


class CdDtl(CommonColumns, Base):
    __tablename__ = "cd_dtl"
    __table_args__ = table_args(
        "코드내역 — 코드 그룹에 딸린 코드값",
        CheckConstraint("use_yn IN ('Y', 'N')", name="use_yn"),
    )

    cd_grp_cd: Mapped[str] = mapped_column(
        String(30),
        ForeignKey("cd_grp_mst.cd_grp_cd"),
        primary_key=True,
        comment="코드그룹코드",
    )
    cd: Mapped[str] = mapped_column(String(30), primary_key=True, comment="코드")
    cd_nm: Mapped[str] = mapped_column(String(100), comment="코드명")
    sort_ord: Mapped[int] = mapped_column(Integer, comment="정렬순서")
    use_yn: Mapped[str] = mapped_column(CHAR(1), server_default="Y", comment="사용여부")
