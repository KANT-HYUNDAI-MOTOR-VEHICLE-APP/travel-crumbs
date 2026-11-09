package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.rating.DefaultRatingRepository
import com.example.travelcrumbs.data.rating.RatingLocalDataSource
import com.example.travelcrumbs.domain.GuidanceDestination
import com.example.travelcrumbs.domain.GuidanceRequestResult
import com.example.travelcrumbs.domain.RouteGuidanceRequester
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.RatingRecords
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** 길안내 요청 포트 fake — 받은 목적지를 기록하고 [result] 로 답함 · [available] 거짓 = release(내비 연결 전). */
class FakeRouteGuidanceRequester(
    var result: GuidanceRequestResult = GuidanceRequestResult.Started,
    override var available: Boolean = true,
) : RouteGuidanceRequester {
    val requested = mutableListOf<GuidanceDestination>()

    override suspend fun requestRoute(destination: GuidanceDestination): GuidanceRequestResult {
        requested += destination
        return result
    }
}

/** 별점 기록 자료원 fake — 메모리에만 둠(DataStore 대신). 저장소는 진짜([DefaultRatingRepository])를 씀. */
class FakeRatingLocalDataSource(initial: RatingRecords = RatingRecords()) : RatingLocalDataSource {
    val state = MutableStateFlow(initial)

    override val records: Flow<RatingRecords> = state

    override suspend fun update(change: (RatingRecords) -> RatingRecords) {
        state.update(change)
    }
}

fun ratingRepositoryOf(
    time: TimeSource = FakeTimeSource(),
    local: FakeRatingLocalDataSource = FakeRatingLocalDataSource(),
): DefaultRatingRepository = DefaultRatingRepository(local, time)
