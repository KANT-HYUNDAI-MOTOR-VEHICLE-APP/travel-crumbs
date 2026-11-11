"""share-prototype/impl 시드 적재 결과 확인."""

import asyncio

from sqlalchemy import text

from app.db.session import SessionLocal


async def main() -> None:
    async with SessionLocal() as session:
        result = (
            await session.execute(
                text(
                    r"""
                    SELECT
                        count(DISTINCT t.trip_no) AS trip_cnt,
                        count(DISTINCT tp.trip_place_no) AS trip_place_cnt,
                        count(DISTINCT tp.place_no) AS place_cnt,
                        count(DISTINCT r.trip_rating_no) AS rating_cnt
                    FROM trip_mst t
                    JOIN user_mst u ON u.user_no = t.user_no
                    JOIN trip_place_dtl tp ON tp.trip_no = t.trip_no
                    LEFT JOIN trip_rating_dtl r ON r.trip_no = t.trip_no
                    WHERE u.login_id LIKE 'prototype\_%' ESCAPE '\\'
                    """
                )
            )
        ).one()
        expected = (5, 11, 9, 90)
        actual = tuple(result)
        if actual != expected:
            raise SystemExit(
                f"공유 프로토타입 시드 불일치: actual={actual}, expected={expected}"
            )
        print("공유 프로토타입 시드 확인 완료: 여행 5 · 여행장소 11 · 장소 9 · 평점 90")


if __name__ == "__main__":
    asyncio.run(main())
