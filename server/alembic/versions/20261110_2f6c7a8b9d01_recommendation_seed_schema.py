"""추천 시드 공통 스키마

Revision ID: 2f6c7a8b9d01
Revises: 4a93d2b71f64
Create Date: 2026-11-10
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "2f6c7a8b9d01"
down_revision: str | Sequence[str] | None = "4a93d2b71f64"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "place_mst",
        sa.Column(
            "addr",
            sa.String(length=300),
            nullable=True,
            comment="주소 — 화면 표시·지역 검색용 원문",
        ),
    )
    op.add_column(
        "trip_place_dtl",
        sa.Column(
            "addr",
            sa.String(length=300),
            nullable=True,
            comment="주소 — 기록 시점의 장소 주소 스냅샷",
        ),
    )
    op.create_table(
        "trip_rating_dtl",
        sa.Column(
            "trip_rating_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="여행평점번호",
        ),
        sa.Column("trip_no", sa.BigInteger(), nullable=False, comment="평가한 여행번호"),
        sa.Column("user_no", sa.BigInteger(), nullable=False, comment="평가한 사용자번호"),
        sa.Column(
            "rating_score",
            sa.Numeric(precision=2, scale=1),
            nullable=False,
            comment="평점 — 1.0~5.0",
        ),
        sa.Column(
            "rated_at", sa.DateTime(timezone=True), nullable=False, comment="평가일시"
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
            "deleted_yn IN ('Y', 'N')",
            name=op.f("ck_trip_rating_dtl_deleted_yn"),
        ),
        sa.CheckConstraint(
            "rating_score BETWEEN 1 AND 5",
            name=op.f("ck_trip_rating_dtl_rating_score"),
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_rating_dtl_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["trip_no"],
            ["trip_mst.trip_no"],
            name=op.f("fk_trip_rating_dtl_trip_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_rating_dtl_updated_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_rating_dtl_user_no"),
        ),
        sa.PrimaryKeyConstraint("trip_rating_no", name=op.f("pk_trip_rating_dtl")),
        sa.UniqueConstraint(
            "trip_no", "user_no", name="uq_trip_rating_dtl_trip_no_user_no"
        ),
        comment="여행평점내역 — 사용자가 길안내 후 남긴 여행 단위 평점",
    )
    op.create_table(
        "trip_rating_tag_rel",
        sa.Column(
            "trip_rating_tag_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="여행평점태그번호",
        ),
        sa.Column(
            "trip_rating_no", sa.BigInteger(), nullable=False, comment="여행평점번호"
        ),
        sa.Column("tag_no", sa.BigInteger(), nullable=False, comment="태그번호"),
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
            "deleted_yn IN ('Y', 'N')",
            name=op.f("ck_trip_rating_tag_rel_deleted_yn"),
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_rating_tag_rel_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["tag_no"],
            ["tag_mst.tag_no"],
            name=op.f("fk_trip_rating_tag_rel_tag_no"),
        ),
        sa.ForeignKeyConstraint(
            ["trip_rating_no"],
            ["trip_rating_dtl.trip_rating_no"],
            name=op.f("fk_trip_rating_tag_rel_trip_rating_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_rating_tag_rel_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint("trip_rating_tag_no", name=op.f("pk_trip_rating_tag_rel")),
        sa.UniqueConstraint(
            "trip_rating_no", "tag_no", name="uq_trip_rating_tag_rel_rating_tag"
        ),
        comment="여행평점태그관계 — 평점과 좋았던 점 태그의 N:M",
    )


def downgrade() -> None:
    op.drop_table("trip_rating_tag_rel")
    op.drop_table("trip_rating_dtl")
    op.drop_column("trip_place_dtl", "addr")
    op.drop_column("place_mst", "addr")
