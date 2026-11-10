"""장소 저장 요청 중복 방지 — Gleo #25.

Revision ID: 4a93d2b71f64
Revises: 853db12d8820
Create Date: 2026-11-10 08:21:39.087446

"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

# revision identifiers, used by Alembic.
revision: str = "4a93d2b71f64"
down_revision: str | Sequence[str] | None = "853db12d8820"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.add_column(
        "trip_place_dtl",
        sa.Column(
            "req_uuid",
            sa.CHAR(36),
            nullable=True,
            comment="요청UUID — 장소 저장 재전송 식별",
        ),
    )
    op.add_column(
        "trip_place_dtl",
        sa.Column(
            "req_hash",
            sa.String(255),
            nullable=True,
            comment="요청해시 — 검증된 장소 저장 본문의 SHA-256",
        ),
    )
    op.create_check_constraint(
        op.f("ck_trip_place_dtl_req_pair"),
        "trip_place_dtl",
        "(req_uuid IS NULL) = (req_hash IS NULL)",
    )
    op.create_unique_constraint(
        "uq_trip_place_dtl_trip_no_created_user_no_req_uuid",
        "trip_place_dtl",
        ["trip_no", "created_user_no", "req_uuid"],
    )


def downgrade() -> None:
    op.drop_constraint(
        "uq_trip_place_dtl_trip_no_created_user_no_req_uuid",
        "trip_place_dtl",
        type_="unique",
    )
    op.drop_constraint(
        op.f("ck_trip_place_dtl_req_pair"), "trip_place_dtl", type_="check"
    )
    op.drop_column("trip_place_dtl", "req_hash")
    op.drop_column("trip_place_dtl", "req_uuid")
