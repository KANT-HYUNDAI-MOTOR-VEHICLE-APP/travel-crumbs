package com.example.travelcrumbs.ui.components

import com.example.travelcrumbs.domain.model.GuidedTrip
import com.example.travelcrumbs.domain.model.MyRating
import com.example.travelcrumbs.domain.model.Tag
import java.time.Instant
import java.time.ZoneId

/**
 * 별점 팝업(#41 · B-06)이 그릴 것 — 기록 칸 자동 팝업과 추천 B-04 「별점 주기」가 같은 모양을 씀.
 * 닉네임은 로드맵 작성자(공개 로드맵에 이미 보이는 이름)뿐 · 별점을 주는 사람(나)의 이름 칸은 없음.
 */
data class RatingDialogItem(
    val tripNo: Long,
    val tripNm: String,
    val nicknameNm: String,
    /** 안내받은 날(기기 시간대) — 「11월 15일 안내」. */
    val guidedMonth: Int,
    val guidedDay: Int,
    /** 고른 별 수 — null 이면 아직 안 고름(「별점 주기」 꺼짐). */
    val ratingScore: Int?,
    val tags: List<RatingTagItem>,
    /** 저장 중 — 「별점 주기」를 한 번만 받음. */
    val saving: Boolean = false,
) {
    val canSubmit: Boolean get() = ratingScore != null && !saving
}

/** 「좋았던 점」 칩 하나. */
data class RatingTagItem(
    val tagNo: Long,
    val tagNm: String,
    val selected: Boolean,
)

/** 팝업에서 고르는 중인 값 — ViewModel 이 들고 사용자 행동으로만 바꿈. */
data class RatingDraft(
    val tripNo: Long,
    val ratingScore: Int? = null,
    val tagNos: Set<Long> = emptySet(),
) {
    fun toggleTag(tagNo: Long): RatingDraft = copy(tagNos = if (tagNo in tagNos) tagNos - tagNo else tagNos + tagNo)

    fun toItem(guided: GuidedTrip, tags: List<Tag>, saving: Boolean = false): RatingDialogItem {
        val date = Instant.ofEpochMilli(guided.guidedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        return RatingDialogItem(
            tripNo = tripNo,
            tripNm = guided.tripNm,
            nicknameNm = guided.nicknameNm,
            guidedMonth = date.monthValue,
            guidedDay = date.dayOfMonth,
            ratingScore = ratingScore,
            tags = tags.map { RatingTagItem(it.tagNo, it.tagNm, it.tagNo in tagNos) },
            saving = saving,
        )
    }

    companion object {
        /** 처음 여는 팝업 — 이미 준 별점이 있으면 그 값으로 채움(고치기). */
        fun of(tripNo: Long, existing: MyRating?): RatingDraft =
            RatingDraft(tripNo = tripNo, ratingScore = existing?.ratingScore, tagNos = existing?.tagNos.orEmpty())
    }
}
