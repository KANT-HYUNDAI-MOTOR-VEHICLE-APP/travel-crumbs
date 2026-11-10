"""PostgreSQL 공통 추천 시드 건수·표준 태그 검증."""

import asyncio

from sqlalchemy import text

from app.db.session import SessionLocal

EXPECTED_COUNTS = {
    "public_trip_cnt": 25,
    "trip_place_cnt": 91,
    "place_cnt": 90,
    "rating_cnt": 287,
}
EXPECTED_TAGS = {
    1: "주차 편해요",
    2: "아이와 가기 좋아요",
    3: "화장실 깨끗해요",
    4: "다시 가고 싶어요",
    5: "웨이팅 있어요",
    6: "주차 어려워요",
    7: "유모차 이동 편해요",
    8: "수유실 있어요",
    9: "기저귀 교환대 있어요",
    10: "쉬어가기 좋아요",
    11: "붐벼요",
    12: "아이 메뉴 있어요",
    13: "음식이 맛있어요",
}


async def main() -> None:
    async with SessionLocal() as session:
        row = (
            await session.execute(
                text(
                    """
                    SELECT
                      count(DISTINCT t.trip_no) AS public_trip_cnt,
                      count(DISTINCT tp.trip_place_no) AS trip_place_cnt,
                      count(DISTINCT p.place_no) AS place_cnt,
                      count(DISTINCT r.trip_rating_no) AS rating_cnt
                    FROM trip_mst t
                    JOIN user_mst u ON u.user_no = t.user_no
                    LEFT JOIN trip_place_dtl tp ON tp.trip_no = t.trip_no
                    LEFT JOIN place_mst p
                      ON p.place_no = tp.place_no
                     AND p.poi_provider = 'RECOMMENDATION_SEED'
                    LEFT JOIN trip_rating_dtl r ON r.trip_no = t.trip_no
                    WHERE t.visibility_cd = 'ANONYMOUS_PUBLIC'
                      AND u.login_id LIKE 'reco_%'
                      AND t.deleted_yn = 'N'
                    """
                )
            )
        ).one()
        actual_counts = dict(row._mapping)
        tags = dict(
            (
                await session.execute(
                    text(
                        "SELECT tag_no, tag_nm FROM tag_mst "
                        "WHERE tag_no BETWEEN 1 AND 13 ORDER BY tag_no"
                    )
                )
            ).all()
        )

    if actual_counts != EXPECTED_COUNTS:
        raise SystemExit(f"시드 건수 불일치: {actual_counts} != {EXPECTED_COUNTS}")
    if tags != EXPECTED_TAGS:
        raise SystemExit(f"표준 태그 불일치: {tags} != {EXPECTED_TAGS}")
    print(f"추천 시드 검증 완료: {actual_counts}")


if __name__ == "__main__":
    asyncio.run(main())
