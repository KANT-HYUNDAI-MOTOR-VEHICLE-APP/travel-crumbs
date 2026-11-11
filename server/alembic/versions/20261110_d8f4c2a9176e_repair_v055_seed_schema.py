"""데이터 표준 v0.55 통합 시드 스키마 보정.

Revision ID: d8f4c2a9176e
Revises: b7e3d91a5c42
Create Date: 2026-11-10
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

revision: str = "d8f4c2a9176e"
down_revision: str | Sequence[str] | None = "b7e3d91a5c42"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def _column_names(table_name: str) -> set[str]:
    return {column["name"] for column in sa.inspect(op.get_bind()).get_columns(table_name)}


def _table_names() -> set[str]:
    return set(sa.inspect(op.get_bind()).get_table_names())


def _add_address_columns() -> None:
    if "addr" not in _column_names("place_mst"):
        op.add_column(
            "place_mst",
            sa.Column("addr", sa.String(300), nullable=True, comment="주소"),
        )
    if "addr" not in _column_names("trip_place_dtl"):
        op.add_column(
            "trip_place_dtl",
            sa.Column("addr", sa.String(300), nullable=True, comment="주소 스냅샷"),
        )


def _create_rating_tables() -> None:
    tables = _table_names()
    if "trip_rating_dtl" not in tables:
        op.create_table(
            "trip_rating_dtl",
            sa.Column("trip_rating_no", sa.BigInteger(), sa.Identity(), nullable=False),
            sa.Column("trip_no", sa.BigInteger(), nullable=False),
            sa.Column("user_no", sa.BigInteger(), nullable=False),
            sa.Column("rating_score", sa.Numeric(2, 1), nullable=False),
            sa.Column("rated_at", sa.DateTime(timezone=True), nullable=False),
            sa.Column(
                "created_at",
                sa.DateTime(timezone=True),
                server_default=sa.text("now()"),
                nullable=False,
            ),
            sa.Column("created_user_no", sa.BigInteger(), nullable=False),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("updated_user_no", sa.BigInteger(), nullable=True),
            sa.Column(
                "deleted_yn", sa.CHAR(1), server_default="N", nullable=False
            ),
            sa.CheckConstraint(
                "deleted_yn IN ('Y', 'N')", name="ck_trip_rating_dtl_deleted_yn"
            ),
            sa.CheckConstraint(
                "rating_score BETWEEN 1 AND 5",
                name="ck_trip_rating_dtl_rating_score",
            ),
            sa.ForeignKeyConstraint(
                ["trip_no"], ["trip_mst.trip_no"], name="fk_trip_rating_dtl_trip_no"
            ),
            sa.ForeignKeyConstraint(
                ["user_no"], ["user_mst.user_no"], name="fk_trip_rating_dtl_user_no"
            ),
            sa.ForeignKeyConstraint(
                ["created_user_no"],
                ["user_mst.user_no"],
                name="fk_trip_rating_dtl_created_user_no",
            ),
            sa.ForeignKeyConstraint(
                ["updated_user_no"],
                ["user_mst.user_no"],
                name="fk_trip_rating_dtl_updated_user_no",
            ),
            sa.PrimaryKeyConstraint("trip_rating_no", name="pk_trip_rating_dtl"),
            sa.UniqueConstraint(
                "trip_no", "user_no", name="uq_trip_rating_dtl_trip_no_user_no"
            ),
        )

    if "trip_rating_tag_rel" not in _table_names():
        op.create_table(
            "trip_rating_tag_rel",
            sa.Column(
                "trip_rating_tag_no", sa.BigInteger(), sa.Identity(), nullable=False
            ),
            sa.Column("trip_rating_no", sa.BigInteger(), nullable=False),
            sa.Column("tag_no", sa.BigInteger(), nullable=False),
            sa.Column(
                "created_at",
                sa.DateTime(timezone=True),
                server_default=sa.text("now()"),
                nullable=False,
            ),
            sa.Column("created_user_no", sa.BigInteger(), nullable=False),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=True),
            sa.Column("updated_user_no", sa.BigInteger(), nullable=True),
            sa.Column(
                "deleted_yn", sa.CHAR(1), server_default="N", nullable=False
            ),
            sa.CheckConstraint(
                "deleted_yn IN ('Y', 'N')",
                name="ck_trip_rating_tag_rel_deleted_yn",
            ),
            sa.ForeignKeyConstraint(
                ["trip_rating_no"],
                ["trip_rating_dtl.trip_rating_no"],
                name="fk_trip_rating_tag_rel_trip_rating_no",
            ),
            sa.ForeignKeyConstraint(
                ["tag_no"], ["tag_mst.tag_no"], name="fk_trip_rating_tag_rel_tag_no"
            ),
            sa.ForeignKeyConstraint(
                ["created_user_no"],
                ["user_mst.user_no"],
                name="fk_trip_rating_tag_rel_created_user_no",
            ),
            sa.ForeignKeyConstraint(
                ["updated_user_no"],
                ["user_mst.user_no"],
                name="fk_trip_rating_tag_rel_updated_user_no",
            ),
            sa.PrimaryKeyConstraint(
                "trip_rating_tag_no", name="pk_trip_rating_tag_rel"
            ),
            sa.UniqueConstraint(
                "trip_rating_no",
                "tag_no",
                name="uq_trip_rating_tag_rel_rating_tag",
            ),
        )


def _create_navigation_log() -> None:
    if "navi_req_log" in _table_names():
        return
    op.create_table(
        "navi_req_log",
        sa.Column("navi_req_log_no", sa.BigInteger(), sa.Identity(), nullable=False),
        sa.Column("user_no", sa.BigInteger(), nullable=False),
        sa.Column("place_no", sa.BigInteger(), nullable=True),
        sa.Column("planned_route_no", sa.BigInteger(), nullable=True),
        sa.Column("navi_req_status_cd", sa.String(30), nullable=False),
        sa.Column("err_msg", sa.String(500), nullable=True),
        sa.Column("req_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("resp_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.Column("created_user_no", sa.BigInteger(), nullable=False),
        sa.ForeignKeyConstraint(
            ["user_no"], ["user_mst.user_no"], name="fk_navi_req_log_user_no"
        ),
        sa.ForeignKeyConstraint(
            ["place_no"], ["place_mst.place_no"], name="fk_navi_req_log_place_no"
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name="fk_navi_req_log_created_user_no",
        ),
        sa.PrimaryKeyConstraint("navi_req_log_no", name="pk_navi_req_log"),
        sa.UniqueConstraint(
            "user_no", "place_no", "req_at", name="uq_navi_req_log_user_place_req_at"
        ),
    )


def _ensure_navigation_codes() -> None:
    op.execute(
        "INSERT INTO cd_mst "
        "(cd_grp_nm, cd, cd_nm, sort_ord, use_yn, created_user_no) VALUES "
        "('NAVI_REQ_STATUS_CD', 'REQUESTING', '요청 중', 1, 'Y', 1), "
        "('NAVI_REQ_STATUS_CD', 'SUCCESS', '성공', 2, 'Y', 1), "
        "('NAVI_REQ_STATUS_CD', 'FAILED', '실패', 3, 'Y', 1) "
        "ON CONFLICT (cd_grp_nm, cd) DO UPDATE SET "
        "cd_nm = EXCLUDED.cd_nm, sort_ord = EXCLUDED.sort_ord, use_yn = 'Y', "
        "deleted_yn = 'N', updated_at = now(), updated_user_no = 1"
    )


def upgrade() -> None:
    _add_address_columns()
    _create_rating_tables()
    _create_navigation_log()
    _ensure_navigation_codes()


def downgrade() -> None:
    pass
