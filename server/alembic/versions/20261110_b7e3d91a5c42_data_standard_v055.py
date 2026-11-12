"""데이터 표준 v0.55 코드그룹명과 경험 태그 반영.

Revision ID: b7e3d91a5c42
Revises: 9a4d6f2c1b73
Create Date: 2026-11-10
"""

from collections.abc import Sequence

from alembic import op

revision: str = "b7e3d91a5c42"
down_revision: str | Sequence[str] | None = "9a4d6f2c1b73"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


CODE_GROUP_RENAMES = {
    "COMPANION_TYPE": "COMPANION_TYPE_CD",
    "CONSENT_TYPE": "CONSENT_TYPE_CD",
    "DATA_SRC": "DATA_SRC_CD",
    "REVIEW_POLARITY": "REVIEW_POLARITY_CD",
    "TRIP_STATUS": "TRIP_STATUS_CD",
    "VISIBILITY": "VISIBILITY_CD",
    "NAVI_REQ_STATUS": "NAVI_REQ_STATUS_CD",
}

ADDITIONAL_CODE_GROUPS = {
    "SHARE_STATUS_CD": [
        ("NEW", "새 공유", 1),
        ("ACCEPTED", "수락", 2),
    ],
    "RECO_TYPE_CD": [
        ("CHILD_FRIENDLY", "아이 동반", 1),
        ("FOOD", "맛집", 2),
        ("EASY_PARKING", "주차 편의", 3),
        ("FAST", "빠른 이동", 4),
    ],
}

ADDITIONAL_TAGS = {
    6: "주차 어려워요",
    7: "유모차 이동 편해요",
    8: "수유실 있어요",
    9: "기저귀 교환대 있어요",
    10: "쉬어가기 좋아요",
    11: "붐벼요",
    12: "아이 메뉴 있어요",
    13: "음식이 맛있어요",
}


def upgrade() -> None:
    for old, new in CODE_GROUP_RENAMES.items():
        op.execute(
            f"UPDATE cd_mst SET cd_grp_nm = '{new}', updated_at = now(), "
            f"updated_user_no = 1 WHERE cd_grp_nm = '{old}'"
        )

    for group, codes in ADDITIONAL_CODE_GROUPS.items():
        for code, name, order in codes:
            op.execute(
                "INSERT INTO cd_mst "
                "(cd_grp_nm, cd, cd_nm, sort_ord, use_yn, created_user_no) "
                f"VALUES ('{group}', '{code}', '{name}', {order}, 'Y', 1) "
                "ON CONFLICT (cd_grp_nm, cd) DO UPDATE SET "
                "cd_nm = EXCLUDED.cd_nm, sort_ord = EXCLUDED.sort_ord, "
                "use_yn = 'Y', deleted_yn = 'N', updated_at = now(), updated_user_no = 1"
            )

    for tag_no, tag_nm in ADDITIONAL_TAGS.items():
        op.execute(
            "INSERT INTO tag_mst (tag_no, tag_nm, use_yn, created_user_no) "
            f"OVERRIDING SYSTEM VALUE VALUES ({tag_no}, '{tag_nm}', 'Y', 1) "
            "ON CONFLICT (tag_no) DO UPDATE SET "
            "tag_nm = EXCLUDED.tag_nm, use_yn = 'Y', deleted_yn = 'N', "
            "updated_at = now(), updated_user_no = 1"
        )
    op.execute(
        "SELECT setval(pg_get_serial_sequence('tag_mst', 'tag_no'), "
        "GREATEST((SELECT max(tag_no) FROM tag_mst), 1))"
    )


def downgrade() -> None:
    op.execute("DELETE FROM tag_mst WHERE tag_no BETWEEN 6 AND 13")
    for group in ADDITIONAL_CODE_GROUPS:
        op.execute(f"DELETE FROM cd_mst WHERE cd_grp_nm = '{group}'")
    for old, new in CODE_GROUP_RENAMES.items():
        op.execute(
            f"UPDATE cd_mst SET cd_grp_nm = '{old}', updated_at = now(), "
            f"updated_user_no = 1 WHERE cd_grp_nm = '{new}'"
        )
