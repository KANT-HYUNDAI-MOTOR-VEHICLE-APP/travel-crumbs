package com.example.travelcrumbs.data.trip

import com.example.travelcrumbs.data.autorecord.AutoPlaceRecorder
import com.example.travelcrumbs.data.navi.NaviRepository
import com.example.travelcrumbs.data.recording.RecordingRepository
import com.example.travelcrumbs.data.unfinishedinput.UnfinishedInputRepository
import com.example.travelcrumbs.data.user.CurrentUserRepository
import com.example.travelcrumbs.data.vehicle.DrivingSignalRepository
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.ActionResult
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.DraftPlaceInput
import com.example.travelcrumbs.domain.model.DrivingState
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * 「이 장소 기록」의 실제 구현(#51 U4 · 와이어프레임 v6 A-02 · A-03) — 지금 여행에 장소 한 곳(trip_place_dtl · draft_yn = Y).
 * 위치 = 내비 getCurrentLocationInfo(위도 · 경도 · 주소) · 위치정보 수집 동의(LOCATION_COLLECT)가 없으면 위치 없이 장소만.
 * 주행 중이면 「이어서 쓰기」 띠에 그 장소를 남김(정차 뒤 A-03 시트) · 정차 중이면 화면이 바로 A-03 시트를 엶.
 * 진행 중 여행이 없으면 아무것도 안 하고 NOT_READY. 좌표 · 주소는 로그에 남기지 않음.
 * 내비 목적지 자동 기록(#66 N9)의 기록 대기가 있으면 수동 행을 넣기 직전에 먼저 정리(반경 안 = 폐기 · 나머지 = 먼저 확정).
 * [idempotencyKey] = Gleo 음성 기록이 한 번 발급한 요청 ID(#25) — 기기 안 저장이라 지금은 쓰지 않음(서버 동기화가 돌아오면 장소 POST 의 Idempotency-Key 로 전달).
 */
class TripPlaceRecorder @Inject constructor(
    private val recording: RecordingRepository,
    private val trips: TripRepository,
    private val users: CurrentUserRepository,
    private val navi: NaviRepository,
    private val driving: DrivingSignalRepository,
    private val unfinishedInputs: UnfinishedInputRepository,
    private val autoRecorder: AutoPlaceRecorder,
) : RecordPlaceUseCase {

    override suspend fun invoke(idempotencyKey: UUID?): ActionResult {
        val progress = recording.progress.first()
        val tripNo = progress.tripNo ?: return ActionResult.NOT_READY
        if (!progress.status.inProgress) return ActionResult.NOT_READY
        val located = users.consents.first()[ConsentType.LOCATION_COLLECT]?.agreed == true
        val location = if (located) navi.currentLocation() else null
        val tripPlaceNo = autoRecorder.withManualRecord(tripNo, location?.lat, location?.lng) {
            trips.addPlace(tripNo, location, draft = true)
        }
        if (driving.drivingState.first() != DrivingState.PARKED) {
            unfinishedInputs.saveNow(
                DraftPlaceInput.DESTINATION_ID,
                mapOf(DraftPlaceInput.FIELD_TRIP_PLACE_NO to tripPlaceNo.toString()),
            )
        }
        return ActionResult.DONE
    }
}
