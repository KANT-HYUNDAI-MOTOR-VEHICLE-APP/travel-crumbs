"""cd_grp_mst 코드그룹마스터 · cd_dtl 코드내역 생성 + 코드값 — 시나리오 1 묶음 3(여행 시작 설정 A-04)

기준: ERD v0.3 cd_grp_mst · cd_dtl · 표준정의서 v0.35 6절 코드 · R10
코드값은 개발용이 아니라 앱이 늘 쓰는 기준 데이터라 시드가 아닌 마이그레이션에 넣음(결정 로그 D14).

Revision ID: 85961b000b3b
Revises: 208944eba6ea
Create Date: 2026-11-07 17:46:52.300063

"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "85961b000b3b"
down_revision: Union[str, Sequence[str], None] = "208944eba6ea"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


# 코드값을 넣는 사람 = 시스템 계정(user_no 1). created_user_no 가 NOT NULL 이라 먼저 있어야 함.
SYSTEM_USER_SQL = [
    """
    INSERT INTO user_mst (user_no, login_id, password_hash, nickname_nm, created_user_no)
    OVERRIDING SYSTEM VALUE
    VALUES (1, 'system', '!', '시스템', 1)
    ON CONFLICT (login_id) DO NOTHING
    """,
    "SELECT setval(pg_get_serial_sequence('user_mst', 'user_no'), "
    "GREATEST((SELECT max(user_no) FROM user_mst), 1))",
]

# 시나리오 1에서 지금까지 쓰는 그룹만 — 다음 화면에서 필요한 그룹(PLACE_TYPE 등)은 그 묶음에서 추가
CODE_GROUPS = [
    ("TRIP_STATUS", "여행 상태"),
    ("VISIBILITY", "공개 범위"),
    ("COMPANION_TYPE", "동행자 유형"),
    ("CONSENT_TYPE", "동의 유형"),
]

CODES = [
    ("TRIP_STATUS", "RECORDING", "기록 중", 1),
    ("TRIP_STATUS", "COMPLETED", "완료", 2),
    ("TRIP_STATUS", "INTERRUPTED", "중단", 3),
    ("TRIP_STATUS", "CANCELED", "취소", 4),
    ("VISIBILITY", "PRIVATE", "나만 보기", 1),
    ("VISIBILITY", "ANONYMOUS_PUBLIC", "익명 공개", 2),
    ("VISIBILITY", "DIRECT_SHARE", "직접 공유만", 3),
    ("COMPANION_TYPE", "SOLO", "혼자", 1),
    ("COMPANION_TYPE", "SPOUSE", "배우자", 2),  # M02 확정 — 결정 로그 D16
    ("COMPANION_TYPE", "CHILD", "아이", 3),  # M02 확정 — 결정 로그 D16
    ("COMPANION_TYPE", "PARENT", "부모님", 4),
    ("COMPANION_TYPE", "PET", "반려동물", 5),
    ("CONSENT_TYPE", "LOCATION_COLLECT", "위치 수집", 1),
    ("CONSENT_TYPE", "RECO_PUBLIC", "추천 공개", 2),
]


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        "cd_grp_mst",
        sa.Column(
            "cd_grp_cd", sa.String(length=30), nullable=False, comment="코드그룹코드"
        ),
        sa.Column(
            "cd_grp_nm", sa.String(length=100), nullable=False, comment="코드그룹명"
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
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_cd_grp_mst_deleted_yn")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_cd_grp_mst_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_cd_grp_mst_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint("cd_grp_cd", name=op.f("pk_cd_grp_mst")),
        comment="코드그룹마스터 — 코드 그룹(R10)",
    )
    op.create_table(
        "cd_dtl",
        sa.Column(
            "cd_grp_cd", sa.String(length=30), nullable=False, comment="코드그룹코드"
        ),
        sa.Column("cd", sa.String(length=30), nullable=False, comment="코드"),
        sa.Column("cd_nm", sa.String(length=100), nullable=False, comment="코드명"),
        sa.Column("sort_ord", sa.Integer(), nullable=False, comment="정렬순서"),
        sa.Column(
            "use_yn",
            sa.CHAR(length=1),
            server_default="Y",
            nullable=False,
            comment="사용여부",
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
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_cd_dtl_deleted_yn")
        ),
        sa.CheckConstraint("use_yn IN ('Y', 'N')", name=op.f("ck_cd_dtl_use_yn")),
        sa.ForeignKeyConstraint(
            ["cd_grp_cd"], ["cd_grp_mst.cd_grp_cd"], name=op.f("fk_cd_dtl_cd_grp_cd")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_cd_dtl_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_cd_dtl_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint("cd_grp_cd", "cd", name=op.f("pk_cd_dtl")),
        comment="코드내역 — 코드 그룹에 딸린 코드값",
    )

    for sql in SYSTEM_USER_SQL:
        op.execute(sql)
    grp = sa.table(
        "cd_grp_mst",
        sa.column("cd_grp_cd"),
        sa.column("cd_grp_nm"),
        sa.column("created_user_no"),
    )
    op.bulk_insert(
        grp,
        [
            {"cd_grp_cd": g, "cd_grp_nm": n, "created_user_no": 1}
            for g, n in CODE_GROUPS
        ],
    )
    dtl = sa.table(
        "cd_dtl",
        sa.column("cd_grp_cd"),
        sa.column("cd"),
        sa.column("cd_nm"),
        sa.column("sort_ord"),
        sa.column("created_user_no"),
    )
    op.bulk_insert(
        dtl,
        [
            {"cd_grp_cd": g, "cd": c, "cd_nm": n, "sort_ord": o, "created_user_no": 1}
            for g, c, n, o in CODES
        ],
    )


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_table("cd_dtl")
    op.drop_table("cd_grp_mst")
