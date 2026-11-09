package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SketchPoint
import com.example.travelcrumbs.domain.model.TripPlace

/**
 * 내 로드맵 계산(#51 U5 · U6 · U7) — 순수 함수. 좌표 · 주소는 여기서만 쓰고 화면엔 상대 위치 · 지역 이름만 나감.
 */
object RoadmapRules {

    /** 경로 그림 점 하나 — 상대 위치(0 ~ 1) + 장소 순서. */
    data class SketchPin(val point: SketchPoint, val placeOrd: Int)

    /**
     * 간소화 경로(A-05 · A-08) — 좌표가 있는 장소를 순서대로 잇는 점들. 가짜 배경 없이 상대 위치만(위 = 북).
     * 좌표 없는 장소(위치 동의 없음 · 못 읽음)는 그림에서 빠짐.
     */
    fun sketch(places: List<TripPlace>, margin: Float = SKETCH_MARGIN): List<SketchPin> {
        val located = places.filter { it.lat != null && it.lng != null }.sortedBy { it.placeOrd }
        if (located.isEmpty()) return emptyList()
        val lats = located.map { it.lat!! }
        val lngs = located.map { it.lng!! }
        val span = 1f - 2 * margin
        fun scale(value: Double, min: Double, max: Double): Float =
            if (max - min == 0.0) 0.5f else ((value - min) / (max - min)).toFloat()
        return located.map { place ->
            SketchPin(
                point = SketchPoint(
                    x = margin + span * scale(place.lng!!, lngs.min(), lngs.max()),
                    y = margin + span * (1f - scale(place.lat!!, lats.min(), lats.max())),
                ),
                placeOrd = place.placeOrd,
            )
        }
    }

    /**
     * 주소 → 지역(시도 · 시군구 · U7 추천 지역) — 첫 낱말 = 시도(짧은 이름으로 · 「강원특별자치도」 → 「강원」) ·
     * 둘째 낱말이 시 · 군 · 구로 끝나면 시군구. 모르면 null.
     */
    fun regionOf(addr: String?): Region? {
        val parts = addr?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
        val sido = parts.firstOrNull()?.let(::shortSido) ?: return null
        val sigungu = parts.getOrNull(1)?.takeIf { it.endsWith("시") || it.endsWith("군") || it.endsWith("구") }
        return Region(sido, sigungu)
    }

    /** 여행의 지역 — 첫 장소(순서 1부터) 중 주소가 있는 곳 기준(지시서 U7). */
    fun regionOfTrip(places: List<TripPlace>): Region? =
        places.sortedBy { it.placeOrd }.firstNotNullOfOrNull { regionOf(it.addr) }

    /** 대표 태그 — 장소에 가장 많이 붙은 태그(같으면 번호가 작은 것). 없으면 null. */
    fun representativeTagNo(places: List<TripPlace>): Long? =
        places.flatMap { it.tagNos }.groupingBy { it }.eachCount()
            .entries.sortedWith(compareByDescending<Map.Entry<Long, Int>> { it.value }.thenBy { it.key })
            .firstOrNull()?.key

    private fun shortSido(word: String): String? {
        FULL_SIDO[word]?.let { return it }
        if (word in SHORT_SIDO) return word
        val stripped = SUFFIXES.firstOrNull { word.endsWith(it) }?.let { word.removeSuffix(it) } ?: return null
        return stripped.takeIf { it in SHORT_SIDO }
    }

    private const val SKETCH_MARGIN = 0.12f

    private val SHORT_SIDO = setOf(
        "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
        "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주",
    )
    private val SUFFIXES = listOf("특별자치시", "특별자치도", "특별시", "광역시", "도")
    private val FULL_SIDO = mapOf(
        "충청북도" to "충북",
        "충청남도" to "충남",
        "전라북도" to "전북",
        "전북특별자치도" to "전북",
        "전라남도" to "전남",
        "경상북도" to "경북",
        "경상남도" to "경남",
    )
}
