package com.example.travelcrumbs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 핀 표(스펙 navigation 4.1 · U43 — Room · 상한 없음 · 삭제 화면 없음).
 * 칸 = 여행 안 순서 · 장소 id · 시각(시간대 포함) · 보냄 상태뿐 — 계정 · 좌표 · 이름 · 주소 · 결과 없음.
 */
@Entity(tableName = "pins")
data class PinEntity(
    /** 여행 안 순서(1부터) — 지금은 내 핀 전체가 한 여행(#6 D2 · #3 이 여행을 나누면 여행별). */
    @PrimaryKey val sequence: Int,
    val placeId: String,
    val startedAtMillis: Long,
    val utcOffsetSeconds: Int,
    /** PinUploadState 이름(PENDING · SENT · REJECTED). */
    val uploadState: String,
)
