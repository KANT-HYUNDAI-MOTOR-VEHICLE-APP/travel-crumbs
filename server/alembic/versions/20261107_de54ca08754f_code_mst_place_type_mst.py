"""공통 코드를 cd_mst 한 테이블로 · 장소유형을 개별 코드 테이블 place_type_mst 로 — 시나리오 1 묶음 5

결정 로그 D24(코드그룹명) · D25(통합 공통 코드 한 테이블 + 값이 많은 코드는 개별 코드 테이블)
- cd_grp_mst · cd_dtl → cd_mst(PK cd_no · 코드그룹명 + 코드 중복 불가). 장소 카테고리 코드는 place_type_mst 로 옮겨 cd_mst 에는 두지 않음
- place_type_mst: PK place_type_cd(개별 코드 테이블은 코드가 PK) · place_category_cd · place_category_nm
- trip_place_dtl · place_mst 의 place_type_cd 에 FK

Revision ID: de54ca08754f
Revises: cfcbbddc2430
Create Date: 2026-11-07 22:39:41.966951

"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "de54ca08754f"
down_revision: Union[str, Sequence[str], None] = "cfcbbddc2430"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "cd_mst",
        sa.Column(
            "cd_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="코드번호",
        ),
        sa.Column(
            "cd_grp_nm",
            sa.String(length=100),
            nullable=False,
            comment="코드그룹명 — 코드 목록의 이름(예: REVIEW_POLARITY)",
        ),
        sa.Column("cd", sa.String(length=30), nullable=False, comment="코드"),
        sa.Column(
            "cd_nm",
            sa.String(length=100),
            nullable=False,
            comment="코드명 — 화면에 보이는 글자",
        ),
        sa.Column("sort_ord", sa.Integer(), nullable=False, comment="정렬순서"),
        sa.Column(
            "use_yn",
            sa.CHAR(length=1),
            server_default="Y",
            nullable=False,
            comment="사용여부",
        ),
        *_common("cd_mst"),
        sa.CheckConstraint("use_yn IN ('Y', 'N')", name=op.f("ck_cd_mst_use_yn")),
        sa.PrimaryKeyConstraint("cd_no", name=op.f("pk_cd_mst")),
        sa.UniqueConstraint("cd_grp_nm", "cd", name="uq_cd_mst_cd_grp_nm_cd"),
        comment="코드마스터 — 통합 공통 코드(코드그룹명 + 코드 중복 불가)",
    )
    op.execute(
        """
        INSERT INTO cd_mst (cd_grp_nm, cd, cd_nm, sort_ord, use_yn, created_at, created_user_no)
        SELECT cd_grp_cd, cd, cd_nm, sort_ord, use_yn, created_at, created_user_no
        FROM cd_dtl
        WHERE cd_grp_cd NOT IN ('PLACE_TYPE', 'PLACE_CATEGORY')
        ORDER BY cd_grp_cd, sort_ord
        """
    )

    op.create_table(
        "place_type_mst",
        sa.Column(
            "place_type_cd",
            sa.String(length=30),
            nullable=False,
            comment="장소유형코드 — 예: BEACH",
        ),
        sa.Column(
            "place_type_nm",
            sa.String(length=100),
            nullable=False,
            comment="장소유형명 — 예: 바다·해변",
        ),
        sa.Column(
            "place_category_cd",
            sa.String(length=30),
            nullable=False,
            comment="장소카테고리코드 — 예: NATURE",
        ),
        sa.Column(
            "place_category_nm",
            sa.String(length=100),
            nullable=False,
            comment="장소카테고리명 — 예: 자연",
        ),
        sa.Column("sort_ord", sa.Integer(), nullable=False, comment="정렬순서"),
        sa.Column(
            "use_yn",
            sa.CHAR(length=1),
            server_default="Y",
            nullable=False,
            comment="사용여부",
        ),
        *_common("place_type_mst"),
        sa.CheckConstraint(
            "use_yn IN ('Y', 'N')", name=op.f("ck_place_type_mst_use_yn")
        ),
        sa.PrimaryKeyConstraint("place_type_cd", name=op.f("pk_place_type_mst")),
        comment="장소유형마스터 — 값이 많고 카테고리 속성이 있어 공통 코드에서 분리(결정 로그 D25)",
    )
    op.execute(
        """
        INSERT INTO place_type_mst (place_type_cd, place_type_nm, place_category_cd, place_category_nm,
                                    sort_ord, created_at, created_user_no)
        SELECT t.cd, t.cd_nm, t.category_cd, c.cd_nm, t.sort_ord, t.created_at, t.created_user_no
        FROM cd_dtl t
        JOIN cd_dtl c ON c.cd_grp_cd = 'PLACE_CATEGORY' AND c.cd = t.category_cd
        WHERE t.cd_grp_cd = 'PLACE_TYPE'
        """
    )
    op.create_foreign_key(
        op.f("fk_trip_place_dtl_place_type_cd"),
        "trip_place_dtl",
        "place_type_mst",
        ["place_type_cd"],
        ["place_type_cd"],
    )
    op.create_foreign_key(
        op.f("fk_place_mst_place_type_cd"),
        "place_mst",
        "place_type_mst",
        ["place_type_cd"],
        ["place_type_cd"],
    )
    op.alter_column(
        "trip_place_dtl",
        "place_type_cd",
        existing_type=sa.String(length=30),
        comment="장소유형코드 — 사용자가 고른 유형(결정 로그 D22)",
        existing_comment="장소유형코드 (PLACE_TYPE · 사용자가 고른 유형 · 결정 로그 D22)",
    )
    op.alter_column(
        "place_mst",
        "place_type_cd",
        existing_type=sa.String(length=30),
        comment="장소유형코드 — 내비 장소는 모름 → NULL(결정 로그 D22)",
        existing_comment="장소유형코드 (PLACE_TYPE · 내비 장소는 모름 → NULL · 결정 로그 D22)",
    )
    op.drop_table("cd_dtl")
    op.drop_table("cd_grp_mst")


def downgrade() -> None:
    raise NotImplementedError(
        "되돌리기 없음 — 이전 구조(cd_grp_mst · cd_dtl)로 돌아가려면 DB 를 새로 만들어야 함"
    )


def _common(table: str) -> list:
    """공통 컬럼 5개(R07) — 모델의 CommonColumns 와 같게."""
    return [
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
            "deleted_yn IN ('Y', 'N')", name=op.f(f"ck_{table}_deleted_yn")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f(f"fk_{table}_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f(f"fk_{table}_updated_user_no"),
        ),
    ]
