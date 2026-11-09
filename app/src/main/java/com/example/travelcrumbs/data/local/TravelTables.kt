package com.example.travelcrumbs.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * 기기 안 여행 표(#51 U1 · Room 판 3) — 표 · 칸 이름은 docs/data/standard.md 그대로(서버 PostgreSQL 과 같은 이름 · android-architecture).
 * 데이터 원본은 서버(ADR 0015) — 지금은 서버 API 전이라 기기 안 임시 원본 · 서버가 생기면 캐시 · 전송 대기 큐로 바뀜.
 * 일시 칸(*_at)은 epoch 밀리초(UTC) · 여부 칸(*_yn)은 "Y" / "N" · 코드 칸(*_cd)은 표준 코드값 영문 대문자.
 */

/**
 * 사용자 마스터(user_mst) — 기기 안 기본 사용자 1명(로그인 화면 전 · #51 U1). 로그인이 생기면 서버 user_mst 의 캐시.
 */
@Entity(tableName = "user_mst")
data class UserMstEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_no")
    val userNo: Long,
    @ColumnInfo(name = "login_id")
    val loginId: String? = null,
    @ColumnInfo(name = "nickname_nm")
    val nicknameNm: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "created_user_no")
    val createdUserNo: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "updated_user_no")
    val updatedUserNo: Long,
    @ColumnInfo(name = "deleted_yn")
    val deletedYn: String = "N",
)

/**
 * 사용자 동의 이력(user_consent_hist) — 동의 · 철회를 행으로 쌓음(R12 · R14) · 유형별 최신 행이 지금 상태.
 */
@Entity(tableName = "user_consent_hist", indices = [Index("user_no")])
data class UserConsentHistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_consent_hist_no")
    val userConsentHistNo: Long = 0,
    @ColumnInfo(name = "user_no")
    val userNo: Long,
    @ColumnInfo(name = "consent_type_cd")
    val consentTypeCd: String,
    @ColumnInfo(name = "consent_yn")
    val consentYn: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "created_user_no")
    val createdUserNo: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "updated_user_no")
    val updatedUserNo: Long,
    @ColumnInfo(name = "deleted_yn")
    val deletedYn: String = "N",
)

/**
 * 여행 마스터(trip_mst) — 기록 시작부터 종료까지 한 번. 지금 여행 = 사용자의 가장 최근 행(#51 U1 · 옛 DataStore 요약 대신).
 * accumulated_millis · segment_start_millis(일시정지 계산) · dest_place_nm(목적지 이름)은 표준 용어에 없음 — 보고서 「표준 밖」.
 */
@Entity(tableName = "trip_mst", indices = [Index("user_no")])
data class TripMstEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "trip_no")
    val tripNo: Long = 0,
    @ColumnInfo(name = "user_no")
    val userNo: Long,
    @ColumnInfo(name = "trip_nm")
    val tripNm: String? = null,
    @ColumnInfo(name = "trip_status_cd")
    val tripStatusCd: String,
    @ColumnInfo(name = "visibility_cd")
    val visibilityCd: String,
    @ColumnInfo(name = "start_at")
    val startAt: Long,
    @ColumnInfo(name = "end_at")
    val endAt: Long? = null,
    @ColumnInfo(name = "duration")
    val duration: Long? = null,
    @ColumnInfo(name = "dest_place_nm")
    val destPlaceNm: String? = null,
    @ColumnInfo(name = "accumulated_millis")
    val accumulatedMillis: Long,
    @ColumnInfo(name = "segment_start_millis")
    val segmentStartMillis: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "created_user_no")
    val createdUserNo: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "updated_user_no")
    val updatedUserNo: Long,
    @ColumnInfo(name = "deleted_yn")
    val deletedYn: String = "N",
)

/**
 * 여행 동행자 내역(trip_companion_dtl) — 여행 하나에 동행자 유형 여러 행.
 */
@Entity(tableName = "trip_companion_dtl", indices = [Index("trip_no")])
data class TripCompanionDtlEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "trip_companion_no")
    val tripCompanionNo: Long = 0,
    @ColumnInfo(name = "trip_no")
    val tripNo: Long,
    @ColumnInfo(name = "companion_type_cd")
    val companionTypeCd: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "created_user_no")
    val createdUserNo: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "updated_user_no")
    val updatedUserNo: Long,
    @ColumnInfo(name = "deleted_yn")
    val deletedYn: String = "N",
)

/**
 * 여행 장소 내역(trip_place_dtl) — 「이 장소 기록」 한 번 = 한 행. 좌표 · 주소는 기기 안에만(로그 · 화면 상태 · 서버에 넣지 않음).
 * user_place_nm · addr 는 표준 용어에 없음 — 보고서 「표준 밖」.
 */
@Entity(tableName = "trip_place_dtl", indices = [Index("trip_no")])
data class TripPlaceDtlEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "trip_place_no")
    val tripPlaceNo: Long = 0,
    @ColumnInfo(name = "trip_no")
    val tripNo: Long,
    @ColumnInfo(name = "place_ord")
    val placeOrd: Int,
    @ColumnInfo(name = "user_place_nm")
    val userPlaceNm: String? = null,
    @ColumnInfo(name = "memo")
    val memo: String? = null,
    @ColumnInfo(name = "draft_yn")
    val draftYn: String,
    @ColumnInfo(name = "lat")
    val lat: Double? = null,
    @ColumnInfo(name = "lng")
    val lng: Double? = null,
    @ColumnInfo(name = "addr")
    val addr: String? = null,
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "created_user_no")
    val createdUserNo: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "updated_user_no")
    val updatedUserNo: Long,
    @ColumnInfo(name = "deleted_yn")
    val deletedYn: String = "N",
)

/**
 * 태그 마스터(tag_mst) — 해요체 경험 태그 · use_yn = Y 만 · sort_ord 순(시드 = 추천 시드 태그와 같은 번호).
 */
@Entity(tableName = "tag_mst")
data class TagMstEntity(
    @PrimaryKey
    @ColumnInfo(name = "tag_no")
    val tagNo: Long,
    @ColumnInfo(name = "tag_nm")
    val tagNm: String,
    @ColumnInfo(name = "use_yn")
    val useYn: String,
    @ColumnInfo(name = "sort_ord")
    val sortOrd: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "created_user_no")
    val createdUserNo: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "updated_user_no")
    val updatedUserNo: Long,
    @ColumnInfo(name = "deleted_yn")
    val deletedYn: String = "N",
)

/**
 * 여행 장소 - 태그 관계(trip_place_tag_rel).
 */
@Entity(tableName = "trip_place_tag_rel", indices = [Index("trip_place_no")])
data class TripPlaceTagRelEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "trip_place_tag_no")
    val tripPlaceTagNo: Long = 0,
    @ColumnInfo(name = "trip_place_no")
    val tripPlaceNo: Long,
    @ColumnInfo(name = "tag_no")
    val tagNo: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "created_user_no")
    val createdUserNo: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "updated_user_no")
    val updatedUserNo: Long,
    @ColumnInfo(name = "deleted_yn")
    val deletedYn: String = "N",
)
