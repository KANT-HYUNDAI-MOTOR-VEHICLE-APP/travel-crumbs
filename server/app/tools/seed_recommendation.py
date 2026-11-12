"""추천 브랜치의 공개 여행 시드를 PostgreSQL에 적재.

실행 순서:
    uv run python -m app.tools.seed_dev
    uv run python -m app.tools.seed_recommendation

같은 trip_no가 있으면 건너뛰므로 반복 실행 가능.
"""

import asyncio
import uuid
from datetime import datetime, timedelta, timezone

from sqlalchemy import select, text

from app.db.session import SessionLocal
from app.places.models import DataSrc, PlaceMst
from app.tools.recommendation_seed_data import PUBLIC_TRIPS, TAG_NOS
from app.trips import route as route_calc
from app.trips.models import (
    TripCompanionDtl,
    TripMst,
    TripPlaceDtl,
    TripPlaceTagRel,
    TripRouteDtl,
    TripStatus,
    TripStatusHist,
    Visibility,
)
from app.trips.repository import coord
from app.users.models import UserMst

UTC = timezone.utc
POI_PROVIDER = "RECOMMENDATION_SEED"


async def find_or_add_user(session, login_id: str, nickname_nm: str) -> UserMst:
    found = (
        await session.execute(select(UserMst).where(UserMst.login_id == login_id))
    ).scalar_one_or_none()
    if found is not None:
        return found
    found = UserMst(
        login_id=login_id,
        password_hash="!",
        nickname_nm=nickname_nm,
        created_user_no=1,
    )
    session.add(found)
    await session.flush()
    return found


async def add_public_trip(session, seed: tuple, reviewers: list[UserMst]) -> bool:
    trip_no, trip_nm, nickname, companions, rating_avg, rating_cnt, published_ms, places = seed
    existing = await session.get(TripMst, trip_no)
    if existing is not None:
        return False

    owner = await find_or_add_user(session, f"reco_{trip_no}", nickname)
    published_at = datetime.fromtimestamp(published_ms / 1000, tz=UTC)
    start_at = published_at - timedelta(hours=2)
    trip = TripMst(
        trip_no=trip_no,
        trip_uuid=str(uuid.uuid5(uuid.NAMESPACE_URL, f"travel-crumbs:trip:{trip_no}")),
        user_no=owner.user_no,
        trip_nm=trip_nm,
        trip_status_cd=TripStatus.COMPLETED,
        visibility_cd=Visibility.ANONYMOUS_PUBLIC,
        start_at=start_at,
        end_at=published_at,
        duration=7200,
        place_cnt=len(places),
        created_at=published_at,
        created_user_no=owner.user_no,
    )
    session.add(trip)
    await session.flush()

    session.add_all(
        TripCompanionDtl(
            trip_no=trip_no,
            companion_type_cd=companion,
            created_at=published_at,
            created_user_no=owner.user_no,
        )
        for companion in companions
    )
    session.add(
        TripStatusHist(
            trip_no=trip_no,
            trip_status_cd=TripStatus.COMPLETED,
            created_at=published_at,
            created_user_no=owner.user_no,
        )
    )

    route_coords = []
    for place_ord, place_seed in enumerate(places, start=1):
        place_no, place_nm, addr, lat, lng, tag_symbols = place_seed
        place = await session.get(PlaceMst, place_no)
        if place is None:
            place = PlaceMst(
                place_no=place_no,
                place_nm=place_nm,
                lat=coord(lat),
                lng=coord(lng),
                data_src_cd=DataSrc.NAVI,
                poi_id=f"RECO-{place_no}",
                poi_provider=POI_PROVIDER,
                created_at=published_at,
                created_user_no=1,
            )
            session.add(place)
            await session.flush()
        await session.execute(
            text("UPDATE place_mst SET addr = :addr WHERE place_no = :place_no"),
            {"addr": addr, "place_no": place_no},
        )
        route_coords.append((lat, lng))
        recorded_at = start_at + timedelta(minutes=place_ord * 20)
        trip_place = TripPlaceDtl(
            trip_no=trip_no,
            place_no=place_no,
            user_place_nm=place_nm,
            lat=coord(lat),
            lng=coord(lng),
            recorded_at=recorded_at,
            draft_yn="N",
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
        session.add_all(
            TripPlaceTagRel(
                trip_place_no=trip_place.trip_place_no,
                tag_no=TAG_NOS[symbol],
                created_at=recorded_at,
                created_user_no=owner.user_no,
            )
            for symbol in tag_symbols
        )

    session.add(
        TripRouteDtl(
            trip_no=trip_no,
            route_data=route_calc.encode_polyline(route_coords),
            created_at=published_at,
            created_user_no=owner.user_no,
        )
    )
    for index in range(rating_cnt):
        reviewer = reviewers[index]
        rated_at = published_at + timedelta(minutes=index)
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
        system = await session.get(UserMst, 1)
        if system is None:
            raise SystemExit("시스템 사용자가 없음 — seed_dev를 먼저 실행")
        reviewers = [
            await find_or_add_user(session, f"reviewer{index:02d}", f"시연평가자{index:02d}")
            for index in range(1, 25)
        ]
        added = 0
        for seed in PUBLIC_TRIPS:
            added += int(await add_public_trip(session, seed, reviewers))
        await reset_identity_sequences(session)
        await session.commit()
        print(f"공개 여행 {added}건 추가 · 전체 시드 {len(PUBLIC_TRIPS)}건")


if __name__ == "__main__":
    asyncio.run(main())
