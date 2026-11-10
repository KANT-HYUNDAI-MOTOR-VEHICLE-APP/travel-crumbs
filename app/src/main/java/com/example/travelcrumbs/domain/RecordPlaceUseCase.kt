package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.ActionResult
import java.util.UUID
import javax.inject.Inject

/** 「이 장소 기록」 연결 지점 — #3 이 구현을 바꿔 끼움(di/ShellModule 의 바인딩 한 줄). */
fun interface RecordPlaceUseCase {
    suspend operator fun invoke(idempotencyKey: UUID?): ActionResult

    suspend operator fun invoke(): ActionResult = invoke(null)
}

/** 기본 구현 — 늘 「준비 중」. */
class NotReadyRecordPlaceUseCase @Inject constructor() : RecordPlaceUseCase {
    override suspend fun invoke(idempotencyKey: UUID?): ActionResult = ActionResult.NOT_READY
}
