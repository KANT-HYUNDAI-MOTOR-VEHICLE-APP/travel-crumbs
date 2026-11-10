package com.example.travelcrumbs.data.place

import android.content.Context
import android.util.Log
import com.example.travelcrumbs.domain.model.Place
import com.example.travelcrumbs.domain.model.PlaceIds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 후보 장소 고정 값을 읽는 유일한 곳 — `assets/places.json`(스펙 navigation 4.1 · 결정 기록 U3).
 * 후보를 바꾸려면 그 JSON 하나만 고침. 장래 출처(#3 방문 기록 · 공유받은 여행)가 생기면 이 자료원만 바꿔 끼움.
 */
interface PlaceSource {
    suspend fun places(): List<Place>
}

@Singleton
class PlaceAssetDataSource @Inject constructor(
    @ApplicationContext context: Context,
) : PlaceSource {
    private val assets = context.applicationContext.assets

    override suspend fun places(): List<Place> = withContext(Dispatchers.IO) {
        runCatching { assets.open(FILE_NAME).bufferedReader().use { it.readText() } }
            .map { parsePlaces(it) }
            .onFailure { Log.w(TAG, "후보 장소를 읽지 못함: ${it.javaClass.simpleName}") }
            .getOrDefault(emptyList())
    }

    private companion object {
        const val TAG = "PlaceAssetDataSource"
        const val FILE_NAME = "places.json"
    }
}

private val json = Json { ignoreUnknownKeys = true }

/** 후보 JSON → 장소 목록. 꼴이 틀린 항목(id 꼴 · 빈 이름) · 겹친 id 는 빼고 JSON 순서 그대로. */
internal fun parsePlaces(text: String): List<Place> {
    val file = json.decodeFromString<PlaceFileJson>(text)
    val seen = HashSet<String>()
    return file.places
        .filter { PlaceIds.isValid(it.id) && it.name.isNotBlank() && seen.add(it.id) }
        .map { Place(it.id, it.name, it.address, it.latitude, it.longitude) }
}

@Serializable
private data class PlaceFileJson(val places: List<PlaceJson>)

@Serializable
private data class PlaceJson(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)
