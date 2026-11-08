package com.example.travelcrumbs.data.gleo

import android.content.Intent
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.RecordPlaceUseCase
import com.example.travelcrumbs.domain.model.GleoCommandState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Gleo 입력·업무 연결·결과의 창구. 별도 음성 인식이나 자체 TTS는 수행하지 않음. */
interface GleoActionRepository {

    /** 이번 실행이 Gleo Action 으로 열린 것인지 — 화면이 보여 주는 값. */
    val openedByGleoAction: StateFlow<Boolean>
    val commandState: StateFlow<GleoCommandState>

    /** MainActivity 가 받은 Intent 를 그대로 넘김. Gleo Action 이 아니면 아무 일도 하지 않음. */
    fun handleActionIntent(intent: Intent?)
    fun clearCommandNotice(noticeId: Long)
}

@Singleton
class DefaultGleoActionRepository @Inject constructor(
    callbackDataSource: GleoCallbackDataSource,
    readiness: GleoRecordingReadiness,
    recordPlace: RecordPlaceUseCase,
) : GleoActionRepository {

    // Activity의 재생성이 진행 중 저장을 취소하거나 다시 실행하지 않도록 프로세스 범위로 공유.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val processor = GleoCommandProcessor(scope, readiness, recordPlace, callbackDataSource::send)
    private val _openedByGleoAction = MutableStateFlow(false)

    override val openedByGleoAction: StateFlow<Boolean> = _openedByGleoAction.asStateFlow()
    override val commandState: StateFlow<GleoCommandState> = processor.state

    override fun handleActionIntent(intent: Intent?) {
        val request = GleoIntentReader.read(intent) ?: return
        if (request.functionName == AppConfig.GLEO_FUNCTION_OPEN) {
            _openedByGleoAction.value = GleoActionParser.parse(request.functionName, request.actionParam).isSuccess
        }
        processor.handle(request)
    }

    override fun clearCommandNotice(noticeId: Long) = processor.clearNotice(noticeId)
}
