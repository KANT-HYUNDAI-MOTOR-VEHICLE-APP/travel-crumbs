"""user_consent_hist 사용자동의이력 생성 — 시나리오 1 묶음 2(위치정보 동의 A-03-1)

기준: ERD v0.3 user_consent_hist · 표준정의서 v0.35 R14

Revision ID: 208944eba6ea
Revises: 0ea1fb4b2906
Create Date: 2026-11-07 17:29:59.040977

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '208944eba6ea'
down_revision: Union[str, Sequence[str], None] = '0ea1fb4b2906'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table('user_consent_hist',
    sa.Column('user_consent_hist_no', sa.BigInteger(), sa.Identity(always=False), nullable=False, comment='사용자동의이력번호'),
    sa.Column('user_no', sa.BigInteger(), nullable=False, comment='사용자번호'),
    sa.Column('consent_type_cd', sa.String(length=30), nullable=False, comment='동의유형코드 (CONSENT_TYPE)'),
    sa.Column('consent_yn', sa.CHAR(length=1), nullable=False, comment='동의여부 — Y 동의 / N 철회'),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False, comment='생성일시'),
    sa.Column('created_user_no', sa.BigInteger(), nullable=False, comment='생성사용자번호'),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True, comment='수정일시'),
    sa.Column('updated_user_no', sa.BigInteger(), nullable=True, comment='수정사용자번호'),
    sa.Column('deleted_yn', sa.CHAR(length=1), server_default='N', nullable=False, comment='삭제여부 — 논리 삭제면 Y'),
    sa.CheckConstraint("consent_yn IN ('Y', 'N')", name=op.f('ck_user_consent_hist_consent_yn')),
    sa.CheckConstraint("deleted_yn IN ('Y', 'N')", name=op.f('ck_user_consent_hist_deleted_yn')),
    sa.ForeignKeyConstraint(['created_user_no'], ['user_mst.user_no'], name=op.f('fk_user_consent_hist_created_user_no')),
    sa.ForeignKeyConstraint(['updated_user_no'], ['user_mst.user_no'], name=op.f('fk_user_consent_hist_updated_user_no')),
    sa.ForeignKeyConstraint(['user_no'], ['user_mst.user_no'], name=op.f('fk_user_consent_hist_user_no')),
    sa.PrimaryKeyConstraint('user_consent_hist_no', name=op.f('pk_user_consent_hist')),
    comment='사용자동의이력 — 동의 · 철회 변경 기록(R14)'
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table('user_consent_hist')
