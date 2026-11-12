package com.example.travelcrumbs.ui.recommendations.map

import com.example.travelcrumbs.data.map.KoreaMapRepository
import com.example.travelcrumbs.data.recommendation.RecommendationRepository
import com.example.travelcrumbs.domain.RecommendationMapRules
import com.example.travelcrumbs.domain.model.KoreaMapAsset
import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.MapPlace
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지도 렌더러 경계의 상태 보관 객체(#59 · 스펙 N16 ①) — 좌표를 가진 값은 여기와 지도 화면 부품에만 둠.
 * - 지도 장소 소스: 저장소(추천)에서 바로 받아 핀 원본으로(ViewModel 상태를 거치지 않음 · 스펙 3-1).
 * - 카메라(지도 가운데 · 확대): 같은 프로세스 메모리만 · SavedStateHandle · 저장 상태 번들 · 로그에 넣지 않음 —
 *   B-02 · B-04 다녀와도 그대로(M38) · 앱 틀의 진입 값이 바뀌면(새로 「추천」) 버리고 첫 맞춤을 다시 함.
 * - 내 위치는 #55(LOCATION_RECO) 문구 반영 뒤 여기서 받음 — 지금은 위치 조회 없음.
 * ViewModel 로는 화면 부품이 장소 번호 · 개수 · 결과 종류만 넘김.
 */
@Singleton
class RecommendationMapHolder @Inject constructor(
    private val recommendations: RecommendationRepository,
    private val koreaMap: KoreaMapRepository,
) {
    private var entryKey: Int? = null

    /** 마지막 카메라 — null 이면 첫 맞춤. */
    var camera: MapCamera? = null
        private set

    /** 마지막으로 반영한 시도 이동 요청 번호 — 같은 요청을 다시 그릴 때(B-02 에서 돌아옴) 다시 옮기지 않게. */
    var appliedFocusSeq: Int = 0
        private set

    /** 앱 틀의 진입 값 — 바뀌면 카메라 · 시도 이동 기록을 버리고 true(첫 맞춤 필요). */
    fun beginEntry(key: Int): Boolean {
        if (entryKey == key) return camera == null
        entryKey = key
        camera = null
        appliedFocusSeq = 0
        return true
    }

    fun saveCamera(value: MapCamera) {
        camera = value
    }

    fun markFocusApplied(seq: Int) {
        appliedFocusSeq = seq
    }

    suspend fun mapAsset(): KoreaMapAsset? = koreaMap.mapAsset()

    /** 공개 장소 핀 원본 — 화면에 들어올 때마다 다시 읽음(비공개 · 삭제 반영 · M26). */
    suspend fun places(): List<MapPlace> = RecommendationMapRules.mapPlaces(recommendations.allPublicTrips())
}

/** 지도 카메라 — 가운데 · 확대 단계(메모리만). */
data class MapCamera(
    val center: MapCoord,
    val zoom: Double,
)
