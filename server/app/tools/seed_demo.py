"""시연 데이터(가안) — 실행: uv run python -m app.tools.seed_demo  (여러 번 실행해도 같은 결과)

먼저 uv run python -m app.tools.seed_dev 로 샘플 사용자를 만든 뒤 실행.
- 페르소나 2명: sample01 = 지현(아이 둘 · 가족 여행), sample02 = 준호(혼자 드라이브)
- 내비 POI 로 확인된 장소(place_mst · DATA_SRC NAVI) — poi_id 는 시연용 가짜 번호(DEMO-…)
- 완료된 여행 2건 — 경로점 · 장소 경험 · 태그 · 상태 이력 · 간소화 경로까지 실제 저장 흐름과 같게
"""

import asyncio
import uuid
from datetime import datetime, timedelta, timezone

from sqlalchemy import select, update

from app.db.session import SessionLocal
from app.places.models import DataSrc, PlaceMst, TagMst
from app.trips import route as route_calc
from app.trips.models import (
    TripCompanionDtl,
    TripMst,
    TripPlaceDtl,
    TripPlaceTagRel,
    TripRouteDtl,
    TripRoutePointDtl,
    TripStatus,
    TripStatusHist,
    Visibility,
)
from app.trips.repository import coord
from app.users.models import UserMst

# 한국 표준시(UTC+9 · 서머타임 없음) — Windows 에는 시간대 DB 가 없어 고정 오프셋으로
KST = timezone(timedelta(hours=9))
POI_PROVIDER = "DEMO"

PERSONAS = {"sample01": "지현", "sample02": "준호"}

# (poi_id, 공식 장소명, 장소유형, 위도, 경도)
PLACES = [
    ("DEMO-001", "덕평자연휴게소(강릉방향)", "REST_AREA", 37.2405, 127.3816),
    ("DEMO-002", "강릉중앙시장", "MARKET", 37.7543, 128.8990),
    ("DEMO-003", "안목해변", "BEACH", 37.7720, 128.9470),
    ("DEMO-004", "경포호", "LAKE_RIVER", 37.7955, 128.8996),
    ("DEMO-005", "대관령휴게소(강릉방향)", "REST_AREA", 37.6886, 128.7558),
    ("DEMO-006", "정동진해변", "BEACH", 37.6907, 129.0342),
]

# 경로 꺾이는 지점(위도, 경도) — 사이를 촘촘히 채워 경로점으로 저장
WAY_SEOUL_GANGNEUNG = [
    (37.5172, 127.0473),
    (37.3966, 127.2546),
    (37.2405, 127.3816),
    (37.3420, 127.9480),
    (37.5450, 128.4440),
    (37.6886, 128.7558),
    (37.7543, 128.8990),
    (37.7720, 128.9470),
    (37.7955, 128.8996),
]
WAY_GANGNEUNG_JEONGDONGJIN = [
    (37.7519, 128.8761),
    (37.6886, 128.7558),
    (37.7200, 128.9200),
    (37.6907, 129.0342),
]

# 여행: (uuid, 사용자, 여행명, 동행자, 출발, 경로, 장소들)
# 장소: (poi_id, 부르는 이름, 도착까지 분, 후기, 태그, 메모)
TRIPS = [
    (
        "00000000-0000-4000-8000-000000000001",
        "sample01",
        "아이와 강릉 1박",
        ["SPOUSE", "CHILD"],
        datetime(2026, 10, 24, 8, 30, tzinfo=KST),
        WAY_SEOUL_GANGNEUNG,
        [
            (
                "DEMO-001",
                "덕평휴게소",
                70,
                "POSITIVE",
                ["화장실 깨끗해요", "아이와 가기 좋아요"],
                "키즈존 넓어서 30분 쉬어 감",
            ),
            (
                "DEMO-002",
                "중앙시장",
                230,
                "POSITIVE",
                ["웨이팅 있어요"],
                "닭강정 줄 30분, 주차는 공영주차장",
            ),
            (
                "DEMO-003",
                "안목해변",
                300,
                "POSITIVE",
                ["주차 편해요", "아이와 가기 좋아요"],
                "커피거리 바로 앞, 모래 고움",
            ),
            (
                "DEMO-004",
                "경포호",
                380,
                "NEGATIVE",
                ["웨이팅 있어요"],
                "주말 주차장 만차, 한 바퀴 돌고 나옴",
            ),
        ],
    ),
    (
        "00000000-0000-4000-8000-000000000002",
        "sample02",
        "정동진 혼자 드라이브",
        ["SOLO"],
        datetime(2026, 10, 31, 6, 0, tzinfo=KST),
        WAY_GANGNEUNG_JEONGDONGJIN,
        [
            (
                "DEMO-005",
                "대관령휴게소",
                25,
                "NEGATIVE",
                ["웨이팅 있어요"],
                "아침에도 화장실 줄 김",
            ),
            (
                "DEMO-006",
                "정동진",
                70,
                "POSITIVE",
                ["다시 가고 싶어요", "주차 편해요"],
                "해 뜨는 시간 맞춰 가면 좋음",
            ),
        ],
    ),
]


def fill(
    way: list[tuple[float, float]], step_m: float = 400.0
) -> list[tuple[float, float]]:
    """꺾이는 지점 사이를 step_m 간격으로 채움(앱이 20m 이상 움직일 때 찍는 경로점을 흉내)."""
    points = [way[0]]
    for a, b in zip(way, way[1:], strict=False):
        n = max(1, int(route_calc.distance_m(a, b) // step_m))
        points += [
            (a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n)
            for i in range(1, n + 1)
        ]
    return points


async def main() -> None:
    async with SessionLocal() as session:
        users = {
            u.login_id: u for u in (await session.execute(select(UserMst))).scalars()
        }
        missing = [login for login in PERSONAS if login not in users]
        if missing:
            raise SystemExit(
                f"샘플 사용자가 없음: {missing} — 먼저 uv run python -m app.tools.seed_dev"
            )
        for login, nickname in PERSONAS.items():
            await session.execute(
                update(UserMst)
                .where(UserMst.login_id == login)
                .values(nickname_nm=nickname)
            )

        places: dict[str, PlaceMst] = {}
        for poi_id, name, type_cd, lat, lng in PLACES:
            found = (
                await session.execute(
                    select(PlaceMst).where(
                        PlaceMst.poi_provider == POI_PROVIDER, PlaceMst.poi_id == poi_id
                    )
                )
            ).scalar_one_or_none()
            if found is None:
                found = PlaceMst(
                    place_nm=name,
                    place_type_cd=type_cd,
                    lat=coord(lat),
                    lng=coord(lng),
                    data_src_cd=DataSrc.NAVI,
                    poi_id=poi_id,
                    poi_provider=POI_PROVIDER,
                    created_user_no=1,
                )
                session.add(found)
                await session.flush()
            places[poi_id] = found
        tags = {
            t.tag_nm: t.tag_no
            for t in (await session.execute(select(TagMst))).scalars()
        }

        for trip_uuid, login, name, companions, start, way, visits in TRIPS:
            if (
                await session.execute(
                    select(TripMst).where(TripMst.trip_uuid == trip_uuid)
                )
            ).scalar_one_or_none():
                continue
            user_no = users[login].user_no
            coords = fill(way)
            dist = route_calc.total_distance_m(coords)
            end = start + timedelta(minutes=visits[-1][2] + 60)
            trip = TripMst(
                trip_uuid=trip_uuid,
                user_no=user_no,
                trip_nm=name,
                trip_status_cd=TripStatus.COMPLETED,
                visibility_cd=Visibility.PRIVATE,
                start_at=start,
                end_at=end,
                dist=dist,
                duration=int((end - start).total_seconds()),
                place_cnt=len(visits),
                created_user_no=user_no,
            )
            session.add(trip)
            await session.flush()
            session.add_all(
                TripCompanionDtl(
                    trip_no=trip.trip_no, companion_type_cd=c, created_user_no=user_no
                )
                for c in companions
            )
            step = (end - start) / len(coords)
            session.add_all(
                TripRoutePointDtl(
                    trip_no=trip.trip_no,
                    lat=coord(lat),
                    lng=coord(lng),
                    recorded_at=start + step * i,
                    created_user_no=user_no,
                )
                for i, (lat, lng) in enumerate(coords)
            )
            session.add(
                TripRouteDtl(
                    trip_no=trip.trip_no,
                    route_data=route_calc.encode_polyline(route_calc.simplify(coords)),
                    created_user_no=user_no,
                )
            )
            session.add_all(
                [
                    TripStatusHist(
                        trip_no=trip.trip_no,
                        trip_status_cd=TripStatus.RECORDING,
                        created_user_no=user_no,
                    ),
                    TripStatusHist(
                        trip_no=trip.trip_no,
                        trip_status_cd=TripStatus.COMPLETED,
                        created_user_no=user_no,
                    ),
                ]
            )
            for poi_id, user_place_nm, minutes, polarity, tag_names, memo in visits:
                place = places[poi_id]
                row = TripPlaceDtl(
                    trip_no=trip.trip_no,
                    place_no=place.place_no,
                    lat=place.lat,
                    lng=place.lng,
                    recorded_at=start + timedelta(minutes=minutes),
                    draft_yn="N",
                    user_place_nm=user_place_nm,
                    place_type_cd=place.place_type_cd,
                    review_polarity_cd=polarity,
                    memo=memo,
                    created_user_no=user_no,
                )
                session.add(row)
                await session.flush()
                session.add_all(
                    TripPlaceTagRel(
                        trip_place_no=row.trip_place_no,
                        tag_no=tags[t],
                        created_user_no=user_no,
                    )
                    for t in tag_names
                )
            print(
                f"여행 추가: {name} ({login}) · 경로점 {len(coords)}개 · {dist / 1000:.1f}km · 장소 {len(visits)}개"
            )
        await session.commit()

        rows = (
            await session.execute(select(UserMst).order_by(UserMst.user_no))
        ).scalars()
        for u in rows:
            print(
                f"user_no={u.user_no}  login_id={u.login_id}  nickname_nm={u.nickname_nm}"
            )


if __name__ == "__main__":
    asyncio.run(main())
