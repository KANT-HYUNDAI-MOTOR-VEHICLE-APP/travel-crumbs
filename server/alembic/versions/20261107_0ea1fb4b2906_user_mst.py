"""user_mst 사용자마스터 생성 — 시나리오 1 묶음 1(홈 A-03 닉네임 표시)

기준: ERD v0.3 user_mst · 표준정의서 v0.35

Revision ID: 0ea1fb4b2906
Revises: 
Create Date: 2026-11-07 16:26:36.889270

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '0ea1fb4b2906'
down_revision: Union[str, Sequence[str], None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table('user_mst',
    sa.Column('user_no', sa.BigInteger(), sa.Identity(always=False), nullable=False, comment='사용자번호'),
    sa.Column('login_id', sa.String(length=50), nullable=False, comment='로그인아이디'),
    sa.Column('password_hash', sa.String(length=255), nullable=False, comment='비밀번호해시 — 해시만 저장'),
    sa.Column('nickname_nm', sa.String(length=100), nullable=False, comment='닉네임명'),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False, comment='생성일시'),
    sa.Column('created_user_no', sa.BigInteger(), nullable=False, comment='생성사용자번호'),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True, comment='수정일시'),
    sa.Column('updated_user_no', sa.BigInteger(), nullable=True, comment='수정사용자번호'),
    sa.Column('deleted_yn', sa.CHAR(length=1), server_default='N', nullable=False, comment='삭제여부 — 논리 삭제면 Y'),
    sa.CheckConstraint("deleted_yn IN ('Y', 'N')", name=op.f('ck_user_mst_deleted_yn')),
    sa.ForeignKeyConstraint(['created_user_no'], ['user_mst.user_no'], name=op.f('fk_user_mst_created_user_no')),
    sa.ForeignKeyConstraint(['updated_user_no'], ['user_mst.user_no'], name=op.f('fk_user_mst_updated_user_no')),
    sa.PrimaryKeyConstraint('user_no', name=op.f('pk_user_mst')),
    sa.UniqueConstraint('login_id', name=op.f('uq_user_mst_login_id')),
    comment='사용자마스터 — 자연인 한 사람의 정보(1인 1행)'
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table('user_mst')
