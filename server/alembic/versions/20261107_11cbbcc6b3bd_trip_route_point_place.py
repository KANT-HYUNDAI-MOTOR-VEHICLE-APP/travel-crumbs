"""trip_route_point_dtl 여행경로점내역 · trip_place_dtl 여행장소내역 생성 — 시나리오 1 묶음 4(여행 기록 중 A-05)

기준: ERD v0.3 · 결정 로그 D09(place_ord 없음) · D18(memo · review_polarity_cd NULL 허용)
place_no FK 는 place_mst 를 만드는 묶음 5에서 추가

Revision ID: 11cbbcc6b3bd
Revises: 9093632c7fc5
Create Date: 2026-11-07 21:05:57.081485

"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "11cbbcc6b3bd"
down_revision: Union[str, Sequence[str], None] = "9093632c7fc5"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        "trip_place_dtl",
        sa.Column(
            "trip_place_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="여행장소번호",
        ),
        sa.Column("trip_no", sa.BigInteger(), nullable=False, comment="여행번호"),
        sa.Column(
            "place_no",
            sa.BigInteger(),
            nullable=True,
            comment="장소번호 — 정차 후 장소 연결 전에는 NULL",
        ),
        sa.Column(
            "lat",
            sa.Numeric(precision=10, scale=7),
            nullable=False,
            comment="위도 — 기록한 위치",
        ),
        sa.Column(
            "lng",
            sa.Numeric(precision=10, scale=7),
            nullable=False,
            comment="경도 — 기록한 위치",
        ),
        sa.Column(
            "recorded_at",
            sa.DateTime(timezone=True),
            nullable=False,
            comment="기록일시 — 사용자가 기록한 업무 시각(타임라인 표시)",
        ),
        sa.Column(
            "draft_yn",
            sa.CHAR(length=1),
            server_default="Y",
            nullable=False,
            comment="임시저장여부 — 상세 입력 전이면 Y",
        ),
        sa.Column(
            "memo",
            sa.String(length=500),
            nullable=True,
            comment="메모 — 경험 메모(정차 후 입력 · NULL 허용 결정 로그 D18)",
        ),
        sa.Column(
            "review_polarity_cd",
            sa.String(length=30),
            nullable=True,
            comment="후기성격코드 (REVIEW_POLARITY · NULL 허용 결정 로그 D18)",
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
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_trip_place_dtl_deleted_yn")
        ),
        sa.CheckConstraint(
            "draft_yn IN ('Y', 'N')", name=op.f("ck_trip_place_dtl_draft_yn")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_place_dtl_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["trip_no"], ["trip_mst.trip_no"], name=op.f("fk_trip_place_dtl_trip_no")
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_place_dtl_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint("trip_place_no", name=op.f("pk_trip_place_dtl")),
        comment="여행장소내역 — 여행 중 기록한 장소(방문 순서는 recorded_at · 결정 로그 D09)",
    )
    op.create_table(
        "trip_route_point_dtl",
        sa.Column(
            "trip_route_point_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="여행경로점번호",
        ),
        sa.Column("trip_no", sa.BigInteger(), nullable=False, comment="여행번호"),
        sa.Column(
            "lat", sa.Numeric(precision=10, scale=7), nullable=False, comment="위도"
        ),
        sa.Column(
            "lng", sa.Numeric(precision=10, scale=7), nullable=False, comment="경도"
        ),
        sa.Column(
            "recorded_at",
            sa.DateTime(timezone=True),
            nullable=False,
            comment="기록일시 — 좌표 저장 시각",
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
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_trip_route_point_dtl_deleted_yn")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_route_point_dtl_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["trip_no"],
            ["trip_mst.trip_no"],
            name=op.f("fk_trip_route_point_dtl_trip_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_route_point_dtl_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint(
            "trip_route_point_no", name=op.f("pk_trip_route_point_dtl")
        ),
        comment="여행경로점내역 — 앱이 자동 저장한 원본 좌표(R13)",
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table("trip_route_point_dtl")
    op.drop_table("trip_place_dtl")
