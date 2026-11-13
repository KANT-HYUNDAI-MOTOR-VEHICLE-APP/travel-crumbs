package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.model.GleoCommandPhase
import com.example.travelcrumbs.domain.model.GleoScreenRequest
import com.example.travelcrumbs.domain.model.GleoStatus

/**
 * 여행 제어 · 정보 조회(#69) 한 번의 결과 — 콜백([status] · [message] · [data])과 화면([phase] · [detail] · [screen])을 함께 담음.
 * [message] 는 영어 완전한 문장(Gleo 가 말로 바꿈) · [data] 는 JSON 배열 문자열.
 */
data class GleoOutcome(
    val phase: GleoCommandPhase,
    val status: GleoStatus,
    val message: String,
    val data: String? = null,
    val detail: String? = null,
    val screen: GleoScreenRequest? = null,
)
