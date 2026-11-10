"""trip_mst 여행마스터 · trip_companion_dtl 여행동행자내역 · trip_status_hist 여행상태이력 생성 — 시나리오 1 묶음 3(여행 시작 설정 A-04)

기준: ERD v0.3 trip_mst · trip_companion_dtl · trip_status_hist

Revision ID: 9093632c7fc5
Revises: 85961b000b3b
Create Date: 2026-11-07 17:46:53.789214

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '9093632c7fc5'
down_revision: Union[str, Sequence[str], None] = '85961b000b3b'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table('trip_mst',
    sa.Column('trip_no', sa.BigInteger(), sa.Identity(always=False), nullable=False, comment='여행번호'),
    sa.Column('trip_uuid', sa.CHAR(length=36), nullable=False, comment='여행UUID — 외부 노출 · 동기화'),
    sa.Column('user_no', sa.BigInteger(), nullable=False, comment='사용자번호 — 소유자'),
    sa.Column('trip_nm', sa.String(length=100), nullable=False, comment='여행명 — 날짜 · 지역 자동 생성 후 수정 가능'),
    sa.Column('trip_status_cd', sa.String(length=30), nullable=False, comment='여행상태코드 (TRIP_STATUS)'),
    sa.Column('visibility_cd', sa.String(length=30), nullable=False, comment='공개범위코드 (VISIBILITY)'),
    sa.Column('start_at', sa.DateTime(timezone=True), nullable=False, comment='시작일시'),
    sa.Column('end_at', sa.DateTime(timezone=True), nullable=True, comment='종료일시'),
    sa.Column('dist', sa.Integer(), nullable=True, comment='거리(m) — 반정규화: 완료 시 계산'),
    sa.Column('duration', sa.Integer(), nullable=True, comment='소요시간(초) — 반정규화: 완료 시 계산'),
    sa.Column('place_cnt', sa.Integer(), server_default='0', nullable=False, comment='장소개수 — 반정규화: 여행장소내역 건수'),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False, comment='생성일시'),
    sa.Column('created_user_no', sa.BigInteger(), nullable=False, comment='생성사용자번호'),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True, comment='수정일시'),
    sa.Column('updated_user_no', sa.BigInteger(), nullable=True, comment='수정사용자번호'),
    sa.Column('deleted_yn', sa.CHAR(length=1), server_default='N', nullable=False, comment='삭제여부 — 논리 삭제면 Y'),
    sa.CheckConstraint("deleted_yn IN ('Y', 'N')", name=op.f('ck_trip_mst_deleted_yn')),
    sa.ForeignKeyConstraint(['created_user_no'], ['user_mst.user_no'], name=op.f('fk_trip_mst_created_user_no')),
    sa.ForeignKeyConstraint(['updated_user_no'], ['user_mst.user_no'], name=op.f('fk_trip_mst_updated_user_no')),
    sa.ForeignKeyConstraint(['user_no'], ['user_mst.user_no'], name=op.f('fk_trip_mst_user_no')),
    sa.PrimaryKeyConstraint('trip_no', name=op.f('pk_trip_mst')),
    sa.UniqueConstraint('trip_uuid', name=op.f('uq_trip_mst_trip_uuid')),
    comment='여행마스터 — 기록 시작~종료 한 번의 이동'
    )
    op.create_table('trip_companion_dtl',
    sa.Column('trip_companion_no', sa.BigInteger(), sa.Identity(always=False), nullable=False, comment='여행동행자번호'),
    sa.Column('trip_no', sa.BigInteger(), nullable=False, comment='여행번호'),
    sa.Column('companion_type_cd', sa.String(length=30), nullable=False, comment='동행자유형코드 (COMPANION_TYPE)'),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False, comment='생성일시'),
    sa.Column('created_user_no', sa.BigInteger(), nullable=False, comment='생성사용자번호'),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True, comment='수정일시'),
    sa.Column('updated_user_no', sa.BigInteger(), nullable=True, comment='수정사용자번호'),
    sa.Column('deleted_yn', sa.CHAR(length=1), server_default='N', nullable=False, comment='삭제여부 — 논리 삭제면 Y'),
    sa.CheckConstraint("deleted_yn IN ('Y', 'N')", name=op.f('ck_trip_companion_dtl_deleted_yn')),
    sa.ForeignKeyConstraint(['created_user_no'], ['user_mst.user_no'], name=op.f('fk_trip_companion_dtl_created_user_no')),
    sa.ForeignKeyConstraint(['trip_no'], ['trip_mst.trip_no'], name=op.f('fk_trip_companion_dtl_trip_no')),
    sa.ForeignKeyConstraint(['updated_user_no'], ['user_mst.user_no'], name=op.f('fk_trip_companion_dtl_updated_user_no')),
    sa.PrimaryKeyConstraint('trip_companion_no', name=op.f('pk_trip_companion_dtl')),
    comment='여행동행자내역 — 여행 1건의 동행자 유형 목록'
    )
    op.create_table('trip_status_hist',
    sa.Column('trip_status_hist_no', sa.BigInteger(), sa.Identity(always=False), nullable=False, comment='여행상태이력번호'),
    sa.Column('trip_no', sa.BigInteger(), nullable=False, comment='여행번호'),
    sa.Column('trip_status_cd', sa.String(length=30), nullable=False, comment='여행상태코드 — 변경 후 상태. 변경 시각 = created_at'),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False, comment='생성일시'),
    sa.Column('created_user_no', sa.BigInteger(), nullable=False, comment='생성사용자번호'),
    sa.Column('updated_at', sa.DateTime(timezone=True), nullable=True, comment='수정일시'),
    sa.Column('updated_user_no', sa.BigInteger(), nullable=True, comment='수정사용자번호'),
    sa.Column('deleted_yn', sa.CHAR(length=1), server_default='N', nullable=False, comment='삭제여부 — 논리 삭제면 Y'),
    sa.CheckConstraint("deleted_yn IN ('Y', 'N')", name=op.f('ck_trip_status_hist_deleted_yn')),
    sa.ForeignKeyConstraint(['created_user_no'], ['user_mst.user_no'], name=op.f('fk_trip_status_hist_created_user_no')),
    sa.ForeignKeyConstraint(['trip_no'], ['trip_mst.trip_no'], name=op.f('fk_trip_status_hist_trip_no')),
    sa.ForeignKeyConstraint(['updated_user_no'], ['user_mst.user_no'], name=op.f('fk_trip_status_hist_updated_user_no')),
    sa.PrimaryKeyConstraint('trip_status_hist_no', name=op.f('pk_trip_status_hist')),
    comment='여행상태이력 — 여행 상태 변경 기록'
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table('trip_status_hist')
    op.drop_table('trip_companion_dtl')
    op.drop_table('trip_mst')
