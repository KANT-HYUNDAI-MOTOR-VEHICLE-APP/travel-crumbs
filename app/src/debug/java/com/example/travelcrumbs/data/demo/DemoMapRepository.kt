package com.example.travelcrumbs.data.demo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal data class MapPoint(val longitude: Double, val latitude: Double)

internal data class MapBounds(val west: Double, val south: Double, val east: Double, val north: Double) {
    fun contains(point: MapPoint): Boolean = point.longitude in west..east && point.latitude in south..north
}

internal data class MapRoadSegment(val from: String, val to: String, val points: List<MapPoint>)

internal data class DemoMapArea(
    val label: String,
    val bounds: MapBounds,
    val layers: Map<String, List<List<MapPoint>>>,
    val routes: List<MapRoadSegment>,
    val overview: Boolean,
) {
    fun roadSegment(from: String, to: String): List<MapPoint> =
        routes.firstOrNull { it.from == from && it.to == to }?.points
            ?: routes.firstOrNull { it.from == to && it.to == from }?.points?.asReversed()
            ?: emptyList()
}

internal class DemoMapPack(private val areas: Map<String, DemoMapArea>) {
    fun areaFor(points: List<MapPoint>): DemoMapArea {
        if (points.isEmpty()) return areas.getValue("seongsu")
        return areas.values.firstOrNull { area -> !area.overview && points.all(area.bounds::contains) }
            ?: areas.getValue("korea")
    }
}

/** 배포된 시연 지도 묶음만 사용. 차량의 위치 및 네트워크를 읽지 않음. */
internal class DemoMapRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    suspend fun load(): DemoMapPack = withContext(Dispatchers.IO) {
        val json = context.assets.open("maps/demo-roadmaps.json").bufferedReader().use { JSONObject(it.readText()) }
        val areas = json.getJSONObject("areas")
        DemoMapPack(areas.keys().asSequence().associateWith { name -> areas.getJSONObject(name).toArea(name) })
    }

    private fun JSONObject.toArea(id: String): DemoMapArea {
        val bounds = getJSONArray("bounds")
        val paths = getJSONArray("paths")
        val layers = (0 until paths.length()).map { index ->
            val path = paths.getJSONArray(index)
            path.getString(0) to path.getJSONArray(1).toPoints()
        }.groupBy({ it.first }, { it.second })
        val routes = optJSONArray("routes") ?: JSONArray()
        return DemoMapArea(
            label = getString("label"),
            bounds = MapBounds(bounds.getDouble(0), bounds.getDouble(1), bounds.getDouble(2), bounds.getDouble(3)),
            layers = layers,
            routes = (0 until routes.length()).map { index ->
                val route = routes.getJSONObject(index)
                MapRoadSegment(route.getString("from"), route.getString("to"), route.getJSONArray("points").toPoints())
            },
            overview = id == "korea",
        )
    }

    private fun JSONArray.toPoints(): List<MapPoint> =
        (0 until length() step 2).map { MapPoint(getDouble(it), getDouble(it + 1)) }
}
