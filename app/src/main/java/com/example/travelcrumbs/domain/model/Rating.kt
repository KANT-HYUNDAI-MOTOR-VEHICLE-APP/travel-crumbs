package com.example.travelcrumbs.domain.model

/*
 * 별점(#41 · 와이어프레임 v6 B-06) 도메인 모델 — 기기 안에만 저장(서버 · 표준 등록 전).
 * 별점을 준 사람(나)의 닉네임 칸은 없음 — 랭킹에는 평균 · 참여 수 · 태그 수로만 더해짐.
 * 「로드맵 별점」은 데이터 표준에 아직 없는 제안 — 서버 표준 등록 때 이름을 맞춤.
 */

/** 안내를 요청한 로드맵 하나 — 별점 자격(안내 요청 = 다녀온 것으로 봄 · 이슈 #41). 이름 · 닉네임은 팝업 표시용. */
data class GuidedTrip(
    val tripNo: Long,
    val tripNm: String,
    val nicknameNm: String,
    /** 처음 안내를 요청한 시각(epoch 밀리초) — 팝업 「11월 15일 안내」. */
    val guidedAt: Long,
    /**
     * 안내 뒤 진행한 여행을 「여행 종료」로 끝낸 시각 — 있으면 별점 자동 팝업 대상(사람 결정 · 중간 정차엔 안 뜸).
     * null = 아직 끝낸 여행 없음(안내만 받음 → 자동 팝업 없이 B-04 「별점 주기」로만).
     */
    val tripEndedAt: Long? = null,
)

/** 내가 준 별점 — 로드맵 하나에 하나 · 다시 주면 바뀜. */
data class MyRating(
    val tripNo: Long,
    /** 별 1 ~ 5. */
    val ratingScore: Int,
    /** 좋았던 점 태그 번호(선택 · 여러 개). */
    val tagNos: Set<Long>,
    val ratedAt: Long,
)

/** 기기 안 별점 기록 전부(저장 단위 하나). */
data class RatingRecords(
    val guidedTrips: List<GuidedTrip> = emptyList(),
    val ratings: List<MyRating> = emptyList(),
    /** 자동 팝업을 이미 띄웠던(「나중에」 · 「별점 주기」) 로드맵 — 다시 자동으로 안 뜸. */
    val promptedTripNos: Set<Long> = emptySet(),
) {
    fun guided(tripNo: Long): GuidedTrip? = guidedTrips.firstOrNull { it.tripNo == tripNo }

    fun ratingOf(tripNo: Long): MyRating? = ratings.firstOrNull { it.tripNo == tripNo }

    /** 로드맵 번호 → 내 별점. */
    val ratingsByTrip: Map<Long, MyRating> get() = ratings.associateBy { it.tripNo }
}
