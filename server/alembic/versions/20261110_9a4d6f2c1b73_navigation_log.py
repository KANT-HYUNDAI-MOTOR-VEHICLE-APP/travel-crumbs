"""길안내 요청 로그와 상태 코드

Revision ID: 9a4d6f2c1b73
Revises: 2f6c7a8b9d01
Create Date: 2026-11-10
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "9a4d6f2c1b73"
down_revision: str | Sequence[str] | None = "2f6c7a8b9d01"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    op.execute(
        "INSERT INTO cd_mst "
        "(cd_grp_nm, cd, cd_nm, sort_ord, use_yn, created_user_no) VALUES "
        "('NAVI_REQ_STATUS', 'REQUESTING', '요청 중', 1, 'Y', 1), "
        "('NAVI_REQ_STATUS', 'SUCCESS', '성공', 2, 'Y', 1), "
        "('NAVI_REQ_STATUS', 'FAILED', '실패', 3, 'Y', 1) "
        "ON CONFLICT (cd_grp_nm, cd) DO NOTHING"
    )
    op.create_table(
        "navi_req_log",
        sa.Column(
            "navi_req_log_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="길안내요청로그번호",
        ),
        sa.Column("user_no", sa.BigInteger(), nullable=False, comment="사용자번호"),
        sa.Column("place_no", sa.BigInteger(), nullable=True, comment="장소번호"),
        sa.Column(
            "planned_route_no",
            sa.BigInteger(),
            nullable=True,
            comment="계획경로번호 — 여러 장소 경로일 때 사용",
        ),
        sa.Column(
            "navi_req_status_cd",
            sa.String(length=30),
            nullable=False,
            comment="길안내요청상태코드",
        ),
        sa.Column(
            "err_msg", sa.String(length=500), nullable=True, comment="오류메시지"
        ),
        sa.Column(
            "req_at",
            sa.DateTime(timezone=True),
            nullable=False,
            comment="요청일시 — 앱에서 길안내를 시작한 시각",
        ),
        sa.Column(
            "resp_at", sa.DateTime(timezone=True), nullable=True, comment="응답일시"
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
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_navi_req_log_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["place_no"],
            ["place_mst.place_no"],
            name=op.f("fk_navi_req_log_place_no"),
        ),
        sa.ForeignKeyConstraint(
            ["user_no"], ["user_mst.user_no"], name=op.f("fk_navi_req_log_user_no")
        ),
        sa.PrimaryKeyConstraint("navi_req_log_no", name=op.f("pk_navi_req_log")),
        sa.UniqueConstraint(
            "user_no",
            "place_no",
            "req_at",
            name="uq_navi_req_log_user_place_req_at",
        ),
        comment="길안내요청로그 — 길안내 시작 사건의 추가 전용 기록",
    )


def downgrade() -> None:
    op.drop_table("navi_req_log")
    op.execute("DELETE FROM cd_mst WHERE cd_grp_nm = 'NAVI_REQ_STATUS'")
