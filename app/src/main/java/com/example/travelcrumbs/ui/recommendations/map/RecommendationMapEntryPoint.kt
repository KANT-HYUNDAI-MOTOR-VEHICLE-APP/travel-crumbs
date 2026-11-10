package com.example.travelcrumbs.ui.recommendations.map

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 추천 지도 화면 부품이 [RecommendationMapHolder] 를 받는 길(#59 · 스펙 N16 ①) — 좌표가 ViewModel 을 거치지 않게
 * 화면 부품이 직접 받음(저장소는 보관 객체 안에서만 · 규칙 4).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RecommendationMapEntryPoint {
    fun recommendationMapHolder(): RecommendationMapHolder
}
