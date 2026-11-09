package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.recommendation.RecommendationSeed
import com.example.travelcrumbs.data.trip.TagRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.ConsentState
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.Tag
import com.example.travelcrumbs.domain.model.TripPlace
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.UserProfile
import com.example.travelcrumbs.domain.model.Visibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** 지금 사용자 대역(#51) — 닉네임 · 동의를 메모리에. */
class FakeCurrentUserRepository(
    nicknameNm: String = "민지네",
    private val time: FakeTimeSource = FakeTimeSource(),
) : CurrentUserRepository {
    private val profile = MutableStateFlow(UserProfile(1L, nicknameNm))
    private val consentState = MutableStateFlow<Map<ConsentType, ConsentState>>(emptyMap())

    override val user: Flow<UserProfile> = profile
    override val consents: Flow<Map<ConsentType, ConsentState>> = consentState

    val history = mutableListOf<Pair<ConsentType, Boolean>>()

    override suspend fun currentUserNo(): Long = 1L

    override suspend fun updateNickname(nicknameNm: String): Boolean {
        val trimmed = nicknameNm.trim()
        if (trimmed.isEmpty()) return false
        profile.value = profile.value.copy(nicknameNm = trimmed)
        return true
    }

    override suspend fun recordConsent(type: ConsentType, agreed: Boolean) {
        history += type to agreed
        consentState.value = consentState.value + (type to ConsentState(type, agreed, time.now))
    }
}

/** 내 여행 대역(#51) — 여행 목록을 메모리에 · [put] 으로 시험이 여행을 넣음. */
class FakeTripRepository(
    initial: List<TripRecord> = emptyList(),
    private val time: FakeTimeSource = FakeTimeSource(),
) : TripRepository {
    private val state = MutableStateFlow(initial)
    private val reviewedState = MutableStateFlow<Set<Long>>(emptySet())
    private var lastPlaceNo = 1000L

    override val trips: Flow<List<TripRecord>> = state.map { list -> list.sortedByDescending { it.tripNo } }

    val current: List<TripRecord> get() = state.value

    fun put(trip: TripRecord) {
        state.value = state.value.filterNot { it.tripNo == trip.tripNo } + trip
    }

    override fun trip(tripNo: Long): Flow<TripRecord?> = state.map { list -> list.firstOrNull { it.tripNo == tripNo } }

    override val pendingReviewTripNo: Flow<Long?> = combine(trips, reviewedState) { list, done ->
        list.firstOrNull()?.takeIf { it.status == TripStatus.COMPLETED && it.placeCnt > 0 && it.tripNo !in done }?.tripNo
    }

    override suspend fun markReviewed(tripNo: Long) {
        reviewedState.value = reviewedState.value + tripNo
    }

    override suspend fun addPlace(tripNo: Long, location: CurrentLocation?, draft: Boolean): Long {
        val no = ++lastPlaceNo
        edit(tripNo) { trip ->
            trip.copy(
                places = trip.places + TripPlace(
                    tripPlaceNo = no,
                    tripNo = tripNo,
                    placeOrd = trip.places.size + 1,
                    userPlaceNm = location?.addr,
                    memo = null,
                    draft = draft,
                    lat = location?.lat,
                    lng = location?.lng,
                    addr = location?.addr,
                    recordedAt = time.now,
                    tagNos = emptyList(),
                ),
            )
        }
        return no
    }

    override suspend fun savePlace(tripPlaceNo: Long, userPlaceNm: String?, memo: String?, tagNos: Set<Long>) {
        state.value = state.value.map { trip ->
            trip.copy(
                places = trip.places.map {
                    if (it.tripPlaceNo == tripPlaceNo) it.copy(userPlaceNm = userPlaceNm, memo = memo, draft = false, tagNos = tagNos.sorted()) else it
                },
            )
        }
    }

    override suspend fun rename(tripNo: Long, tripNm: String) {
        if (tripNm.isNotBlank()) edit(tripNo) { it.copy(tripNm = tripNm.trim()) }
    }

    override suspend fun setVisibility(tripNo: Long, visibility: Visibility) {
        edit(tripNo) { it.copy(visibility = visibility) }
    }

    override suspend fun makeAllPrivate() {
        state.value = state.value.map { it.copy(visibility = Visibility.PRIVATE) }
    }

    override suspend fun clearLocations() {
        state.value = state.value.map { trip ->
            trip.copy(places = trip.places.map { it.copy(lat = null, lng = null, addr = null) })
        }
    }

    override suspend fun deleteTrip(tripNo: Long) {
        state.value = state.value.filterNot { it.tripNo == tripNo }
    }

    override suspend fun deleteAll() {
        state.value = emptyList()
    }

    private fun edit(tripNo: Long, change: (TripRecord) -> TripRecord) {
        state.value = state.value.map { if (it.tripNo == tripNo) change(it) else it }
    }
}

class FakeTagRepository(list: List<Tag> = RecommendationSeed.tags) : TagRepository {
    override val tags: Flow<List<Tag>> = MutableStateFlow(list)
}

/** 끝난 여행 하나(시험용) — 장소 [places] 개 · 첫 장소는 속초 중앙시장 근처. */
fun completedTrip(
    tripNo: Long = 7L,
    tripNm: String = "속초 가족여행",
    places: Int = 2,
    visibility: Visibility = Visibility.PRIVATE,
    draftFirst: Boolean = false,
): TripRecord = TripRecord(
    tripNo = tripNo,
    tripNm = tripNm,
    status = TripStatus.COMPLETED,
    visibility = visibility,
    startAt = 1_762_700_000_000L,
    endAt = 1_762_710_000_000L,
    durationMillis = 3_600_000L,
    destPlaceNm = "속초 중앙시장",
    companions = setOf(CompanionType.SPOUSE, CompanionType.CHILD),
    places = (1..places).map { ord ->
        TripPlace(
            tripPlaceNo = tripNo * 100 + ord,
            tripNo = tripNo,
            placeOrd = ord,
            userPlaceNm = "장소 $ord",
            memo = null,
            draft = draftFirst && ord == 1,
            lat = 38.2 + ord * 0.01,
            lng = 128.59 + ord * 0.01,
            addr = "강원 속초시 중앙로 $ord",
            recordedAt = 1_762_700_000_000L + ord,
            tagNos = if (draftFirst && ord == 1) emptyList() else listOf(1L),
        )
    },
)
