package com.example.travelcrumbs.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 핀 표(스펙 navigation 4.1 · U43 — Room · 상한 없음 · 삭제 화면 없음) — 안내 시작 1건 = 1행 · 서버로 보낼 전송 대기 큐.
 * 표 이름은 데이터 표준 정의서의 길안내 요청 로그(`navi_req_log` · T01 로그 · T02 · R02)와 같게 씀(android-architecture 「Room 이름은 서버와 같게」).
 * 칸 = 여행 안 순서 · 장소 id · 시각(시간대 포함) · 보냄 상태뿐 — 계정 · 좌표 · 이름 · 주소 · 결과 없음.
 * placeId · utcOffsetSeconds · uploadState 는 표준 용어 사전에 맞는 용어가 아직 없어 이름을 그대로 둠(정의서 등록 뒤 바꿈).
 */
@Entity(tableName = "navi_req_log")
data class NaviReqLogEntity(
    /** 여행 안 순서(1부터) — 지금은 내 핀 전체가 한 여행(#6 D2 · #3 이 여행을 나누면 여행별). */
    @PrimaryKey
    @ColumnInfo(name = "navi_req_log_no")
    val sequence: Int,
    val placeId: String,
    /** 안내 시작 시각(epoch 밀리초) — 이 행이 생긴 때. */
    @ColumnInfo(name = "created_at")
    val startedAtMillis: Long,
    val utcOffsetSeconds: Int,
    /** PinUploadState 이름(PENDING · SENT · REJECTED). */
    val uploadState: String,
)
