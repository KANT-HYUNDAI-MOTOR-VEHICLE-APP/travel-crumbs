package com.example.travelcrumbs.data.trip

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.data.local.TravelDao
import com.example.travelcrumbs.data.local.TripCompanionDtlEntity
import com.example.travelcrumbs.data.local.TripMstEntity
import com.example.travelcrumbs.data.local.TripPlaceDtlEntity
import com.example.travelcrumbs.data.local.TripPlaceTagRelEntity
import com.example.travelcrumbs.data.local.YesNo
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.InputValidation
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.AutoPlace
import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.CurrentLocation
import com.example.travelcrumbs.domain.model.TripPlace
import com.example.travelcrumbs.domain.model.TripRecord
import com.example.travelcrumbs.domain.model.TripStatus
import com.example.travelcrumbs.domain.model.Visibility
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * 내 여행 · 장소 · 태그 · 공개 범위의 유일한 출처(#51 U1 · A-03 · A-05 ~ A-08 · C-03 철회 · U7 추천).
 * 지금은 기기 안 Room(서버 API 전 임시 원본) — 데이터 원본은 서버(ADR 0015)라 서버 API 가 생기면 이 인터페이스의 구현만 바꿈
 * (di/TripModule). 좌표 · 주소 · 메모는 로그에 남기지 않음.
 */
interface TripRepository {
    /** 지금 사용자의 여행 전부(모든 상태) — 최근 시작 순. */
    val trips: Flow<List<TripRecord>>

    /** 여행 하나 — 없으면 null. */
    fun trip(tripNo: Long): Flow<TripRecord?>

    /** 로드맵 검토(A-05)를 기다리는 여행 — 가장 최근 여행이 끝났고(COMPLETED) 장소가 있고 아직 검토 안 했으면 그 번호. */
    val pendingReviewTripNo: Flow<Long?>

    /** 검토 끝(「저장」 · 「나중에」) — 그 여행은 다시 검토 화면에 안 뜸. */
    suspend fun markReviewed(tripNo: Long)

    /** 장소 한 곳 기록(「이 장소 기록」) — [draft] 면 위치만(태그는 나중에) · 장소 순서는 그 여행의 다음 번호. 새 행 번호. */
    suspend fun addPlace(tripNo: Long, location: CurrentLocation?, draft: Boolean): Long

    /**
     * 자동 기록 확정 1건(#66 스펙 4.1) — draft Y · 순서 = 그 여행의 다음 번호 · 기록 시각 = [place] 의 도착 시각 · 메모 없음. 새 행 번호.
     * 이름은 부르는 쪽이 정한 값 그대로(내비 목적지 이름 · 없으면 주소 처음 값 · 둘 다 없으면 null).
     */
    suspend fun addAutoPlace(tripNo: Long, place: AutoPlace): Long

    /** 장소 고치기(A-03 「저장」 · A-05 · A-08) — 이름 · 메모 · 태그(통째로) · 임시저장 해제. */
    suspend fun savePlace(tripPlaceNo: Long, userPlaceNm: String?, memo: String?, tagNos: Set<Long>)

    /** 여행명 고치기(A-08) — 비었으면 바꾸지 않음. */
    suspend fun rename(tripNo: Long, tripNm: String)

    /** 공개 범위 바꾸기(A-06 · A-07 · A-08 토글). */
    suspend fun setVisibility(tripNo: Long, visibility: Visibility)

    /** 추천 공개 동의 철회(C-03) — 내 공개 여행을 모두 비공개로. */
    suspend fun makeAllPrivate()

    /**
     * 위치 수집 동의 철회(C-03) — 내 장소의 좌표 · 주소(와 주소에서 온 이름)를 기기에서 지움(물리 · 지운 여행의 장소까지).
     * trip_mst.dest_place_nm 은 남김 — A-01 에서 앱 장소 목록(places.json 후보)에서 고른 이름이지 위치 수집 결과가 아님.
     * [autoNames] = 자동 기록 이름 원본(장소 번호 → 확정 때 이름 · #66 3.9) — 지금 이름이 원본과 글자까지 같으면(사람이 안 고침) 그 이름도 비움.
     */
    suspend fun clearLocations(autoNames: Map<Long, String> = emptyMap())

    /** A-08 「여행 삭제」 — 그 여행을 지움(논리 삭제 · 목록 · 추천에서 빠짐). */
    suspend fun deleteTrip(tripNo: Long)

    /** C-03 「내 기록 모두 지우기」 — 내 여행 · 동행 · 장소 · 태그 관계를 물리 삭제 + 별점 기록 · 검토 표시 키 삭제(진행 중 기록도 끝남 · 동의 이력은 남김). */
    suspend fun deleteAll()
}

@Singleton
class LocalTripRepository @Inject constructor(
    private val dao: TravelDao,
    private val users: CurrentUserRepository,
    private val dataStore: DataStore<Preferences>,
    private val timeSource: TimeSource,
) : TripRepository {

    override val trips: Flow<List<TripRecord>> = flow {
        val userNo = users.currentUserNo()
        emitAll(
            combine(dao.trips(), dao.companions(), dao.places(), dao.placeTags()) { trips, companions, places, tags ->
                assembleTrips(trips.filter { it.userNo == userNo }, companions, places, tags)
            },
        )
    }.distinctUntilChanged()

    override fun trip(tripNo: Long): Flow<TripRecord?> =
        trips.map { list -> list.firstOrNull { it.tripNo == tripNo } }.distinctUntilChanged()

    private val reviewed: Flow<Set<Long>> = dataStore.data.map { prefs ->
        prefs[AppPreferencesKeys.REVIEWED_TRIP_NOS].orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    override val pendingReviewTripNo: Flow<Long?> = combine(trips, reviewed) { list, done ->
        list.firstOrNull()?.takeIf { it.status == TripStatus.COMPLETED && it.placeCnt > 0 && it.tripNo !in done }?.tripNo
    }.distinctUntilChanged()

    override suspend fun markReviewed(tripNo: Long) {
        dataStore.edit { prefs ->
            val before = prefs[AppPreferencesKeys.REVIEWED_TRIP_NOS].orEmpty().split(',').filter { it.isNotBlank() }
            if (tripNo.toString() !in before) prefs[AppPreferencesKeys.REVIEWED_TRIP_NOS] = (before + tripNo.toString()).joinToString(",")
        }
    }

    override suspend fun addPlace(tripNo: Long, location: CurrentLocation?, draft: Boolean): Long {
        val userNo = users.currentUserNo()
        val now = timeSource.nowMillis()
        return dao.insertPlace(
            TripPlaceDtlEntity(
                tripNo = tripNo,
                placeOrd = dao.lastPlaceOrd(tripNo) + 1,
                userPlaceNm = location?.addr?.let(::placeNameFromAddr),
                memo = null,
                draftYn = YesNo.of(draft),
                lat = location?.lat,
                lng = location?.lng,
                addr = location?.addr,
                recordedAt = now,
                createdAt = now,
                createdUserNo = userNo,
                updatedAt = now,
                updatedUserNo = userNo,
            ),
        )
    }

    override suspend fun addAutoPlace(tripNo: Long, place: AutoPlace): Long {
        val userNo = users.currentUserNo()
        val now = timeSource.nowMillis()
        return dao.insertPlace(
            TripPlaceDtlEntity(
                tripNo = tripNo,
                placeOrd = dao.lastPlaceOrd(tripNo) + 1,
                userPlaceNm = place.userPlaceNm,
                memo = null,
                draftYn = YesNo.YES,
                lat = place.lat,
                lng = place.lng,
                addr = place.addr,
                recordedAt = place.recordedAt,
                createdAt = now,
                createdUserNo = userNo,
                updatedAt = now,
                updatedUserNo = userNo,
            ),
        )
    }

    override suspend fun savePlace(tripPlaceNo: Long, userPlaceNm: String?, memo: String?, tagNos: Set<Long>) {
        val userNo = users.currentUserNo()
        val now = timeSource.nowMillis()
        dao.savePlace(
            tripPlaceNo = tripPlaceNo,
            userPlaceNm = userPlaceNm?.trim()?.take(AppConfig.PLACE_NAME_MAX_LENGTH)?.takeIf { it.isNotEmpty() },
            memo = memo?.trim()?.takeIf { InputValidation.memo(it) && it.isNotEmpty() },
            draftYn = YesNo.NO,
            tags = tagNos.sorted().map {
                TripPlaceTagRelEntity(
                    tripPlaceNo = tripPlaceNo,
                    tagNo = it,
                    createdAt = now,
                    createdUserNo = userNo,
                    updatedAt = now,
                    updatedUserNo = userNo,
                )
            },
            now = now,
        )
    }

    override suspend fun rename(tripNo: Long, tripNm: String) {
        if (!InputValidation.tripName(tripNm)) return
        dao.renameTrip(tripNo, tripNm.trim(), timeSource.nowMillis())
    }

    override suspend fun setVisibility(tripNo: Long, visibility: Visibility) {
        dao.setVisibility(tripNo, visibility.name, timeSource.nowMillis())
    }

    override suspend fun clearLocations(autoNames: Map<Long, String>) {
        val userNo = users.currentUserNo()
        // 장소 이름이 주소에서 만든 처음 값 · 자동 기록 때 넣은 이름(#66 3.9) 그대로면 그것도 위치 정보 — 비움(지운 행까지).
        // 사람이 고친 이름은 남김 · 비교는 주소를 지우기 전에
        val derivedNameNos = dao.allPlacesOfUser(userNo)
            .filter { place -> place.userPlaceNm != null && isUntouchedName(place.userPlaceNm, place.addr, autoNames[place.tripPlaceNo]) }
            .map { it.tripPlaceNo }
        dao.clearLocations(userNo, derivedNameNos, timeSource.nowMillis())
    }

    override suspend fun deleteTrip(tripNo: Long) {
        dao.markTripDeleted(tripNo, timeSource.nowMillis())
    }

    override suspend fun deleteAll() {
        dao.deleteAllTripsOfUser(users.currentUserNo())
        dataStore.edit {
            it.remove(AppPreferencesKeys.REVIEWED_TRIP_NOS)
            // 별점 기록(안내받은 로드맵 · 내 별점 · RatingRepository 의 키)도 내 기록 — 함께 지움(#51 리뷰 2바퀴 B)
            it.remove(AppPreferencesKeys.RATING_RECORDS_JSON)
        }
    }

    override suspend fun makeAllPrivate() {
        dao.changeVisibility(
            userNo = users.currentUserNo(),
            from = Visibility.ANONYMOUS_PUBLIC.name,
            to = Visibility.PRIVATE.name,
            now = timeSource.nowMillis(),
        )
    }

}

/** 표 행들 → 여행 목록(최근 시작 순 · 장소는 순서대로 · 태그는 붙인 순). 모르는 코드값은 안전 쪽(비공개 · 동행자 버림). */
internal fun assembleTrips(
    trips: List<TripMstEntity>,
    companions: List<TripCompanionDtlEntity>,
    places: List<TripPlaceDtlEntity>,
    tags: List<TripPlaceTagRelEntity>,
): List<TripRecord> {
    val companionsByTrip = companions.groupBy { it.tripNo }
    val placesByTrip = places.groupBy { it.tripNo }
    val tagsByPlace = tags.groupBy({ it.tripPlaceNo }, { it.tagNo })
    return trips.sortedByDescending { it.tripNo }.map { row ->
        TripRecord(
            tripNo = row.tripNo,
            tripNm = row.tripNm,
            status = TripStatus.entries.firstOrNull { it.name == row.tripStatusCd } ?: TripStatus.INTERRUPTED,
            visibility = Visibility.entries.firstOrNull { it.name == row.visibilityCd } ?: Visibility.PRIVATE,
            startAt = row.startAt,
            endAt = row.endAt,
            durationMillis = row.duration?.let { it * MILLIS_PER_SECOND },
            destPlaceNm = row.destPlaceNm,
            companions = companionsByTrip[row.tripNo].orEmpty()
                .mapNotNull { c -> CompanionType.entries.firstOrNull { it.name == c.companionTypeCd } }
                .toSet(),
            places = placesByTrip[row.tripNo].orEmpty().sortedBy { it.placeOrd }.map { p ->
                TripPlace(
                    tripPlaceNo = p.tripPlaceNo,
                    tripNo = p.tripNo,
                    placeOrd = p.placeOrd,
                    userPlaceNm = p.userPlaceNm,
                    memo = p.memo,
                    draft = p.draftYn == YesNo.YES,
                    lat = p.lat,
                    lng = p.lng,
                    addr = p.addr,
                    recordedAt = p.recordedAt,
                    tagNos = tagsByPlace[p.tripPlaceNo].orEmpty(),
                )
            },
        )
    }
}

/** 이름이 처음 값 그대로인가 — 주소에서 만든 처음 값 또는 자동 기록 이름 원본과 글자까지 같음(#66 3.9). */
internal fun isUntouchedName(name: String, addr: String?, autoName: String?): Boolean =
    (addr != null && name == placeNameFromAddr(addr)) || (autoName != null && name == autoName)

/**
 * 주소 → 장소 이름 처음 값(A-03 「장소 이름 = 주소에서 · 고칠 수 있음」) — 시도 · 시군구를 뺀 나머지(도로명 · 번지).
 * 둘 이하로 끊기면 주소 그대로. 장소 검색(POI) 이 없어 이름 대신 주소 끝부분을 씀(보고서 남은 결정).
 */
internal fun placeNameFromAddr(addr: String): String? {
    val parts = addr.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return null
    val rest = if (parts.size > 2) parts.drop(2) else parts
    return rest.joinToString(" ")
}

private const val MILLIS_PER_SECOND = 1_000L
