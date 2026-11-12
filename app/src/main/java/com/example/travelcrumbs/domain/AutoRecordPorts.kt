package com.example.travelcrumbs.domain

/**
 * 「여행 종료」를 한 번에 맡는 동작(#66 스펙 3.8 「여행 종료 때 확정」) — 기록 대기 확정 → 여행 종료 순서.
 * 정차 홈 · 주행 화면이 함께 씀 · 구현은 data(기록 상태 저장소는 대기 · 확정을 모름).
 * 돌려주는 값 = 이번에 정한 종료 시각(이미 끝난 여행이면 null · RecordingRepository.endTrip 과 같음).
 */
fun interface EndTripUseCase {
    suspend operator fun invoke(): Long?
}

/**
 * 위치 정보를 지우는 동작들(#66 스펙 3.8 「철회 · 모두 지우기 · 여행 삭제 때 정리」) — 지금 부르는 곳(설정 · A-08)이 이 길로 부름.
 * 기록 대기 · 자동 이름 원본 · 진행 중 직접 안내를 같은 흐름에서 정리 · 구현은 data.
 */
interface PlaceRecordCleanup {
    /** 위치 수집 동의 철회 뒤(동의 행을 남긴 다음) — 좌표 · 주소 · 처음 이름(주소 · 자동) 비움 + 대기 · 원본 삭제(3.9). */
    suspend fun withdrawLocation()

    /** A-08 「여행 삭제」 — 그 여행 + 그 여행의 대기 · 원본. */
    suspend fun deleteTrip(tripNo: Long)

    /** C-03 「내 기록 모두 지우기」 — 내 기록 전부 + 대기 · 원본. */
    suspend fun deleteAll()
}
