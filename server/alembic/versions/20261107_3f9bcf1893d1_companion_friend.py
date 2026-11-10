"""동행자 코드 FRIEND(친구) 추가 — 결정 로그 D16(동행자는 묶지 않고 나눔)

순서: 혼자 · 배우자 · 아이 · 부모님 · 친구 · 반려동물

Revision ID: 3f9bcf1893d1
Revises: 401ebd4217bc
Create Date: 2026-11-07 23:33:00.903135

"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "3f9bcf1893d1"
down_revision: Union[str, Sequence[str], None] = "401ebd4217bc"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute(
        "UPDATE cd_mst SET sort_ord = 6 WHERE cd_grp_nm = 'COMPANION_TYPE' AND cd = 'PET'"
    )
    op.execute(
        "INSERT INTO cd_mst (cd_grp_nm, cd, cd_nm, sort_ord, created_user_no) "
        "VALUES ('COMPANION_TYPE', 'FRIEND', '친구', 5, 1) ON CONFLICT (cd_grp_nm, cd) DO NOTHING"
    )


def downgrade() -> None:
    op.execute(
        "DELETE FROM cd_mst WHERE cd_grp_nm = 'COMPANION_TYPE' AND cd = 'FRIEND'"
    )
    op.execute(
        "UPDATE cd_mst SET sort_ord = 5 WHERE cd_grp_nm = 'COMPANION_TYPE' AND cd = 'PET'"
    )
