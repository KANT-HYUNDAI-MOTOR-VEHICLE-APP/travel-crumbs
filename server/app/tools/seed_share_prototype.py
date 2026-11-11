"""share-prototype/impl의 5개 공개 로드맵을 PostgreSQL에 적재."""

import asyncio
import uuid
from datetime import datetime, timedelta, timezone

from sqlalchemy import text

from app.db.session import SessionLocal
from app.places.models import DataSrc, PlaceMst
from app.tools.seed_recommendation import find_or_add_user
from app.tools.share_prototype_seed_data import PROTOTYPE_ROADMAPS
from app.trips import route as route_calc
from app.trips.models import (
    TripMst,
    TripPlaceDtl,
    TripRouteDtl,
    TripStatus,
    TripStatusHist,
    Visibility,
)
from app.trips.repository import coord
from app.users.models import UserMst

UTC = timezone.utc
POI_PROVIDER = "SHARE_PROTOTYPE"


async def add_roadmap(session, seed: tuple, reviewers: list[UserMst]) -> bool:
    trip_no, source_id, trip_nm, region, rating_avg, rating_cnt, places = seed
    if await session.get(TripMst, trip_no) is not None:
        return False

    owner = await find_or_add_user(session, f"prototype_{source_id}", "Travel Crumbs")
    end_at = datetime(2026, 11, 9, 12, 0, tzinfo=UTC) + timedelta(minutes=trip_no)
    start_at = end_at - timedelta(hours=2)
    trip = TripMst(
        trip_no=trip_no,
        trip_uuid=str(uuid.uuid5(uuid.NAMESPACE_URL, f"travel-crumbs:prototype:{source_id}")),
        user_no=owner.user_no,
        trip_nm=trip_nm,
        trip_status_cd=TripStatus.COMPLETED,
        visibility_cd=Visibility.ANONYMOUS_PUBLIC,
        start_at=start_at,
        end_at=end_at,
        duration=7200,
        place_cnt=len(places),
        created_at=end_at,
        created_user_no=owner.user_no,
    )
    session.add(trip)
    await session.flush()
    session.add(
        TripStatusHist(
            trip_no=trip_no,
            trip_status_cd=TripStatus.COMPLETED,
            created_at=end_at,
            created_user_no=owner.user_no,
        )
    )

    route_coords = []
    for place_ord, place_seed in enumerate(places, start=1):
        place_no, poi_id, place_nm, addr, lat, lng, category, place_type_cd = place_seed
        place = await session.get(PlaceMst, place_no)
        if place is None:
            place = PlaceMst(
                place_no=place_no,
                place_nm=place_nm,
                place_type_cd=place_type_cd,
                lat=coord(lat),
                lng=coord(lng),
                data_src_cd=DataSrc.NAVI,
                poi_id=poi_id,
                poi_provider=POI_PROVIDER,
                created_at=end_at,
                created_user_no=1,
            )
            session.add(place)
            await session.flush()
        await session.execute(
            text("UPDATE place_mst SET addr = :addr WHERE place_no = :place_no"),
            {"addr": addr, "place_no": place_no},
        )
        route_coords.append((lat, lng))
        recorded_at = start_at + timedelta(minutes=place_ord * 30)
        trip_place = TripPlaceDtl(
            trip_no=trip_no,
            place_no=place_no,
            user_place_nm=place_nm,
            place_type_cd=place_type_cd,
            lat=coord(lat),
            lng=coord(lng),
            recorded_at=recorded_at,
            draft_yn="N",
            memo=f"{region} · {category}",
            created_at=recorded_at,
            created_user_no=owner.user_no,
        )
        session.add(trip_place)
        await session.flush()
        await session.execute(
            text(
                "UPDATE trip_place_dtl SET addr = :addr "
                "WHERE trip_place_no = :trip_place_no"
            ),
            {"addr": addr, "trip_place_no": trip_place.trip_place_no},
        )

    session.add(
        TripRouteDtl(
            trip_no=trip_no,
            route_data=route_calc.encode_polyline(route_coords),
            created_at=end_at,
            created_user_no=owner.user_no,
        )
    )
    for index in range(rating_cnt):
        reviewer = reviewers[index]
        rated_at = end_at + timedelta(minutes=index)
        await session.execute(
            text(
                "INSERT INTO trip_rating_dtl "
                "(trip_no, user_no, rating_score, rated_at, created_at, created_user_no) "
                "VALUES (:trip_no, :user_no, :rating_score, :rated_at, :created_at, :created_user_no) "
                "ON CONFLICT (trip_no, user_no) DO NOTHING"
            ),
            {
                "trip_no": trip_no,
                "user_no": reviewer.user_no,
                "rating_score": rating_avg,
                "rated_at": rated_at,
                "created_at": rated_at,
                "created_user_no": reviewer.user_no,
            },
        )
    return True


async def reset_identity_sequences(session) -> None:
    for table_nm, column_nm in (
        ("user_mst", "user_no"),
        ("place_mst", "place_no"),
        ("trip_mst", "trip_no"),
    ):
        await session.execute(
            text(
                "SELECT setval(pg_get_serial_sequence(:table_nm, :column_nm), "
                "GREATEST((SELECT max(" + column_nm + ") FROM " + table_nm + "), 1))"
            ),
            {"table_nm": table_nm, "column_nm": column_nm},
        )


async def main() -> None:
    async with SessionLocal() as session:
        if await session.get(UserMst, 1) is None:
            raise SystemExit("시스템 사용자가 없음 — seed_dev를 먼저 실행")
        max_rating_cnt = max(seed[5] for seed in PROTOTYPE_ROADMAPS)
        reviewers = [
            await find_or_add_user(
                session,
                f"reviewer{index:02d}",
                f"시연평가자{index:02d}",
            )
            for index in range(1, max_rating_cnt + 1)
        ]
        added = 0
        for seed in PROTOTYPE_ROADMAPS:
            added += int(await add_roadmap(session, seed, reviewers))
        await reset_identity_sequences(session)
        await session.commit()
        print(
            f"공유 프로토타입 공개 여행 {added}건 추가 · "
            f"전체 시드 {len(PROTOTYPE_ROADMAPS)}건"
        )


if __name__ == "__main__":
    asyncio.run(main())

