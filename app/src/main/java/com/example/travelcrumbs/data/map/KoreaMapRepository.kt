package com.example.travelcrumbs.data.map

import android.content.Context
import android.util.Log
import com.example.travelcrumbs.domain.model.KoreaMapAsset
import com.example.travelcrumbs.domain.model.MapBounds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 추천 지도(#59)의 한국 전국 지도 자산에 닿는 유일한 길 — `assets/maps/` 의 지도 파일 · 스타일 · 범위만 읽음(네트워크 · 위치 없음).
 * 하나라도 못 읽으면 null → 화면은 「지도를 불러오지 못했어요」(N2 · M30).
 */
interface KoreaMapRepository {
    suspend fun mapAsset(): KoreaMapAsset?
}

@Singleton
class AssetKoreaMapRepository internal constructor(
    private val readText: (String) -> String,
    private val exists: (String) -> Boolean,
) : KoreaMapRepository {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        readText = { name -> context.assets.open(name).bufferedReader().use { it.readText() } },
        // 지도 파일은 압축 없이 들어가 openFd 로 길이만 확인(전체를 읽지 않음)
        exists = { name -> context.assets.openFd(name).use { it.length > 0 } },
    )

    private val lock = Mutex()
    private var loaded = false
    private var cached: KoreaMapAsset? = null

    override suspend fun mapAsset(): KoreaMapAsset? = lock.withLock {
        if (loaded) return@withLock cached
        withContext(Dispatchers.IO) {
            runCatching {
                check(exists(TILES_FILE)) { "지도 파일 비어 있음" }
                readText(STYLE_FILE)
                parseKoreaMapBounds(readText(BOUNDS_FILE), STYLE_URI)
            }.onFailure { Log.w(TAG, "지도 자산을 읽지 못함: ${it.javaClass.simpleName}") }
                .getOrNull()
        }.also {
            cached = it
            loaded = true
        }
    }

    private companion object {
        const val TAG = "KoreaMapRepository"
        const val TILES_FILE = "maps/korea-z12.pmtiles"
        const val STYLE_FILE = "maps/korea_style.json"
        const val BOUNDS_FILE = "maps/korea_bounds.json"
        const val STYLE_URI = "asset://$STYLE_FILE"
    }
}

@Serializable
private data class KoreaBoundsJson(
    val country: List<Double>,
    val camera: List<Double>,
    val sido: Map<String, List<Double>>,
)

private val json = Json { ignoreUnknownKeys = true }

/** `maps/korea_bounds.json` — 범위는 [서, 남, 동, 북]. 모양이 틀리면 예외(지도 못 그림). */
internal fun parseKoreaMapBounds(text: String, styleUri: String): KoreaMapAsset {
    val parsed = json.decodeFromString<KoreaBoundsJson>(text)
    return KoreaMapAsset(
        styleUri = styleUri,
        countryBounds = parsed.country.toBounds(),
        cameraBounds = parsed.camera.toBounds(),
        sidoBounds = parsed.sido.mapValues { (_, value) -> value.toBounds() },
    )
}

private fun List<Double>.toBounds(): MapBounds {
    require(size == 4) { "범위는 숫자 4개" }
    val bounds = MapBounds(west = this[0], south = this[1], east = this[2], north = this[3])
    require(bounds.west < bounds.east && bounds.south < bounds.north) { "범위 순서" }
    return bounds
}
