package com.example.travelcrumbs.ui.demo

import androidx.lifecycle.ViewModel
import com.example.travelcrumbs.domain.model.Place
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class DemoStop(
    val name: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val memo: String = "",
    val navigationTarget: Place? = null,
)
internal data class DemoRoadmap(
    val id: String,
    val title: String,
    val city: String,
    val district: String,
    val stars: Double,
    val count: Int,
    val stops: List<DemoStop>,
    val tag: String = "",
    val memo: String = "",
    val owner: String? = null,
    val companion: String = "",
)

internal val seoulStops = listOf(
    DemoStop("서울숲", "산책", navigationTarget = Place("seoul-forest", "서울숲", "서울 성동구 뚝섬로 273", 37.5432, 127.0419)),
    DemoStop("성수 카페거리", "카페", navigationTarget = Place("seongsu-cafe", "성수 카페거리", "서울 성동구 연무장길", 37.5445, 127.0557)),
    DemoStop("뚝섬한강공원", "야경", navigationTarget = Place("ttukseom-park", "뚝섬한강공원", "서울 광진구 뚝섬 제3주차장 입구", 37.53127883, 127.0664085)),
)

internal val demoRegions = linkedMapOf(
    "서울특별시" to listOf("성동구", "종로구"),
    "부산광역시" to listOf("해운대구"),
    "제주특별자치도" to listOf("제주시"),
)

internal val demoRoadmaps = listOf(
    DemoRoadmap("forest", "서울숲에서 한강까지", "서울특별시", "성동구", 4.9, 24, seoulStops, "산책", "해 질 무렵 한강이 특히 예뻐요."),
    DemoRoadmap("cafe", "성수 카페와 골목 산책", "서울특별시", "성동구", 4.7, 18, seoulStops.take(2), "카페"),
    DemoRoadmap("palace", "궁과 서촌 한 바퀴", "서울특별시", "종로구", 4.8, 12, listOf(DemoStop("경복궁", "문화", navigationTarget = Place("gyeongbokgung", "경복궁", "서울 종로구 사직로 161", 37.5796, 126.977)), DemoStop("서촌", "산책", navigationTarget = Place("seochon", "서촌", "서울 종로구 자하문로15길", 37.5785, 126.9696)))),
    DemoRoadmap("ocean", "해운대 바다 드라이브", "부산광역시", "해운대구", 4.9, 20, listOf(DemoStop("해운대해수욕장", "바다", navigationTarget = Place("haeundae-beach", "해운대해수욕장", "부산 해운대구 해운대해변로 264", 35.1587, 129.1604)), DemoStop("달맞이길", "전망", navigationTarget = Place("dalmaji", "달맞이길", "부산 해운대구 달맞이길", 35.1578, 129.1819)))),
    DemoRoadmap("jeju", "제주 바람 따라", "제주특별자치도", "제주시", 4.8, 16, listOf(DemoStop("용두암", "전망", navigationTarget = Place("yongduam", "용두암", "제주 제주시 용두암길 15", 33.5149624, 126.5116869)), DemoStop("이호테우해변", "바다", navigationTarget = Place("iho-beach", "이호테우해변", "제주 제주시 도리로", 33.4981, 126.4524)))),
)

internal data class DemoTrip(
    val roadmap: DemoRoadmap,
    val editedStops: List<DemoStop>,
    val saved: Boolean = false,
    val published: Boolean = false,
)

internal data class DemoUiState(
    val account: String? = null,
    val locationConsent: Boolean = false,
    val recording: Boolean = false,
    val paused: Boolean = false,
    val steps: Int = 0,
    val stops: List<DemoStop> = emptyList(),
    val trips: Map<String, DemoTrip> = emptyMap(),
    val companion: String = "혼자",
    val savedPlaces: Map<String, List<DemoStop>> = emptyMap(),
    val city: String? = null,
    val district: String? = null,
    val ratings: Map<Pair<String, String>, Int> = emptyMap(),
    val message: String? = null,
) {
    private val currentTrip get() = trips[account]
    val ownTrip get() = currentTrip?.roadmap
    val editedStops get() = currentTrip?.editedStops.orEmpty()
    val saved get() = currentTrip?.saved == true
    val published get() = currentTrip?.published == true
    val sharedRoadmaps get() = trips.values.filter { it.published }.map { it.roadmap }
    val ownSavedPlaces get() = savedPlaces[account].orEmpty()
    fun guidedStop(place: Place): DemoStop =
        stops.takeIf { recording }?.firstOrNull { it.name == place.name }
            ?: ownSavedPlaces.firstOrNull { it.name == place.name }
            ?: DemoStop(place.name, "내비 목적지", navigationTarget = place)

    val nextSimulatedStop get() = seoulStops.firstOrNull { candidate -> stops.none { it.name == candidate.name } }
    val minutes get() = (steps + stops.size) * 4
}

internal class DemoViewModel : ViewModel() {
    private val state = MutableStateFlow(DemoUiState())
    val uiState = state.asStateFlow()

    fun login(name: String) = state.update { it.copy(account = name, message = null) }
    fun logout() = state.update { it.copy(account = null, locationConsent = false, recording = false, paused = false, message = null) }
    fun reset() { state.value = DemoUiState() }

    fun setLocationConsent(value: Boolean) = state.update {
        it.copy(locationConsent = value, paused = it.paused || (it.recording && !value))
    }

    fun selectCompanion(companion: String) = state.update { it.copy(companion = companion) }

    fun start() = state.update {
        if (it.account == null || !it.locationConsent || it.recording) it else
            it.copy(recording = true, paused = false, steps = 0, stops = emptyList(), message = null)
    }

    fun advance() = state.update {
        if (it.recording && !it.paused && it.locationConsent) it.copy(steps = (it.steps + 1).coerceAtMost(seoulStops.size))
        else it
    }

    fun addStop() = state.update {
        val next = it.nextSimulatedStop ?: return@update it
        if (!it.recording || it.paused || !it.locationConsent) it else
            it.copy(stops = it.stops + next, message = null)
    }

    fun saveGuidedDestination(destination: DemoGuidedDestination, tags: List<String>?) = state.update {
        val account = it.account ?: return@update it
        if (account != destination.account) return@update it
        val existing = it.guidedStop(destination.place)
        val stop = existing.copy(
            tags = tags ?: existing.tags,
            navigationTarget = destination.place,
        )
        if (it.recording) {
            it.copy(stops = it.stops.upsertPlace(stop))
        } else {
            it.copy(savedPlaces = it.savedPlaces + (account to it.ownSavedPlaces.upsertPlace(stop)))
        }
    }

    fun updateRecordTags(placeName: String, tags: List<String>, savedPlace: Boolean) = state.update {
        val account = it.account ?: return@update it
        if (savedPlace) {
            val updated = it.ownSavedPlaces.map { stop -> if (stop.name == placeName) stop.copy(tags = tags) else stop }
            it.copy(savedPlaces = it.savedPlaces + (account to updated))
        } else {
            it.copy(stops = it.stops.map { stop -> if (stop.name == placeName) stop.copy(tags = tags) else stop })
        }
    }

    fun addSavedPlace(stop: DemoStop) = state.update {
        if (it.recording && stop in it.ownSavedPlaces && it.stops.none { place -> place.name == stop.name }) {
            it.copy(stops = it.stops + stop)
        } else {
            it
        }
    }

    private fun List<DemoStop>.upsertPlace(stop: DemoStop): List<DemoStop> =
        if (none { it.name == stop.name }) this + stop else map { if (it.name == stop.name) stop else it }

    fun togglePause() = state.update {
        if (it.recording && it.locationConsent) it.copy(paused = !it.paused) else it
    }

    fun finish() = state.update { current ->
        val account = current.account ?: return@update current
        if (!current.recording || current.stops.isEmpty()) return@update current
        val roadmap = DemoRoadmap(
            id = "mine-$account",
            title = "성수 한 바퀴",
            city = "서울특별시",
            district = "성동구",
            stars = 0.0,
            count = 0,
            stops = current.stops,
            owner = account,
            companion = current.companion,
        )
        current.copy(
            recording = false,
            paused = false,
            trips = current.trips + (account to DemoTrip(roadmap, current.stops)),
            ratings = current.ratings.filterKeys { it.second != roadmap.id },
            message = null,
        )
    }

    fun editStop(index: Int, update: (DemoStop) -> DemoStop) = updateTrip { trip ->
        trip.copy(editedStops = trip.editedStops.mapIndexed { position, stop ->
            if (position == index) update(stop) else stop
        })
    }

    fun save() = updateTrip("로드맵을 저장했어요.") { trip ->
        val stops = trip.editedStops.map { it.copy(memo = it.memo.trim()) }
        trip.copy(roadmap = trip.roadmap.copy(stops = stops), editedStops = stops, saved = true, published = false)
    }

    fun publish() = updateTrip("추천에 로드맵을 공개했어요.") { trip ->
        if (trip.saved) trip.copy(published = true) else trip
    }

    fun unpublish() = updateTrip("로드맵 공개를 취소했어요.") { trip ->
        trip.copy(published = false)
    }

    private fun updateTrip(message: String? = null, transform: (DemoTrip) -> DemoTrip) = state.update {
        val account = it.account ?: return@update it
        val trip = it.trips[account] ?: return@update it
        it.copy(trips = it.trips + (account to transform(trip)), message = message)
    }

    fun selectCity(city: String) = state.update { it.copy(city = city, district = null) }
    fun selectDistrict(district: String) = state.update { it.copy(district = district) }
    fun rate(id: String, stars: Int) = state.update { current ->
        val account = current.account ?: return@update current
        if (current.ownTrip?.id == id || stars !in 1..5) current else current.copy(ratings = current.ratings + ((account to id) to stars))
    }
}
