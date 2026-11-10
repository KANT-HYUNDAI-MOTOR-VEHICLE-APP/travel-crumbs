"""모든 테이블 모델의 부모 + 공통 컬럼(표준정의서 R07 · ERD v0.3)."""

from datetime import datetime

from sqlalchemy import CHAR, BigInteger, CheckConstraint, DateTime, ForeignKey, MetaData, func
from sqlalchemy.orm import DeclarativeBase, Mapped, declared_attr, mapped_column

# 제약조건 이름 규칙 — Alembic 이 같은 이름을 만들도록 고정
NAMING_CONVENTION = {
    "ix": "ix_%(table_name)s_%(column_0_name)s",
    "uq": "uq_%(table_name)s_%(column_0_name)s",
    "ck": "ck_%(table_name)s_%(constraint_name)s",
    "fk": "fk_%(table_name)s_%(column_0_name)s",
    "pk": "pk_%(table_name)s",
}


class Base(DeclarativeBase):
    metadata = MetaData(naming_convention=NAMING_CONVENTION)


class CommonColumns:
    """공통 컬럼 5개 — 컬럼 순서 규칙(T05)대로 업무 컬럼 뒤에 붙음."""

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), sort_order=900, comment="생성일시"
    )

    @declared_attr
    def created_user_no(cls) -> Mapped[int]:
        return mapped_column(
            BigInteger, ForeignKey("user_mst.user_no"), sort_order=901, comment="생성사용자번호"
        )

    updated_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), sort_order=902, comment="수정일시"
    )

    @declared_attr
    def updated_user_no(cls) -> Mapped[int | None]:
        return mapped_column(
            BigInteger, ForeignKey("user_mst.user_no"), sort_order=903, comment="수정사용자번호"
        )

    deleted_yn: Mapped[str] = mapped_column(
        CHAR(1),
        server_default="N",
        sort_order=904,
        comment="삭제여부 — 논리 삭제면 Y",
    )


def table_args(comment: str, *constraints: object) -> tuple[object, ...]:
    """테이블마다 공통 제약(deleted_yn 은 Y · N 만) + 테이블 설명(comment)."""
    return (
        CheckConstraint("deleted_yn IN ('Y', 'N')", name="deleted_yn"),
        *constraints,
        {"comment": comment},
    )
