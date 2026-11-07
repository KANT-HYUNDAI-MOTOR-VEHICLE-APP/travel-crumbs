"""place_mst · tag_mst 읽기 · 쓰기 — SQL 은 이 파일에만."""

from decimal import Decimal

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.places.models import DataSrc, PlaceMst, PlaceTypeMst, TagMst


async def find_tags(session: AsyncSession) -> list[TagMst]:
    stmt = (
        select(TagMst)
        .where(TagMst.use_yn == "Y", TagMst.deleted_yn == "N")
        .order_by(TagMst.tag_no)
    )
    return list((await session.execute(stmt)).scalars())


async def find_valid_tag_nos(session: AsyncSession, tag_nos: list[int]) -> set[int]:
    stmt = select(TagMst.tag_no).where(
        TagMst.tag_no.in_(tag_nos), TagMst.use_yn == "Y", TagMst.deleted_yn == "N"
    )
    return set((await session.execute(stmt)).scalars())


async def find_or_add_navi_place(
    session: AsyncSession,
    user_no: int,
    *,
    place_nm: str,
    lat: Decimal,
    lng: Decimal,
    poi_id: str,
    poi_provider: str,
) -> PlaceMst:
    """같은 POI(제공자 + 번호)면 있는 장소, 없으면 새 공식 장소(DATA_SRC NAVI · 결정 로그 D20)."""
    stmt = select(PlaceMst).where(
        PlaceMst.poi_provider == poi_provider,
        PlaceMst.poi_id == poi_id,
        PlaceMst.deleted_yn == "N",
    )
    found = (await session.execute(stmt)).scalar_one_or_none()
    if found is not None:
        return found
    place = PlaceMst(
        place_nm=place_nm,
        lat=lat,
        lng=lng,
        data_src_cd=DataSrc.NAVI,
        poi_id=poi_id,
        poi_provider=poi_provider,
        created_user_no=user_no,
    )
    session.add(place)
    await session.flush()
    return place


async def find_place_types(session: AsyncSession) -> list[PlaceTypeMst]:
    stmt = (
        select(PlaceTypeMst)
        .where(PlaceTypeMst.use_yn == "Y", PlaceTypeMst.deleted_yn == "N")
        .order_by(PlaceTypeMst.sort_ord)
    )
    return list((await session.execute(stmt)).scalars())
