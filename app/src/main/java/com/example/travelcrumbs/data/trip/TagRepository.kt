package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.local.TagMstEntity
import com.example.travelcrumbs.data.local.TravelDao
import com.example.travelcrumbs.data.recommendation.RecommendationSeed
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.Tag
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * 경험 태그(tag_mst · 해요체)의 유일한 출처(#51 U1 · A-03 칩 · A-05 · B-06 「좋았던 점」).
 * 처음 읽을 때 기기 안 표에 시드(추천 시드와 같은 번호 · 이름 — 공개 로드맵 태그 수가 맞게)를 넣음. 서버 tag_mst 가 생기면 구현만 바꿈.
 */
interface TagRepository {
    /** 쓰는 태그(use_yn = Y) · sort_ord 순. */
    val tags: Flow<List<Tag>>
}

@Singleton
class LocalTagRepository @Inject constructor(
    private val dao: TravelDao,
    private val timeSource: TimeSource,
) : TagRepository {

    override val tags: Flow<List<Tag>> = flow {
        val now = timeSource.nowMillis()
        // 이미 있으면 그대로(IGNORE) — 사람이 use_yn 을 바꾼 행을 덮지 않음
        dao.insertTags(
            RecommendationSeed.tags.mapIndexed { index, tag ->
                TagMstEntity(
                    tagNo = tag.tagNo,
                    tagNm = tag.tagNm,
                    useYn = "Y",
                    sortOrd = index + 1,
                    createdAt = now,
                    createdUserNo = AppConfig.LOCAL_USER_NO,
                    updatedAt = now,
                    updatedUserNo = AppConfig.LOCAL_USER_NO,
                )
            },
        )
        emitAll(dao.tags().map { rows -> rows.map { Tag(it.tagNo, it.tagNm) } })
    }
}
