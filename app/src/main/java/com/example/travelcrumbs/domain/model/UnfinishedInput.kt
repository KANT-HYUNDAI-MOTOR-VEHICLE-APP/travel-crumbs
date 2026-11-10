package com.example.travelcrumbs.domain.model

/**
 * 작성 중 내용(스펙 app-shell-v5 4-1 · D27) — 주행으로 바뀌는 순간 입력 중이던 내용. 한 번에 하나.
 * [fields] = 입력 칸 이름 → 내용. 내용은 기기 안에만 · 로그에 찍지 않음.
 */
data class UnfinishedInput(
    val destinationId: String,
    val fields: Map<String, String>,
    val savedAtMillis: Long,
)

/**
 * 주행 중 기록한 임시저장 장소(#51 U4) — 「이어서 쓰기」 띠가 정차 뒤 A-03 시트를 그 장소로 엶.
 * [DESTINATION_ID] 는 앱 틀 목적지 「기록」의 id(ui/navigation/AppDestination.RECORDS.id)와 같은 글자 — 시험이 고정.
 */
object DraftPlaceInput {
    const val DESTINATION_ID: String = "records"
    const val FIELD_TRIP_PLACE_NO: String = "trip_place_no"
}
