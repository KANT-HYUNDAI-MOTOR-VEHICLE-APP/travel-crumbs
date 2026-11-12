package com.example.travelcrumbs.testing

import com.example.travelcrumbs.data.autorecord.AutoPlaceRecorder
import com.example.travelcrumbs.data.autorecord.AutoRecordLocalDataSource
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.trip.TripRepository
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.GuideRequestTracker
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.AutoPlaceName
import com.example.travelcrumbs.domain.model.PendingArrival
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 기록 대기 · 자동 이름 원본 기기 안 저장 대역(#66) — 메모리. 새로 만들지 않고 넘기면 프로세스 재시작 흉내(저장은 남음). */
class FakeAutoRecordLocalDataSource(
    var pendingList: List<PendingArrival> = emptyList(),
    var nameList: List<AutoPlaceName> = emptyList(),
) : AutoRecordLocalDataSource {
    override suspend fun pendings(): List<PendingArrival> = pendingList

    override suspend fun savePendings(pendings: List<PendingArrival>) {
        pendingList = pendings
    }

    override suspend fun names(): List<AutoPlaceName> = nameList

    override suspend fun saveNames(names: List<AutoPlaceName>) {
        nameList = names
    }

    override suspend fun clearAll() {
        pendingList = emptyList()
        nameList = emptyList()
    }
}

/** 시험용 자동 기록 조립 — [scope] 기본은 메인(시험에선 setMain 의 시험 디스패처). [AutoPlaceRecorder.attach] 는 부르는 쪽 몫. */
fun autoRecorderOf(
    navi: FakeNaviRepository,
    driving: DrivingSignalRepository,
    recording: RecordingRepository,
    users: CurrentUserRepository,
    trips: TripRepository,
    inputs: UnfinishedInputRepository,
    guide: GuideRequestTracker,
    store: AutoRecordLocalDataSource = FakeAutoRecordLocalDataSource(),
    time: TimeSource,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
): AutoPlaceRecorder = AutoPlaceRecorder(navi, driving, recording, users, trips, inputs, guide, store, time, scope)

/** 대기 정리 없이 여행 저장소만 부르는 정리 대역 — 자동 기록과 무관한 화면 시험용. */
class TripOnlyCleanup(private val trips: TripRepository) : com.example.travelcrumbs.domain.PlaceRecordCleanup {
    override suspend fun withdrawLocation() = trips.clearLocations()

    override suspend fun deleteTrip(tripNo: Long) = trips.deleteTrip(tripNo)

    override suspend fun deleteAll() = trips.deleteAll()
}

/** 대기 확정 없이 기록 상태 저장소의 종료만 부르는 대역. */
fun endTripOf(recording: RecordingRepository) = com.example.travelcrumbs.domain.EndTripUseCase { recording.endTrip() }
