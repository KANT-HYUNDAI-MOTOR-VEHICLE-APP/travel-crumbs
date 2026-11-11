"""경험 태그 · 장소 유형 목록 — 장소 경험 입력(A-06) 버튼."""

from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.session import get_session
from app.places import repository
from app.places.schemas import PlaceTypeResponse, TagResponse

router = APIRouter(tags=["places"])


@router.get("/tags", response_model=list[TagResponse])
async def read_tags(session: AsyncSession = Depends(get_session)) -> list[TagResponse]:
    return [
        TagResponse(tag_no=t.tag_no, tag_nm=t.tag_nm)
        for t in await repository.find_tags(session)
    ]


@router.get("/place-types", response_model=list[PlaceTypeResponse])
async def read_place_types(
    session: AsyncSession = Depends(get_session),
) -> list[PlaceTypeResponse]:
    return [
        PlaceTypeResponse(
            place_type_cd=t.place_type_cd,
            place_type_nm=t.place_type_nm,
            place_category_cd=t.place_category_cd,
            place_category_nm=t.place_category_nm,
        )
        for t in await repository.find_place_types(session)
    ]
