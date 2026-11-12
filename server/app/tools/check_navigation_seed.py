"""내비 후보 장소 시드 검증."""

import asyncio

from sqlalchemy import func, select

from app.db.session import SessionLocal
from app.places.models import PlaceMst
from app.tools.seed_navigation import POI_PROVIDER


async def main() -> None:
    async with SessionLocal() as session:
        count = (
            await session.execute(
                select(func.count()).select_from(PlaceMst).where(
                    PlaceMst.poi_provider == POI_PROVIDER,
                    PlaceMst.deleted_yn == "N",
                )
            )
        ).scalar_one()
    if count != 5:
        raise SystemExit(f"내비 장소 시드 불일치: expected=5 actual={count}")
    print("navigation_places=5 OK")


if __name__ == "__main__":
    asyncio.run(main())
