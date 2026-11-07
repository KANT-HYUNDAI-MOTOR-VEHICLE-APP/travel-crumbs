"""trip_route_dtl 여행경로내역 · data_change_hist 데이터변경이력 생성 — 시나리오 1 묶음 6(여행 종료 · 상세 A-08)

기준: ERD v0.3 trip_route_dtl(route_data = 인코딩 폴리라인) · 결정 로그 D17 · D26

Revision ID: 401ebd4217bc
Revises: de54ca08754f
Create Date: 2026-11-07 23:06:47.382923

"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "401ebd4217bc"
down_revision: Union[str, Sequence[str], None] = "de54ca08754f"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        "data_change_hist",
        sa.Column(
            "data_change_hist_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="데이터변경이력번호",
        ),
        sa.Column(
            "table_nm",
            sa.String(length=100),
            nullable=False,
            comment="테이블명 — 예: trip_mst",
        ),
        sa.Column(
            "column_nm",
            sa.String(length=100),
            nullable=False,
            comment="컬럼명 — 예: trip_nm",
        ),
        sa.Column(
            "row_no",
            sa.BigInteger(),
            nullable=False,
            comment="행번호 — 바뀐 행의 PK 번호",
        ),
        sa.Column(
            "before_value", sa.String(length=500), nullable=True, comment="변경전값"
        ),
        sa.Column(
            "after_value", sa.String(length=500), nullable=True, comment="변경후값"
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
            comment="생성일시",
        ),
        sa.Column(
            "created_user_no", sa.BigInteger(), nullable=False, comment="생성사용자번호"
        ),
        sa.Column(
            "updated_at", sa.DateTime(timezone=True), nullable=True, comment="수정일시"
        ),
        sa.Column(
            "updated_user_no", sa.BigInteger(), nullable=True, comment="수정사용자번호"
        ),
        sa.Column(
            "deleted_yn",
            sa.CHAR(length=1),
            server_default="N",
            nullable=False,
            comment="삭제여부 — 논리 삭제면 Y",
        ),
        sa.CheckConstraint(
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_data_change_hist_deleted_yn")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_data_change_hist_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_data_change_hist_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint(
            "data_change_hist_no", name=op.f("pk_data_change_hist")
        ),
        comment="데이터변경이력 — 어느 테이블 · 컬럼 · 행의 값이 언제 누구에 의해 바뀌었는지(변경 일시 = created_at)",
    )
    op.create_table(
        "trip_route_dtl",
        sa.Column(
            "trip_route_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="여행경로번호",
        ),
        sa.Column("trip_no", sa.BigInteger(), nullable=False, comment="여행번호"),
        sa.Column(
            "route_data",
            sa.Text(),
            nullable=False,
            comment="경로데이터 — 인코딩 폴리라인(구글 · 정밀도 1e-5)",
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
            comment="생성일시",
        ),
        sa.Column(
            "created_user_no", sa.BigInteger(), nullable=False, comment="생성사용자번호"
        ),
        sa.Column(
            "updated_at", sa.DateTime(timezone=True), nullable=True, comment="수정일시"
        ),
        sa.Column(
            "updated_user_no", sa.BigInteger(), nullable=True, comment="수정사용자번호"
        ),
        sa.Column(
            "deleted_yn",
            sa.CHAR(length=1),
            server_default="N",
            nullable=False,
            comment="삭제여부 — 논리 삭제면 Y",
        ),
        sa.CheckConstraint(
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_trip_route_dtl_deleted_yn")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_route_dtl_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["trip_no"], ["trip_mst.trip_no"], name=op.f("fk_trip_route_dtl_trip_no")
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_route_dtl_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint("trip_route_no", name=op.f("pk_trip_route_dtl")),
        sa.UniqueConstraint("trip_no", name="uq_trip_route_dtl_trip_no"),
        comment="여행경로내역 — 여행 종료 때 원본 경로점을 간소화한 경로(여행과 1:1)",
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table("trip_route_dtl")
    op.drop_table("data_change_hist")
