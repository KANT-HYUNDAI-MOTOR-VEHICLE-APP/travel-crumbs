package com.example.travelcrumbs.data.rating

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.domain.model.GuidedTrip
import com.example.travelcrumbs.domain.model.MyRating
import com.example.travelcrumbs.domain.model.RatingRecords
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 별점 기록(#41)을 기기 안(DataStore 키 하나)에 둠 — 서버로 보내지 않음 · 내용은 로그에 찍지 않음. */
interface RatingLocalDataSource {
    val records: Flow<RatingRecords>

    /** 읽고 바꿔 쓰기를 한 번에(동시에 두 번 불려도 하나가 다른 하나를 덮지 않음). */
    suspend fun update(change: (RatingRecords) -> RatingRecords)
}

@Singleton
class DataStoreRatingLocalDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : RatingLocalDataSource {

    override val records: Flow<RatingRecords> = dataStore.data.map { prefs -> decode(prefs[AppPreferencesKeys.RATING_RECORDS_JSON]) }

    override suspend fun update(change: (RatingRecords) -> RatingRecords) {
        dataStore.edit { prefs ->
            val next = change(decode(prefs[AppPreferencesKeys.RATING_RECORDS_JSON]))
            prefs[AppPreferencesKeys.RATING_RECORDS_JSON] = json.encodeToString(Stored.serializer(), Stored.from(next))
        }
    }

    private fun decode(raw: String?): RatingRecords {
        if (raw == null) return RatingRecords()
        return runCatching { json.decodeFromString(Stored.serializer(), raw).toDomain() }
            .onFailure { Log.w(TAG, "별점 기록 읽기 실패: ${it.javaClass.simpleName}") }
            .getOrDefault(RatingRecords())
    }

    @Serializable
    private data class Stored(
        val guidedTrips: List<StoredGuided> = emptyList(),
        val ratings: List<StoredRating> = emptyList(),
        val promptedTripNos: List<Long> = emptyList(),
    ) {
        fun toDomain() = RatingRecords(
            guidedTrips = guidedTrips.map { GuidedTrip(it.tripNo, it.tripNm, it.nicknameNm, it.guidedAt, it.tripEndedAt) },
            ratings = ratings.map { MyRating(it.tripNo, it.ratingScore, it.tagNos.toSet(), it.ratedAt) },
            promptedTripNos = promptedTripNos.toSet(),
        )

        companion object {
            fun from(records: RatingRecords) = Stored(
                guidedTrips = records.guidedTrips.map { StoredGuided(it.tripNo, it.tripNm, it.nicknameNm, it.guidedAt, it.tripEndedAt) },
                ratings = records.ratings.map { StoredRating(it.tripNo, it.ratingScore, it.tagNos.sorted(), it.ratedAt) },
                promptedTripNos = records.promptedTripNos.sorted(),
            )
        }
    }

    @Serializable
    private data class StoredGuided(
        val tripNo: Long,
        val tripNm: String,
        val nicknameNm: String,
        val guidedAt: Long,
        val tripEndedAt: Long? = null,
    )

    @Serializable
    private data class StoredRating(val tripNo: Long, val ratingScore: Int, val tagNos: List<Long>, val ratedAt: Long)

    private companion object {
        const val TAG = "RatingLocal"
    }
}
