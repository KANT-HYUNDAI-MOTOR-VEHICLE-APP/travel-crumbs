"""장소 · 태그 요청 · 응답 모양."""

from pydantic import BaseModel, Field


class TagResponse(BaseModel):
    tag_no: int = Field(description="태그번호")
    tag_nm: str = Field(description="태그명")


class PlaceTypeResponse(BaseModel):
    place_type_cd: str = Field(description="장소유형코드")
    place_type_nm: str = Field(description="장소유형명")
    place_category_cd: str = Field(description="장소카테고리코드")
    place_category_nm: str = Field(description="장소카테고리명")
