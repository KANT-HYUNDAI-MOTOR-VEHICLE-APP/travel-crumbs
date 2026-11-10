"""place_mst 장소마스터 · tag_mst 태그마스터 · trip_place_tag_rel 생성, trip_place_dtl · cd_dtl 컬럼 추가 + 코드 · 태그 — 시나리오 1 묶음 5(장소 경험 입력 A-06)

기준: ERD v0.3 · 결정 로그 D19(user_place_nm) · D20(poi_id · poi_provider · NAVI) · D21(POSITIVE · NEGATIVE) · D22(place_type_cd) · D23(카테고리)

Revision ID: cfcbbddc2430
Revises: 11cbbcc6b3bd
Create Date: 2026-11-07 22:07:37.825794

"""

from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "cfcbbddc2430"
down_revision: Union[str, Sequence[str], None] = "11cbbcc6b3bd"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


# 묶음 5에서 쓰는 코드 그룹 — 편의시설(FACILITY · FACILITY_CATEGORY)은 place_facility_dtl 을 만들 때 추가
CODE_GROUPS = [
    ("PLACE_CATEGORY", "장소 카테고리"),
    ("PLACE_TYPE", "장소 유형"),
    ("REVIEW_POLARITY", "후기 성격"),
    ("DATA_SRC", "데이터 출처"),
]

PLACE_CATEGORIES = [
    ("MOBILITY", "이동·차량"),
    ("FOOD", "음식"),
    ("NATURE", "자연"),
    ("SIGHTSEEING", "관광"),
    ("CULTURE", "문화"),
    ("SHOPPING", "쇼핑"),
    ("LEISURE", "체험·레저"),
    ("STAY", "숙박"),
    ("ETC", "기타"),
]

# (코드, 코드명, 카테고리)
PLACE_TYPES = [
    ("REST_AREA", "휴게소", "MOBILITY"),
    ("DROWSY_SHELTER", "졸음쉼터", "MOBILITY"),
    ("GAS_STATION", "주유소", "MOBILITY"),
    ("EV_STATION", "전기차 충전소", "MOBILITY"),
    ("PARKING", "주차장", "MOBILITY"),
    ("RESTAURANT", "음식점", "FOOD"),
    ("CAFE", "카페", "FOOD"),
    ("BEACH", "바다·해변", "NATURE"),
    ("MOUNTAIN", "산", "NATURE"),
    ("VALLEY", "계곡", "NATURE"),
    ("LAKE_RIVER", "호수·강", "NATURE"),
    ("ISLAND", "섬", "NATURE"),
    ("PARK", "공원·수목원", "NATURE"),
    ("ATTRACTION", "명소·랜드마크", "SIGHTSEEING"),
    ("VIEWPOINT", "전망대", "SIGHTSEEING"),
    ("MUSEUM", "박물관", "CULTURE"),
    ("GALLERY", "미술관·전시", "CULTURE"),
    ("MARKET", "전통시장", "SHOPPING"),
    ("MALL", "쇼핑몰·아웃렛", "SHOPPING"),
    ("EXPERIENCE", "체험시설", "LEISURE"),
    ("THEME_PARK", "테마파크", "LEISURE"),
    ("CAMPING", "캠핑장", "LEISURE"),
    ("LODGING", "숙소", "STAY"),
    ("ETC_PLACE", "기타", "ETC"),
]

OTHER_CODES = [
    ("REVIEW_POLARITY", "POSITIVE", "좋았어요", 1),
    ("REVIEW_POLARITY", "NEGATIVE", "별로예요", 2),
    ("DATA_SRC", "PUBLIC_API", "공공데이터", 1),
    ("DATA_SRC", "USER", "사용자 입력", 2),
    ("DATA_SRC", "NAVI", "내비 POI", 3),
]

# 경험 태그 — 주의점 · 팁도 태그로(결정 로그 D21)
TAGS = ["주차 편함", "아이 동반 좋음", "화장실 깨끗", "재방문", "웨이팅 있음"]


def upgrade() -> None:
    """Upgrade schema."""
    op.create_table(
        "place_mst",
        sa.Column(
            "place_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="장소번호",
        ),
        sa.Column(
            "place_nm",
            sa.String(length=100),
            nullable=False,
            comment="장소명 — 공식 명칭",
        ),
        sa.Column(
            "place_type_cd",
            sa.String(length=30),
            nullable=True,
            comment="장소유형코드 (PLACE_TYPE · 내비 장소는 모름 → NULL · 결정 로그 D22)",
        ),
        sa.Column(
            "lat", sa.Numeric(precision=10, scale=7), nullable=False, comment="위도"
        ),
        sa.Column(
            "lng", sa.Numeric(precision=10, scale=7), nullable=False, comment="경도"
        ),
        sa.Column(
            "data_src_cd",
            sa.String(length=30),
            nullable=False,
            comment="데이터출처코드 (DATA_SRC)",
        ),
        sa.Column(
            "poi_id",
            sa.String(length=50),
            nullable=True,
            comment="POI번호 — 지도 회사의 장소 고유번호(D20)",
        ),
        sa.Column(
            "poi_provider",
            sa.String(length=30),
            nullable=True,
            comment="POI제공자 — POI번호를 만든 지도 회사(D20)",
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
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_place_mst_deleted_yn")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_place_mst_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_place_mst_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint("place_no", name=op.f("pk_place_mst")),
        sa.UniqueConstraint(
            "poi_provider", "poi_id", name="uq_place_mst_poi_provider_poi_id"
        ),
        comment="장소마스터 — 내비 POI · 공공데이터로 확인된 공식 장소(1곳 1행 · 결정 로그 D19)",
    )
    op.create_table(
        "tag_mst",
        sa.Column(
            "tag_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="태그번호",
        ),
        sa.Column("tag_nm", sa.String(length=100), nullable=False, comment="태그명"),
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
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_tag_mst_deleted_yn")
        ),
        sa.CheckConstraint("use_yn IN ('Y', 'N')", name=op.f("ck_tag_mst_use_yn")),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_tag_mst_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_tag_mst_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint("tag_no", name=op.f("pk_tag_mst")),
        comment="태그마스터 — 경험 키워드(주의점 · 팁 포함 · 결정 로그 D21)",
    )
    op.create_table(
        "trip_place_tag_rel",
        sa.Column(
            "trip_place_tag_no",
            sa.BigInteger(),
            sa.Identity(always=False),
            nullable=False,
            comment="여행장소태그번호",
        ),
        sa.Column(
            "trip_place_no", sa.BigInteger(), nullable=False, comment="여행장소번호"
        ),
        sa.Column("tag_no", sa.BigInteger(), nullable=False, comment="태그번호"),
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
            "deleted_yn IN ('Y', 'N')", name=op.f("ck_trip_place_tag_rel_deleted_yn")
        ),
        sa.ForeignKeyConstraint(
            ["created_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_place_tag_rel_created_user_no"),
        ),
        sa.ForeignKeyConstraint(
            ["tag_no"], ["tag_mst.tag_no"], name=op.f("fk_trip_place_tag_rel_tag_no")
        ),
        sa.ForeignKeyConstraint(
            ["trip_place_no"],
            ["trip_place_dtl.trip_place_no"],
            name=op.f("fk_trip_place_tag_rel_trip_place_no"),
        ),
        sa.ForeignKeyConstraint(
            ["updated_user_no"],
            ["user_mst.user_no"],
            name=op.f("fk_trip_place_tag_rel_updated_user_no"),
        ),
        sa.PrimaryKeyConstraint(
            "trip_place_tag_no", name=op.f("pk_trip_place_tag_rel")
        ),
        sa.UniqueConstraint(
            "trip_place_no", "tag_no", name="uq_trip_place_tag_rel_trip_place_no_tag_no"
        ),
        comment="여행장소태그관계 — 여행장소내역과 태그의 N:M",
    )
    op.add_column(
        "cd_dtl",
        sa.Column(
            "category_cd",
            sa.String(length=30),
            nullable=True,
            comment="카테고리코드 — 이 코드가 속한 카테고리 그룹의 코드(예: PLACE_TYPE BEACH → PLACE_CATEGORY NATURE · 결정 로그 D23)",
        ),
    )
    op.add_column(
        "trip_place_dtl",
        sa.Column(
            "user_place_nm",
            sa.String(length=100),
            nullable=True,
            comment="사용자장소명 — 사용자가 부르는 이름(결정 로그 D19)",
        ),
    )
    op.add_column(
        "trip_place_dtl",
        sa.Column(
            "place_type_cd",
            sa.String(length=30),
            nullable=True,
            comment="장소유형코드 (PLACE_TYPE · 사용자가 고른 유형 · 결정 로그 D22)",
        ),
    )
    op.alter_column(
        "trip_place_dtl",
        "place_no",
        existing_type=sa.BIGINT(),
        comment="장소번호 — 공식 장소(place_mst)로 확인 전에는 NULL",
        existing_comment="장소번호 — 정차 후 장소 연결 전에는 NULL",
        existing_nullable=True,
    )
    op.create_foreign_key(
        op.f("fk_trip_place_dtl_place_no"),
        "trip_place_dtl",
        "place_mst",
        ["place_no"],
        ["place_no"],
    )

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
        sa.column("category_cd"),
        sa.column("created_user_no"),
    )
    rows = [
        {
            "cd_grp_cd": "PLACE_CATEGORY",
            "cd": c,
            "cd_nm": n,
            "sort_ord": i,
            "category_cd": None,
        }
        for i, (c, n) in enumerate(PLACE_CATEGORIES, 1)
    ]
    rows += [
        {
            "cd_grp_cd": "PLACE_TYPE",
            "cd": c,
            "cd_nm": n,
            "sort_ord": i,
            "category_cd": cat,
        }
        for i, (c, n, cat) in enumerate(PLACE_TYPES, 1)
    ]
    rows += [
        {"cd_grp_cd": g, "cd": c, "cd_nm": n, "sort_ord": o, "category_cd": None}
        for g, c, n, o in OTHER_CODES
    ]
    op.bulk_insert(dtl, [{**r, "created_user_no": 1} for r in rows])
    tag = sa.table("tag_mst", sa.column("tag_nm"), sa.column("created_user_no"))
    op.bulk_insert(tag, [{"tag_nm": t, "created_user_no": 1} for t in TAGS])


def downgrade() -> None:
    """Downgrade schema."""
    groups = ", ".join(f"'{g}'" for g, _ in CODE_GROUPS)
    op.execute(f"DELETE FROM cd_dtl WHERE cd_grp_cd IN ({groups})")
    op.execute(f"DELETE FROM cd_grp_mst WHERE cd_grp_cd IN ({groups})")
    op.drop_constraint(
        op.f("fk_trip_place_dtl_place_no"), "trip_place_dtl", type_="foreignkey"
    )
    op.alter_column(
        "trip_place_dtl",
        "place_no",
        existing_type=sa.BIGINT(),
        comment="장소번호 — 정차 후 장소 연결 전에는 NULL",
        existing_comment="장소번호 — 공식 장소(place_mst)로 확인 전에는 NULL",
        existing_nullable=True,
    )
    op.drop_column("trip_place_dtl", "place_type_cd")
    op.drop_column("trip_place_dtl", "user_place_nm")
    op.drop_column("cd_dtl", "category_cd")
    op.drop_table("trip_place_tag_rel")
    op.drop_table("tag_mst")
    op.drop_table("place_mst")
