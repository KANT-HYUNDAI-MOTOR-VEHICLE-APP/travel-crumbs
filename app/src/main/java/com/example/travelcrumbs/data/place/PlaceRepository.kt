package com.example.travelcrumbs.data.place

import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.data.remote.NaviPlaceDto
import com.example.travelcrumbs.data.remote.TripPlaceItem
import com.example.travelcrumbs.data.remote.TripPlaceUpdateRequest
import com.example.travelcrumbs.domain.model.PlaceInput
import com.example.travelcrumbs.domain.model.PlaceType
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.domain.model.TripPlace
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 기록한 장소(trip_place_dtl) · 경험 태그(tag_mst)의 창구 — 장소 경험 입력(A-06). */
interface PlaceRepository {
    /** 입력 안 한 임시 기록(draft_yn = Y) — [refreshDrafts] 가 갱신. 홈 · A-06 이 같은 값을 봄. */
    val drafts: StateFlow<List<TripPlace>>

    suspend fun refreshDrafts(tripUuid: String): Result<List<TripPlace>>

    suspend fun tags(): Result<List<Tag>>

    /** 장소유형(place_type_mst) — code = 장소유형코드, categoryCode = 장소카테고리코드. */
    suspend fun placeTypes(): Result<List<PlaceType>>

    /** 「저장」 — 임시 기록을 완성하고 임시 기록 목록을 다시 읽음. */
    suspend fun complete(tripUuid: String, tripPlaceNo: Long, input: PlaceInput): Result<TripPlace>

    /** 기록 중인 여행이 없어졌을 때. */
    fun clearDrafts()
}

@Singleton
class DefaultPlaceRepository @Inject constructor(
    private val api: AppApi,
) : PlaceRepository {

    private val draftList = MutableStateFlow<List<TripPlace>>(emptyList())
    override val drafts: StateFlow<List<TripPlace>> = draftList.asStateFlow()

    override suspend fun refreshDrafts(tripUuid: String): Result<List<TripPlace>> = runCatching {
        api.getPlaces(tripUuid, draftYn = YES).map { it.toDomain() }
    }.onSuccess { draftList.value = it }

    override suspend fun tags(): Result<List<Tag>> = runCatching {
        api.getTags().map { Tag(tagNo = it.tagNo, name = it.tagNm) }
    }

    override suspend fun placeTypes(): Result<List<PlaceType>> = runCatching {
        api.getPlaceTypes().map {
            PlaceType(it.placeTypeCd, it.placeTypeNm, it.placeCategoryCd, it.placeCategoryNm)
        }
    }

    override suspend fun complete(tripUuid: String, tripPlaceNo: Long, input: PlaceInput): Result<TripPlace> =
        runCatching {
            val navi = input.naviPlace
            val poiId = navi?.poiId
            val poiProvider = navi?.poiProvider
            val naviDto = if (navi != null && poiId != null && poiProvider != null) {
                NaviPlaceDto(navi.name, navi.lat, navi.lng, poiId, poiProvider)
            } else {
                null
            }
            api.updatePlace(
                tripUuid,
                tripPlaceNo,
                TripPlaceUpdateRequest(
                    userPlaceNm = input.userPlaceName,
                    placeTypeCd = input.placeTypeCode,
                    reviewPolarityCd = input.reviewPolarityCode,
                    memo = input.memo,
                    tagNos = input.tagNos,
                    naviPlace = naviDto,
                ),
            ).toDomain()
        }.onSuccess { refreshDrafts(tripUuid) }

    override fun clearDrafts() {
        draftList.value = emptyList()
    }

    private fun TripPlaceItem.toDomain(): TripPlace = TripPlace(
        tripPlaceNo = tripPlaceNo,
        lat = lat,
        lng = lng,
        recordedAt = recordedAt,
        isDraft = draftYn == YES,
        userPlaceName = userPlaceNm,
        placeTypeCode = placeTypeCd,
        reviewPolarityCode = reviewPolarityCd,
        memo = memo,
        tagNos = tagNos,
    )

    private companion object {
        const val YES = "Y"
    }
}
