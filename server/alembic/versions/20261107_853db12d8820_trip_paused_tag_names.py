"""여행 상태 PAUSED(일시정지) 추가 · 경험 태그 이름을 해요체로 — 결정 로그 D27

상태 순서: 기록 중 · 일시정지 · 완료 · 중단 · 취소

Revision ID: 853db12d8820
Revises: 3f9bcf1893d1
Create Date: 2026-11-07 23:36:38.340534

"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "853db12d8820"
down_revision: Union[str, Sequence[str], None] = "3f9bcf1893d1"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


TAG_RENAMES = [
    ("주차 편함", "주차 편해요"),
    ("아이 동반 좋음", "아이와 가기 좋아요"),
    ("화장실 깨끗", "화장실 깨끗해요"),
    ("재방문", "다시 가고 싶어요"),
    ("웨이팅 있음", "웨이팅 있어요"),
]
STATUS_ORDER = ["RECORDING", "PAUSED", "COMPLETED", "INTERRUPTED", "CANCELED"]


def upgrade() -> None:
    op.execute(
        "INSERT INTO cd_mst (cd_grp_nm, cd, cd_nm, sort_ord, created_user_no) "
        "VALUES ('TRIP_STATUS', 'PAUSED', '일시정지', 2, 1) ON CONFLICT (cd_grp_nm, cd) DO NOTHING"
    )
    for i, cd in enumerate(STATUS_ORDER, 1):
        op.execute(
            f"UPDATE cd_mst SET sort_ord = {i} WHERE cd_grp_nm = 'TRIP_STATUS' AND cd = '{cd}'"
        )
    for old, new in TAG_RENAMES:
        op.execute(
            f"UPDATE tag_mst SET tag_nm = '{new}', updated_at = now(), updated_user_no = 1 WHERE tag_nm = '{old}'"
        )


def downgrade() -> None:
    for old, new in TAG_RENAMES:
        op.execute(f"UPDATE tag_mst SET tag_nm = '{old}' WHERE tag_nm = '{new}'")
    op.execute("DELETE FROM cd_mst WHERE cd_grp_nm = 'TRIP_STATUS' AND cd = 'PAUSED'")
    for i, cd in enumerate(["RECORDING", "COMPLETED", "INTERRUPTED", "CANCELED"], 1):
        op.execute(
            f"UPDATE cd_mst SET sort_ord = {i} WHERE cd_grp_nm = 'TRIP_STATUS' AND cd = '{cd}'"
        )
