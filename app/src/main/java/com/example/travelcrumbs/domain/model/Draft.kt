package com.example.travelcrumbs.domain.model

/**
 * 주행으로 바뀌는 순간 입력 중이던 내용(스펙 app-shell 4-1). 한 번에 하나.
 * [fields] = 입력 칸 이름 → 내용. 내용은 기기 안에만 · 로그에 찍지 않음.
 */
data class Draft(
    val destinationId: String,
    val fields: Map<String, String>,
    val savedAtMillis: Long,
)
