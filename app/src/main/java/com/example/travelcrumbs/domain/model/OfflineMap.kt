package com.example.travelcrumbs.domain.model

/**
 * 앱에 묶어 둔 시연용 오프라인 도로 지도(`assets/maps/roadmaps.json`) — 추천 상세(B-04) 경로 그림에만 씀.
 * 지도 · 위치 · 네트워크를 런타임에 쓰지 않음. 출처는 OpenStreetMap(ODbL) · 구간은 OSRM 으로 미리 계산(assets/maps/SOURCES.txt).
 */
data class OfflineMapPack(val areas: List<MapArea>)

/** 경도 · 위도 한 점. */
data class MapCoord(val lng: Double, val lat: Double)

/** 지역 범위(서 · 남 · 동 · 북). */
data class MapBounds(val west: Double, val south: Double, val east: Double, val north: Double) {
    fun contains(coord: MapCoord): Boolean = coord.lng in west..east && coord.lat in south..north

    val span: Double get() = (east - west) * (north - south)
}

/** 지도 층 — 묶음 파일의 층 이름([key]) 그대로. 그리는 순서도 이 순서(아래 → 위). */
enum class MapLayer(val key: String) {
    WATER("water"),
    PARK("park"),
    RIVER("river"),
    SHORE("shore"),
    LOCAL("local"),
    STREET("street"),
    MAJOR("major"),
    ;

    companion object {
        fun fromKey(key: String): MapLayer? = entries.firstOrNull { it.key == key }
    }
}

/** 지역 하나 — 층별 선 · 면 · 로드맵 장소 사이 도로 구간. */
data class MapArea(
    val areaId: String,
    val label: String,
    val bounds: MapBounds,
    val layers: Map<MapLayer, List<List<MapCoord>>>,
    val routes: List<MapRoute>,
)

/** 미리 계산한 도로 구간 — 장소 번호(placeNo) 사이. */
data class MapRoute(
    val fromPlaceNo: Long,
    val toPlaceNo: Long,
    val points: List<MapCoord>,
)

/** 화면 평면의 한 점 — 지도 가운데에서 떨어진 정도(x 오른쪽 · y 아래쪽 · 경도는 위도 보정). 실제 좌표 아님. */
data class MapXY(val x: Float, val y: Float)

/** 장소 사이 한 구간 — [onRoad] 가 false 면 미리 계산한 도로가 없어 두 장소를 곧게 이음. */
data class RouteLeg(val points: List<MapXY>, val onRoad: Boolean)

/**
 * B-04 에 그릴 도로 지도 — 장소들을 가운데에 두고 평면으로 옮긴 결과.
 * [halfWidth] · [halfHeight] 는 장소 · 구간이 들어가야 하는 반폭 · 반높이(화면은 이 범위가 들어가게 배율을 고름).
 */
data class RouteMap(
    val areaLabel: String,
    val layers: Map<MapLayer, List<List<MapXY>>>,
    val legs: List<RouteLeg>,
    val pins: List<MapXY>,
    val halfWidth: Float,
    val halfHeight: Float,
)
