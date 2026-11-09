package com.example.travelcrumbs.data.recommendation

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SidoRegion
import com.example.travelcrumbs.domain.model.Tag

/**
 * 시연용 시드(#37) — 와이어프레임 v6 시나리오 S-2 의 강원 속초시 로드맵 3개 + 강릉시 · 양양군 몇 개.
 * 서버(공개 trip_mst · trip_place_dtl · trip_place_tag_rel · tag_mst)가 생기기 전까지만 쓰는 값 · 이 파일 한 곳에만 둠.
 * 시군구별 로드맵 수 · 태그별 개수 · 대표 태그는 여기 적지 않고 도메인 계산(RecommendationRules)이 셈.
 * 이름 · 별점은 지어낸 시연 값 · 좌표는 실제 장소 근처.
 */
internal object RecommendationSeed {

    private val KIDS = Tag(tagNo = 1, tagNm = "아이와 가기 좋아요")
    private val PARKING = Tag(tagNo = 2, tagNm = "주차 편해요")
    private val AGAIN = Tag(tagNo = 3, tagNm = "다시 가고 싶어요")
    private val WAITING = Tag(tagNo = 4, tagNm = "웨이팅 있어요")
    private val RESTROOM = Tag(tagNo = 5, tagNm = "화장실 깨끗해요")

    private const val GANGWON = "강원"
    private val SOKCHO = Region(GANGWON, "속초시")
    private val GANGNEUNG = Region(GANGWON, "강릉시")
    private val YANGYANG = Region(GANGWON, "양양군")

    val sidoRegions: List<SidoRegion> = listOf(
        SidoRegion("서울", emptyList()),
        SidoRegion("경기", emptyList()),
        SidoRegion(GANGWON, listOf("속초시", "강릉시", "양양군", "춘천시", "평창군")),
        SidoRegion("충북", emptyList()),
        SidoRegion("충남", emptyList()),
        SidoRegion("전북", emptyList()),
        SidoRegion("전남", emptyList()),
        SidoRegion("경북", emptyList()),
        SidoRegion("경남", emptyList()),
        SidoRegion("제주", emptyList()),
    )

    val trips: List<RecommendedTrip> = listOf(
        RecommendedTrip(
            tripNo = 101,
            tripNm = "속초 가족여행",
            nicknameNm = "민지네",
            region = SOKCHO,
            companionTypes = setOf(CompanionType.SPOUSE, CompanionType.CHILD),
            ratingAvg = 4.8,
            ratingCnt = 12,
            publishedAt = 1_762_300_000_000,
            places = listOf(
                place(1001, "속초 중앙시장 공영주차장", 1, "강원 속초시 중앙로", 38.2049, 128.5906, PARKING, RESTROOM),
                place(1002, "중앙시장 닭강정", 2, "강원 속초시 중앙로", 38.2043, 128.5918, WAITING, KIDS),
                place(1003, "영랑호 산책로", 3, "강원 속초시 영랑호반길", 38.2236, 128.5852, KIDS, AGAIN),
                place(1004, "속초해수욕장", 4, "강원 속초시 해오름로", 38.1906, 128.6022, KIDS, PARKING),
            ),
        ),
        RecommendedTrip(
            tripNo = 102,
            tripNm = "바다 보고 시장 한 바퀴",
            nicknameNm = "바다곰",
            region = SOKCHO,
            companionTypes = setOf(CompanionType.SPOUSE),
            ratingAvg = 4.6,
            ratingCnt = 9,
            publishedAt = 1_762_500_000_000,
            places = listOf(
                place(1011, "대포항", 1, "강원 속초시 대포항길", 38.1747, 128.6067, PARKING, WAITING),
                place(1012, "외옹치 바다향기로", 2, "강원 속초시 대포동", 38.1849, 128.6052, AGAIN),
                place(1013, "아바이마을", 3, "강원 속초시 청호로", 38.2017, 128.5990, WAITING),
                place(1001, "속초 중앙시장 공영주차장", 4, "강원 속초시 중앙로", 38.2049, 128.5906, PARKING),
                place(1014, "속초등대전망대", 5, "강원 속초시 영금정로", 38.2121, 128.6000, AGAIN),
            ),
        ),
        RecommendedTrip(
            tripNo = 103,
            tripNm = "설악산 아래 하루",
            nicknameNm = "산책러",
            region = SOKCHO,
            companionTypes = setOf(CompanionType.CHILD, CompanionType.PARENT),
            ratingAvg = 4.4,
            ratingCnt = 6,
            publishedAt = 1_762_400_000_000,
            places = listOf(
                place(1021, "설악산 소공원", 1, "강원 속초시 설악산로", 38.1726, 128.4890, AGAIN, KIDS, RESTROOM),
                place(1022, "신흥사", 2, "강원 속초시 설악산로", 38.1712, 128.4883, AGAIN, KIDS),
                place(1023, "척산온천", 3, "강원 속초시 관광로", 38.1856, 128.5469, AGAIN, PARKING, KIDS),
            ),
        ),
        RecommendedTrip(
            tripNo = 201,
            tripNm = "경포 바다 산책",
            nicknameNm = "솔향기",
            region = GANGNEUNG,
            companionTypes = setOf(CompanionType.SOLO),
            ratingAvg = 4.7,
            ratingCnt = 15,
            publishedAt = 1_762_200_000_000,
            places = listOf(
                place(2001, "경포해변", 1, "강원 강릉시 창해로", 37.8055, 128.9087, AGAIN, PARKING),
                place(2002, "안목해변 커피거리", 2, "강원 강릉시 창해로14번길", 37.7722, 128.9472, WAITING),
                place(2003, "오죽헌", 3, "강원 강릉시 율곡로", 37.7792, 128.8786, KIDS, RESTROOM),
            ),
        ),
        RecommendedTrip(
            tripNo = 202,
            tripNm = "강릉 시장 투어",
            nicknameNm = "먹보네",
            region = GANGNEUNG,
            companionTypes = setOf(CompanionType.SPOUSE, CompanionType.CHILD),
            ratingAvg = 4.3,
            ratingCnt = 5,
            publishedAt = 1_762_600_000_000,
            places = listOf(
                place(2011, "강릉중앙시장", 1, "강원 강릉시 금성로", 37.7545, 128.8990, WAITING, KIDS),
                place(2012, "주문진수산시장", 2, "강원 강릉시 주문진읍", 37.8930, 128.8268, PARKING),
            ),
        ),
        RecommendedTrip(
            tripNo = 301,
            tripNm = "서핑 입문 주말",
            nicknameNm = "파도타기",
            region = YANGYANG,
            companionTypes = setOf(CompanionType.SOLO),
            ratingAvg = 4.5,
            ratingCnt = 7,
            publishedAt = 1_762_100_000_000,
            places = listOf(
                place(3001, "서피비치", 1, "강원 양양군 현북면", 38.0270, 128.7186, AGAIN, RESTROOM),
                place(3002, "하조대", 2, "강원 양양군 현북면", 38.0233, 128.7215, PARKING),
                place(3003, "낙산사", 3, "강원 양양군 강현면", 38.1248, 128.6278, AGAIN),
            ),
        ),
    )

    @Suppress("LongParameterList")
    private fun place(
        placeNo: Long,
        placeNm: String,
        placeOrd: Int,
        addr: String,
        lat: Double,
        lng: Double,
        vararg tags: Tag,
    ) = RecommendedPlace(
        placeNo = placeNo,
        placeNm = placeNm,
        placeOrd = placeOrd,
        addr = addr,
        lat = lat,
        lng = lng,
        tags = tags.toList(),
    )
}
