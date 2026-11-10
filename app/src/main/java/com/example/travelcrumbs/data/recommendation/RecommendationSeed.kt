package com.example.travelcrumbs.data.recommendation

import com.example.travelcrumbs.domain.model.CompanionType
import com.example.travelcrumbs.domain.model.RecommendedPlace
import com.example.travelcrumbs.domain.model.RecommendedTrip
import com.example.travelcrumbs.domain.model.Region
import com.example.travelcrumbs.domain.model.SidoNames
import com.example.travelcrumbs.domain.model.SidoRegion
import com.example.travelcrumbs.domain.model.Tag

/**
 * 시연용 시드(#37 · #51 확장) — 와이어프레임 v6 시나리오 S-2 의 강원 속초시 로드맵 3개를 포함해 시도 8곳 · 시군구 19곳 · 로드맵 25개.
 * (강원 속초 · 강릉 · 양양 · 춘천 · 평창 / 서울 종로 · 용산 · 마포 · 강남 / 부산 해운대 · 수영 · 중구 / 제주 제주 · 서귀포 /
 * 경기 수원 · 가평 / 전북 전주 / 경북 경주 / 전남 여수)
 * 서버(공개 trip_mst · trip_place_dtl · trip_place_tag_rel · tag_mst)가 생기기 전까지만 쓰는 임시 값 · 이 파일 한 곳에만 둠.
 * 서버가 생기면 debug source set 으로 옮김(지금은 공모전 제출 APK 가 release 라 main 에 둠 · 화면에 「시연용 데이터」).
 * 시군구 목록 · 시군구별 로드맵 수 · 태그별 개수 · 대표 태그는 여기 따로 적지 않고 로드맵에서 뽑거나 도메인 계산(RecommendationRules)이 셈.
 * 번호: 시군구마다 번호대 하나 — trip_no = 대 × 100 + n · place_no = 대 × 1000 + 10 × (n − 1) + 순서(대 1 ~ 19 · 최대 19,014).
 * 내 공개 여행의 추천 번호(AppConfig.LOCAL_TRIP_RECO_NO_OFFSET 1,000,000 + trip_no)와 겹치지 않음.
 * 닉네임 · 여행 이름 · 별점은 지어낸 시연 값 · 장소 이름 · 주소 · 좌표는 실제 장소(B-05 「안내 시작」이 실제 내비에 경로를 띄움).
 * 주소는 시도 짧은 이름(SidoNames) + 시군구로 시작 · B-04 경로 그림에서 핀이 겹치지 않게 아주 가까운 장소는 좌표를 조금 벌림.
 * 동행자는 표준 「결정」값(SOLO · PARENT · PET) 위주 · 「미결」(SPOUSE · CHILD)은 몰리지 않게.
 */
internal object RecommendationSeed {

    private val KIDS = Tag(tagNo = 1, tagNm = "아이와 가기 좋아요")
    private val PARKING = Tag(tagNo = 2, tagNm = "주차 편해요")
    private val AGAIN = Tag(tagNo = 3, tagNm = "다시 가고 싶어요")
    private val WAITING = Tag(tagNo = 4, tagNm = "웨이팅 있어요")
    private val RESTROOM = Tag(tagNo = 5, tagNm = "화장실 깨끗해요")

    /** 태그 마스터(tag_mst) — 별점 「좋았던 점」 칩(#41). */
    val tags: List<Tag> = listOf(KIDS, PARKING, AGAIN, WAITING, RESTROOM)

    private const val GANGWON = "강원"
    private val SOKCHO = Region(GANGWON, "속초시")
    private val GANGNEUNG = Region(GANGWON, "강릉시")
    private val YANGYANG = Region(GANGWON, "양양군")
    private val CHUNCHEON = Region(GANGWON, "춘천시")
    private val PYEONGCHANG = Region(GANGWON, "평창군")

    private const val SEOUL = "서울"
    private val JONGNO = Region(SEOUL, "종로구")
    private val YONGSAN = Region(SEOUL, "용산구")
    private val MAPO = Region(SEOUL, "마포구")
    private val GANGNAM = Region(SEOUL, "강남구")

    private const val BUSAN = "부산"
    private val HAEUNDAE = Region(BUSAN, "해운대구")
    private val SUYEONG = Region(BUSAN, "수영구")
    private val BUSAN_JUNG = Region(BUSAN, "중구")

    private const val JEJU = "제주"
    private val JEJU_SI = Region(JEJU, "제주시")
    private val SEOGWIPO = Region(JEJU, "서귀포시")

    private const val GYEONGGI = "경기"
    private val SUWON = Region(GYEONGGI, "수원시")
    private val GAPYEONG = Region(GYEONGGI, "가평군")

    private val JEONJU = Region("전북", "전주시")
    private val GYEONGJU = Region("경북", "경주시")
    private val YEOSU = Region("전남", "여수시")

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
                place(1002, "중앙시장 닭강정", 2, "강원 속초시 중앙로", 38.2075, 128.5950, WAITING, KIDS),
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
                place(1022, "신흥사", 2, "강원 속초시 설악산로", 38.1690, 128.4950, AGAIN, KIDS),
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
                place(3002, "하조대", 2, "강원 양양군 현북면", 38.0200, 128.7320, PARKING),
                place(3003, "낙산사", 3, "강원 양양군 강현면", 38.1248, 128.6278, AGAIN),
            ),
        ),
        RecommendedTrip(
            tripNo = 401,
            tripNm = "춘천 호수 한 바퀴",
            nicknameNm = "몽실이네",
            region = CHUNCHEON,
            companionTypes = setOf(CompanionType.PET),
            ratingAvg = 4.5,
            ratingCnt = 8,
            publishedAt = 1_762_150_000_000,
            places = listOf(
                place(4001, "소양강 스카이워크", 1, "강원 춘천시 영서로 2663", 37.8945, 127.7230, PARKING, AGAIN),
                place(4002, "공지천 조각공원", 2, "강원 춘천시 근화동", 37.8735, 127.7155, KIDS),
                place(4003, "춘천 명동 닭갈비골목", 3, "강원 춘천시 금강로62번길", 37.8810, 127.7280, WAITING),
                place(4004, "김유정문학촌", 4, "강원 춘천시 신동면 김유정로 1430-14", 37.8165, 127.7175, RESTROOM),
            ),
        ),
        RecommendedTrip(
            tripNo = 501,
            tripNm = "대관령 바람 쐬기",
            nicknameNm = "효도여행",
            region = PYEONGCHANG,
            companionTypes = setOf(CompanionType.PARENT),
            ratingAvg = 4.6,
            ratingCnt = 10,
            publishedAt = 1_762_250_000_000,
            places = listOf(
                place(5001, "대관령 양떼목장", 1, "강원 평창군 대관령면 대관령마루길 483-32", 37.6880, 128.7530, KIDS, AGAIN),
                place(5002, "월정사", 2, "강원 평창군 진부면 오대산로 374-8", 37.7317, 128.5925, RESTROOM),
                place(5003, "오대산 전나무숲길", 3, "강원 평창군 진부면 오대산로", 37.7270, 128.6000, AGAIN, PARKING),
                place(5004, "봉평 효석문화마을", 4, "강원 평창군 봉평면 창동리", 37.6170, 128.3600, PARKING),
            ),
        ),
        RecommendedTrip(
            tripNo = 601,
            tripNm = "궁궐 한 바퀴",
            nicknameNm = "고궁산책",
            region = JONGNO,
            companionTypes = setOf(CompanionType.PARENT),
            ratingAvg = 4.9,
            ratingCnt = 21,
            publishedAt = 1_762_350_000_000,
            places = listOf(
                place(6001, "경복궁", 1, "서울 종로구 사직로 161", 37.5796, 126.9770, AGAIN, RESTROOM),
                place(6002, "국립민속박물관", 2, "서울 종로구 삼청로 37", 37.5830, 126.9800, KIDS, RESTROOM),
                place(6003, "북촌한옥마을", 3, "서울 종로구 계동", 37.5826, 126.9860, AGAIN, WAITING),
                place(6004, "창덕궁", 4, "서울 종로구 율곡로 99", 37.5794, 126.9910, AGAIN),
            ),
        ),
        RecommendedTrip(
            tripNo = 602,
            tripNm = "익선동 골목 산책",
            nicknameNm = "골목대장",
            region = JONGNO,
            companionTypes = setOf(CompanionType.SOLO),
            ratingAvg = 4.4,
            ratingCnt = 7,
            publishedAt = 1_762_650_000_000,
            places = listOf(
                place(6011, "익선동 한옥거리", 1, "서울 종로구 익선동", 37.5743, 126.9897, WAITING),
                place(6012, "인사동 쌈지길", 2, "서울 종로구 인사동길 44", 37.5741, 126.9849, AGAIN),
                place(6013, "광장시장", 3, "서울 종로구 창경궁로 88", 37.5701, 126.9996, WAITING, AGAIN),
            ),
        ),
        RecommendedTrip(
            tripNo = 701,
            tripNm = "박물관 가는 날",
            nicknameNm = "주말나들이",
            region = YONGSAN,
            companionTypes = setOf(CompanionType.PARENT, CompanionType.CHILD),
            ratingAvg = 4.7,
            ratingCnt = 13,
            publishedAt = 1_762_450_000_000,
            places = listOf(
                place(7001, "국립중앙박물관", 1, "서울 용산구 서빙고로 137", 37.5240, 126.9803, KIDS, PARKING, RESTROOM),
                place(7002, "용산가족공원", 2, "서울 용산구 용산동6가", 37.5215, 126.9870, KIDS),
                place(7003, "전쟁기념관", 3, "서울 용산구 이태원로 29", 37.5365, 126.9772, KIDS, RESTROOM),
                place(7004, "남산서울타워", 4, "서울 용산구 남산공원길 105", 37.5512, 126.9882, AGAIN, WAITING),
            ),
        ),
        RecommendedTrip(
            tripNo = 801,
            tripNm = "한강 따라 망원",
            nicknameNm = "산책하는보리",
            region = MAPO,
            companionTypes = setOf(CompanionType.PET),
            ratingAvg = 4.6,
            ratingCnt = 11,
            publishedAt = 1_762_550_000_000,
            places = listOf(
                place(8001, "망원한강공원", 1, "서울 마포구 마포나루길 467", 37.5551, 126.8960, PARKING, AGAIN),
                place(8002, "망원시장", 2, "서울 마포구 포은로8길 14", 37.5560, 126.9060, WAITING),
                place(8003, "경의선숲길 연남동 구간", 3, "서울 마포구 연남동", 37.5600, 126.9230, AGAIN),
                place(8004, "하늘공원", 4, "서울 마포구 하늘공원로 95", 37.5680, 126.8850, RESTROOM),
            ),
        ),
        RecommendedTrip(
            tripNo = 901,
            tripNm = "비 오는 날 코엑스",
            nicknameNm = "혼자서도",
            region = GANGNAM,
            companionTypes = setOf(CompanionType.SOLO),
            ratingAvg = 4.2,
            ratingCnt = 5,
            publishedAt = 1_762_700_000_000,
            places = listOf(
                place(9001, "코엑스 별마당도서관", 1, "서울 강남구 영동대로 513", 37.5103, 127.0600, RESTROOM, PARKING),
                place(9002, "봉은사", 2, "서울 강남구 봉은사로 531", 37.5147, 127.0577, AGAIN),
                place(9003, "선릉과 정릉", 3, "서울 강남구 선릉로100길 1", 37.5087, 127.0489, RESTROOM),
            ),
        ),
        RecommendedTrip(
            tripNo = 1001,
            tripNm = "해운대 바다 산책",
            nicknameNm = "콩이랑",
            region = HAEUNDAE,
            companionTypes = setOf(CompanionType.PET),
            ratingAvg = 4.7,
            ratingCnt = 18,
            publishedAt = 1_762_320_000_000,
            places = listOf(
                place(10001, "동백섬", 1, "부산 해운대구 우동", 35.1530, 129.1520, AGAIN),
                place(10002, "해운대해수욕장", 2, "부산 해운대구 해운대해변로 264", 35.1587, 129.1604, PARKING, RESTROOM),
                place(10003, "블루라인파크 미포정거장", 3, "부산 해운대구 중동", 35.1585, 129.1720, WAITING, KIDS),
                place(10004, "청사포", 4, "부산 해운대구 중동", 35.1600, 129.1910, AGAIN),
            ),
        ),
        RecommendedTrip(
            tripNo = 1002,
            tripNm = "달맞이길 드라이브",
            nicknameNm = "둘이서",
            region = HAEUNDAE,
            companionTypes = setOf(CompanionType.SPOUSE),
            ratingAvg = 4.3,
            ratingCnt = 6,
            publishedAt = 1_762_620_000_000,
            places = listOf(
                place(10011, "APEC나루공원", 1, "부산 해운대구 우동", 35.1700, 129.1310, PARKING),
                place(10012, "해월정", 2, "부산 해운대구 중동", 35.1580, 129.1800, AGAIN),
                place(10013, "송정해수욕장", 3, "부산 해운대구 송정해변로 62", 35.1786, 129.1997, PARKING, RESTROOM),
            ),
        ),
        RecommendedTrip(
            tripNo = 1101,
            tripNm = "광안리 야경",
            nicknameNm = "밤바다",
            region = SUYEONG,
            companionTypes = setOf(CompanionType.SOLO),
            ratingAvg = 4.8,
            ratingCnt = 16,
            publishedAt = 1_762_420_000_000,
            places = listOf(
                place(11001, "남천동 벚꽃길", 1, "부산 수영구 남천동", 35.1440, 129.1130, AGAIN),
                place(11002, "광안리해수욕장", 2, "부산 수영구 광안해변로 219", 35.1532, 129.1186, AGAIN, WAITING),
                place(11003, "민락수변공원", 3, "부산 수영구 민락동", 35.1550, 129.1330, RESTROOM),
            ),
        ),
        RecommendedTrip(
            tripNo = 1201,
            tripNm = "남포동 시장 한 바퀴",
            nicknameNm = "엄마랑부산",
            region = BUSAN_JUNG,
            companionTypes = setOf(CompanionType.PARENT),
            ratingAvg = 4.4,
            ratingCnt = 9,
            publishedAt = 1_762_520_000_000,
            places = listOf(
                place(12001, "용두산공원", 1, "부산 중구 용두산길 37-55", 35.1007, 129.0325, AGAIN, RESTROOM),
                place(12002, "국제시장", 2, "부산 중구 신창동4가", 35.1015, 129.0280, WAITING),
                place(12003, "BIFF광장", 3, "부산 중구 남포동", 35.0985, 129.0270, WAITING),
                place(12004, "자갈치시장", 4, "부산 중구 자갈치해안로 52", 35.0966, 129.0306, PARKING, AGAIN),
            ),
        ),
        RecommendedTrip(
            tripNo = 1301,
            tripNm = "애월 해안 드라이브",
            nicknameNm = "제주강아지",
            region = JEJU_SI,
            companionTypes = setOf(CompanionType.PET),
            ratingAvg = 4.6,
            ratingCnt = 13,
            publishedAt = 1_762_220_000_000,
            places = listOf(
                place(13001, "동문재래시장", 1, "제주 제주시 관덕로14길 20", 33.5122, 126.5270, WAITING),
                place(13002, "한담해안산책로", 2, "제주 제주시 애월읍 곽지리", 33.4620, 126.3100, AGAIN),
                place(13003, "곽지해수욕장", 3, "제주 제주시 애월읍 곽지리", 33.4505, 126.3045, PARKING, RESTROOM),
                place(13004, "협재해수욕장", 4, "제주 제주시 한림읍 협재리", 33.3940, 126.2397, AGAIN, PARKING),
            ),
        ),
        RecommendedTrip(
            tripNo = 1401,
            tripNm = "성산 일출 보러",
            nicknameNm = "부모님과제주",
            region = SEOGWIPO,
            companionTypes = setOf(CompanionType.PARENT),
            ratingAvg = 4.9,
            ratingCnt = 24,
            publishedAt = 1_762_120_000_000,
            places = listOf(
                place(14001, "성산일출봉", 1, "제주 서귀포시 성산읍 일출로 284-12", 33.4580, 126.9425, AGAIN, PARKING),
                place(14002, "광치기해변", 2, "제주 서귀포시 성산읍 고성리", 33.4510, 126.9210, AGAIN),
                place(14003, "섭지코지", 3, "제주 서귀포시 성산읍 섭지코지로 107", 33.4240, 126.9310, RESTROOM, PARKING),
            ),
        ),
        RecommendedTrip(
            tripNo = 1402,
            tripNm = "올레 7코스 반나절",
            nicknameNm = "걷는사람",
            region = SEOGWIPO,
            companionTypes = setOf(CompanionType.SOLO),
            ratingAvg = 4.5,
            ratingCnt = 9,
            publishedAt = 1_762_680_000_000,
            places = listOf(
                place(14011, "외돌개", 1, "제주 서귀포시 서홍동", 33.2400, 126.5450, AGAIN),
                place(14012, "천지연폭포", 2, "제주 서귀포시 천지동", 33.2470, 126.5545, RESTROOM),
                place(14013, "서귀포매일올레시장", 3, "제주 서귀포시 중앙로62번길 18", 33.2500, 126.5630, WAITING),
                place(14014, "정방폭포", 4, "제주 서귀포시 동홍동", 33.2450, 126.5718, PARKING),
            ),
        ),
        RecommendedTrip(
            tripNo = 1501,
            tripNm = "화성 성곽 걷기",
            nicknameNm = "성곽러",
            region = SUWON,
            companionTypes = setOf(CompanionType.SOLO),
            ratingAvg = 4.6,
            ratingCnt = 10,
            publishedAt = 1_762_380_000_000,
            places = listOf(
                place(15001, "장안문", 1, "경기 수원시 장안구 영화동", 37.2890, 127.0146, AGAIN),
                place(15002, "방화수류정", 2, "경기 수원시 팔달구 북수동", 37.2878, 127.0196, AGAIN, RESTROOM),
                place(15003, "화성행궁", 3, "경기 수원시 팔달구 정조로 825", 37.2818, 127.0135, KIDS, PARKING),
                place(15004, "수원 통닭거리", 4, "경기 수원시 팔달구 팔달로1가", 37.2795, 127.0175, WAITING),
            ),
        ),
        RecommendedTrip(
            tripNo = 1601,
            tripNm = "가평 강아지랑 하루",
            nicknameNm = "두부산책",
            region = GAPYEONG,
            companionTypes = setOf(CompanionType.PET),
            ratingAvg = 4.4,
            ratingCnt = 8,
            publishedAt = 1_762_480_000_000,
            places = listOf(
                place(16001, "아침고요수목원", 1, "경기 가평군 상면 수목원로 432", 37.7440, 127.3530, AGAIN, RESTROOM),
                place(16002, "쁘띠프랑스", 2, "경기 가평군 청평면 호반로 1063", 37.7190, 127.4890, KIDS),
                place(16003, "자라섬", 3, "경기 가평군 가평읍 달전리", 37.8180, 127.5160, PARKING),
                place(16004, "가평나루", 4, "경기 가평군 가평읍 북한강변로 1024", 37.8130, 127.5250, PARKING, WAITING),
            ),
        ),
        RecommendedTrip(
            tripNo = 1701,
            tripNm = "한옥마을 먹방",
            nicknameNm = "전주엄마",
            region = JEONJU,
            companionTypes = setOf(CompanionType.PARENT),
            ratingAvg = 4.7,
            ratingCnt = 14,
            publishedAt = 1_762_280_000_000,
            places = listOf(
                place(17001, "경기전", 1, "전북 전주시 완산구 태조로 44", 35.8154, 127.1500, AGAIN, KIDS),
                place(17002, "전동성당", 2, "전북 전주시 완산구 태조로 51", 35.8132, 127.1480, AGAIN),
                place(17003, "남부시장", 3, "전북 전주시 완산구 전동", 35.8120, 127.1458, WAITING),
                place(17004, "오목대", 4, "전북 전주시 완산구 교동", 35.8140, 127.1565, RESTROOM),
            ),
        ),
        RecommendedTrip(
            tripNo = 1801,
            tripNm = "신라 유적 산책",
            nicknameNm = "누룽지네",
            region = GYEONGJU,
            companionTypes = setOf(CompanionType.PET),
            ratingAvg = 4.8,
            ratingCnt = 19,
            publishedAt = 1_762_580_000_000,
            places = listOf(
                place(18001, "불국사", 1, "경북 경주시 불국로 385", 35.7900, 129.3320, AGAIN, PARKING),
                place(18002, "대릉원", 2, "경북 경주시 황남동", 35.8380, 129.2120, AGAIN),
                place(18003, "첨성대", 3, "경북 경주시 인왕동", 35.8347, 129.2190, KIDS),
                place(18004, "동궁과 월지", 4, "경북 경주시 원화로 102", 35.8349, 129.2266, AGAIN, WAITING),
            ),
        ),
        RecommendedTrip(
            tripNo = 1901,
            tripNm = "여수 밤바다",
            nicknameNm = "바닷바람",
            region = YEOSU,
            companionTypes = setOf(CompanionType.SOLO),
            ratingAvg = 4.5,
            ratingCnt = 12,
            publishedAt = 1_762_330_000_000,
            places = listOf(
                place(19001, "향일암", 1, "전남 여수시 돌산읍 향일암로 60", 34.5920, 127.8020, AGAIN),
                place(19002, "여수해상케이블카 돌산탑승장", 2, "전남 여수시 돌산읍 돌산로 3600-1", 34.7340, 127.7470, WAITING),
                place(19003, "이순신광장", 3, "전남 여수시 중앙동", 34.7385, 127.7365, PARKING),
                place(19004, "오동도", 4, "전남 여수시 오동도로 222", 34.7440, 127.7660, AGAIN, RESTROOM),
            ),
        ),
    )

    /**
     * 시도 목록은 앱 안 한 곳(SidoNames) — 시군구는 위 로드맵에서 뽑음(시드에 처음 나온 순서 · 시드가 없는 시도는 빈 목록).
     * trips 아래에 둬야 초기화 순서가 맞음.
     */
    val sidoRegions: List<SidoRegion> = SidoNames.ALL.map { sido ->
        SidoRegion(sido, trips.filter { it.region.sidoNm == sido }.mapNotNull { it.region.sigunguNm }.distinct())
    }

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
