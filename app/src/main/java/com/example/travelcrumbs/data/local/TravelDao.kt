package com.example.travelcrumbs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 기기 안 여행 표(#51 U1) 읽기 · 쓰기 — 저장소(data/user · data/trip · data/recording)만 씀.
 * 지운 행(deleted_yn = Y)은 읽지 않음. 자료가 작아(사용자 한 명의 여행) 목록은 통째로 읽어 저장소가 묶음.
 */
@Dao
interface TravelDao {
    // 사용자 · 동의

    @Query("SELECT * FROM user_mst WHERE user_no = :userNo AND deleted_yn = '${YesNo.NO}'")
    fun user(userNo: Long): Flow<UserMstEntity?>

    @Query("SELECT * FROM user_mst WHERE user_no = :userNo AND deleted_yn = '${YesNo.NO}'")
    suspend fun userOnce(userNo: Long): UserMstEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(row: UserMstEntity)

    @Query("UPDATE user_mst SET nickname_nm = :nicknameNm, updated_at = :now, updated_user_no = :userNo WHERE user_no = :userNo")
    suspend fun updateNickname(userNo: Long, nicknameNm: String, now: Long)

    @Query("SELECT * FROM user_consent_hist WHERE user_no = :userNo AND deleted_yn = '${YesNo.NO}' ORDER BY user_consent_hist_no ASC")
    fun consents(userNo: Long): Flow<List<UserConsentHistEntity>>

    @Insert
    suspend fun insertConsent(row: UserConsentHistEntity)

    // 여행

    @Query("SELECT * FROM trip_mst WHERE user_no = :userNo AND deleted_yn = '${YesNo.NO}' ORDER BY trip_no DESC LIMIT 1")
    fun latestTrip(userNo: Long): Flow<TripMstEntity?>

    @Query("SELECT * FROM trip_mst WHERE user_no = :userNo AND deleted_yn = '${YesNo.NO}' ORDER BY trip_no DESC LIMIT 1")
    suspend fun latestTripOnce(userNo: Long): TripMstEntity?

    @Query("SELECT * FROM trip_mst WHERE deleted_yn = '${YesNo.NO}' ORDER BY trip_no DESC")
    fun trips(): Flow<List<TripMstEntity>>

    @Insert
    suspend fun insertTrip(row: TripMstEntity): Long

    @Update
    suspend fun updateTrip(row: TripMstEntity)

    @Query("UPDATE trip_mst SET trip_nm = :tripNm, updated_at = :now, updated_user_no = user_no WHERE trip_no = :tripNo")
    suspend fun renameTrip(tripNo: Long, tripNm: String, now: Long)

    @Query("UPDATE trip_mst SET visibility_cd = :visibilityCd, updated_at = :now, updated_user_no = user_no WHERE trip_no = :tripNo")
    suspend fun setVisibility(tripNo: Long, visibilityCd: String, now: Long)

    @Query(
        "UPDATE trip_mst SET visibility_cd = :to, updated_at = :now, updated_user_no = user_no " +
            "WHERE user_no = :userNo AND visibility_cd = :from",
    )
    suspend fun changeVisibility(userNo: Long, from: String, to: String, now: Long)

    @Query("SELECT * FROM trip_companion_dtl WHERE deleted_yn = '${YesNo.NO}' ORDER BY trip_companion_no ASC")
    fun companions(): Flow<List<TripCompanionDtlEntity>>

    @Insert
    suspend fun insertCompanions(rows: List<TripCompanionDtlEntity>)

    // 장소 · 태그

    @Query("SELECT * FROM trip_place_dtl WHERE deleted_yn = '${YesNo.NO}' ORDER BY trip_no ASC, place_ord ASC")
    fun places(): Flow<List<TripPlaceDtlEntity>>

    @Query("SELECT COUNT(*) FROM trip_place_dtl WHERE trip_no = :tripNo AND deleted_yn = '${YesNo.NO}'")
    fun placeCount(tripNo: Long): Flow<Int>

    @Query("SELECT COALESCE(MAX(place_ord), 0) FROM trip_place_dtl WHERE trip_no = :tripNo")
    suspend fun lastPlaceOrd(tripNo: Long): Int

    @Insert
    suspend fun insertPlace(row: TripPlaceDtlEntity): Long

    @Query(
        "UPDATE trip_place_dtl SET user_place_nm = :userPlaceNm, memo = :memo, draft_yn = :draftYn, " +
            "updated_at = :now WHERE trip_place_no = :tripPlaceNo",
    )
    suspend fun updatePlace(tripPlaceNo: Long, userPlaceNm: String?, memo: String?, draftYn: String, now: Long)

    @Query("SELECT * FROM trip_place_tag_rel WHERE deleted_yn = '${YesNo.NO}' ORDER BY trip_place_tag_no ASC")
    fun placeTags(): Flow<List<TripPlaceTagRelEntity>>

    @Query("DELETE FROM trip_place_tag_rel WHERE trip_place_no = :tripPlaceNo")
    suspend fun deletePlaceTags(tripPlaceNo: Long)

    @Insert
    suspend fun insertPlaceTags(rows: List<TripPlaceTagRelEntity>)

    /** 장소 하나를 고침 — 이름 · 메모 · 임시저장 여부 · 태그를 한 트랜잭션으로(태그는 통째로 바꿈). */
    @Transaction
    suspend fun savePlace(
        tripPlaceNo: Long,
        userPlaceNm: String?,
        memo: String?,
        draftYn: String,
        tags: List<TripPlaceTagRelEntity>,
        now: Long,
    ) {
        updatePlace(tripPlaceNo, userPlaceNm, memo, draftYn, now)
        deletePlaceTags(tripPlaceNo)
        if (tags.isNotEmpty()) insertPlaceTags(tags)
    }

    /** 위치 칸 비우기(위치 수집 동의 철회 · #51 리뷰 P1) — 좌표 · 주소를 물리적으로 지움 · 장소 이름이 주소에서 온 처음 값이면 그것도. */
    @Query(
        "UPDATE trip_place_dtl SET lat = NULL, lng = NULL, addr = NULL, user_place_nm = :userPlaceNm, updated_at = :now " +
            "WHERE trip_place_no = :tripPlaceNo",
    )
    suspend fun clearPlaceLocation(tripPlaceNo: Long, userPlaceNm: String?, now: Long)

    /** 여행 하나 지우기(A-08 「여행 삭제」 · 논리 삭제 · #51 리뷰 P2) — 목록 · 추천 · 지금 여행에서 빠짐. */
    @Query("UPDATE trip_mst SET deleted_yn = '${YesNo.YES}', updated_at = :now, updated_user_no = user_no WHERE trip_no = :tripNo")
    suspend fun markTripDeleted(tripNo: Long, now: Long)

    @Query(
        "DELETE FROM trip_place_tag_rel WHERE trip_place_no IN (SELECT trip_place_no FROM trip_place_dtl " +
            "WHERE trip_no IN (SELECT trip_no FROM trip_mst WHERE user_no = :userNo))",
    )
    suspend fun deletePlaceTagsOfUser(userNo: Long)

    @Query("DELETE FROM trip_place_dtl WHERE trip_no IN (SELECT trip_no FROM trip_mst WHERE user_no = :userNo)")
    suspend fun deletePlacesOfUser(userNo: Long)

    @Query("DELETE FROM trip_companion_dtl WHERE trip_no IN (SELECT trip_no FROM trip_mst WHERE user_no = :userNo)")
    suspend fun deleteCompanionsOfUser(userNo: Long)

    @Query("DELETE FROM trip_mst WHERE user_no = :userNo")
    suspend fun deleteTripsOfUser(userNo: Long)

    /** 내 기록 모두 지우기(C-03 · 물리 삭제 · #51 리뷰 P2) — 여행 · 동행 · 장소 · 태그 관계. 동의 이력 · 사용자 · 태그 마스터는 남김. */
    @Transaction
    suspend fun deleteAllTripsOfUser(userNo: Long) {
        deletePlaceTagsOfUser(userNo)
        deletePlacesOfUser(userNo)
        deleteCompanionsOfUser(userNo)
        deleteTripsOfUser(userNo)
    }

    @Query("SELECT * FROM tag_mst WHERE use_yn = '${YesNo.YES}' AND deleted_yn = '${YesNo.NO}' ORDER BY sort_ord ASC")
    fun tags(): Flow<List<TagMstEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTags(rows: List<TagMstEntity>)
}
