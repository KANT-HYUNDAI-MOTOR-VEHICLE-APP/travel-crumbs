package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.ActionResult
import javax.inject.Inject

/** 「음성」 연결 지점 — #5 가 구현을 바꿔 끼움(di/ShellModule 의 바인딩 한 줄). */
fun interface StartVoiceInputUseCase {
    suspend operator fun invoke(): ActionResult
}

/** 기본 구현 — 늘 「준비 중」. */
class NotReadyStartVoiceInputUseCase @Inject constructor() : StartVoiceInputUseCase {
    override suspend fun invoke(): ActionResult = ActionResult.NOT_READY
}
