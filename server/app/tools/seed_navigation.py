"""내비 브랜치 places.json을 PostgreSQL 장소마스터로 통합."""

import asyncio

from sqlalchemy import select, text

from app.db.session import SessionLocal
from app.places.models import DataSrc, PlaceMst
from app.tools.navigation_seed_data import NAVIGATION_PLACES
from app.trips.repository import coord

POI_PROVIDER = "APP_ASSET"


async def main() -> None:
    async with SessionLocal() as session:
        for poi_id, name, addr, lat, lng in NAVIGATION_PLACES:
            found = (
                await session.execute(
                    select(PlaceMst).where(
                        PlaceMst.poi_provider == POI_PROVIDER,
                        PlaceMst.poi_id == poi_id,
                    )
                )
            ).scalar_one_or_none()
            if found is None:
                found = PlaceMst(
                    place_nm=name,
                    lat=coord(lat),
                    lng=coord(lng),
                    data_src_cd=DataSrc.NAVI,
                    poi_id=poi_id,
                    poi_provider=POI_PROVIDER,
                    created_user_no=1,
                )
                session.add(found)
                await session.flush()
            await session.execute(
                text("UPDATE place_mst SET addr = :addr WHERE place_no = :place_no"),
                {"addr": addr, "place_no": found.place_no},
            )
        await session.commit()

        count = len(
            (
                await session.execute(
                    select(PlaceMst.place_no).where(
                        PlaceMst.poi_provider == POI_PROVIDER,
                        PlaceMst.deleted_yn == "N",
                    )
                )
            ).all()
        )
    print(f"navigation_places={count}")


if __name__ == "__main__":
    asyncio.run(main())
