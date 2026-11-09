package com.example.travelcrumbs.data.map

import android.content.Context
import android.util.Log
import com.example.travelcrumbs.domain.model.MapArea
import com.example.travelcrumbs.domain.model.MapBounds
import com.example.travelcrumbs.domain.model.MapCoord
import com.example.travelcrumbs.domain.model.MapLayer
import com.example.travelcrumbs.domain.model.MapRoute
import com.example.travelcrumbs.domain.model.OfflineMapPack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * 시연용 오프라인 도로 지도 묶음에 닿는 유일한 길 — `assets/maps/roadmaps.json` 만 읽음.
 * 위치 · 지도 SDK · 네트워크를 쓰지 않음. 묶음을 못 읽으면 빈 묶음(화면은 기존 경로 그림으로 대체).
 */
interface OfflineMapRepository {
    suspend fun mapPack(): OfflineMapPack
}

@Singleton
class AssetOfflineMapRepository @Inject constructor(
    @ApplicationContext context: Context,
) : OfflineMapRepository {
    private val assets = context.applicationContext.assets
    private val lock = Mutex()
    private var cached: OfflineMapPack? = null

    override suspend fun mapPack(): OfflineMapPack = lock.withLock {
        cached ?: withContext(Dispatchers.IO) {
            runCatching { assets.open(FILE_NAME).bufferedReader().use { it.readText() } }
                .map(::parseOfflineMapPack)
                .onFailure { Log.w(TAG, "지도 묶음을 읽지 못함: ${it.javaClass.simpleName}") }
                .getOrDefault(OfflineMapPack(emptyList()))
        }.also { cached = it }
    }

    private companion object {
        const val TAG = "OfflineMapRepository"
        const val FILE_NAME = "maps/roadmaps.json"
    }
}

private val json = Json { ignoreUnknownKeys = true }

/**
 * 묶음 JSON → 지도. 꼴(데모 프로토타입과 같음): `areas` → 지역 id → `label` · `bounds`[서, 남, 동, 북] ·
 * `paths`[[층 이름, [경도, 위도, 경도, 위도 …]] …] · `routes`[{from, to, points}](from · to = 장소 번호).
 * 모르는 층 · 점이 2개 못 되는 선 · 번호가 아닌 구간은 뺌.
 */
internal fun parseOfflineMapPack(text: String): OfflineMapPack {
    val file = json.decodeFromString<MapFileJson>(text)
    return OfflineMapPack(
        file.areas.map { (areaId, area) ->
            MapArea(
                areaId = areaId,
                label = area.label,
                bounds = MapBounds(area.bounds[0], area.bounds[1], area.bounds[2], area.bounds[3]),
                layers = area.paths.mapNotNull { path -> path.toLayerLine() }
                    .groupBy({ it.first }, { it.second }),
                routes = area.routes.mapNotNull { route ->
                    val from = route.from.toLongOrNull() ?: return@mapNotNull null
                    val to = route.to.toLongOrNull() ?: return@mapNotNull null
                    MapRoute(from, to, route.points.toCoords()).takeIf { it.points.size >= 2 }
                },
            )
        },
    )
}

private fun JsonArray.toLayerLine(): Pair<MapLayer, List<MapCoord>>? {
    if (size < 2) return null
    val layer = MapLayer.fromKey(this[0].jsonPrimitive.content) ?: return null
    val coords = this[1].jsonArray.map { it.jsonPrimitive.double }.toCoords()
    return if (coords.size >= 2) layer to coords else null
}

private fun List<Double>.toCoords(): List<MapCoord> = chunked(2).filter { it.size == 2 }.map { (lng, lat) -> MapCoord(lng, lat) }

@Serializable
private data class MapFileJson(val areas: Map<String, MapAreaJson>)

@Serializable
private data class MapAreaJson(
    val label: String,
    val bounds: List<Double>,
    val paths: List<JsonArray> = emptyList(),
    val routes: List<MapRouteJson> = emptyList(),
)

@Serializable
private data class MapRouteJson(
    val from: String,
    val to: String,
    val points: List<Double>,
)
