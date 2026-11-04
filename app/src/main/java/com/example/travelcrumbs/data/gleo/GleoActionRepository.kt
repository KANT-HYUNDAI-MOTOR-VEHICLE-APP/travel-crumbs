package com.example.travelcrumbs.data.gleo

import android.content.Intent
import android.util.Log
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.GleoCallback
import com.example.travelcrumbs.domain.model.GleoStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gleo Action 을 받아 해석하고 곧바로 Callback 을 돌려보냄.
 * Gleo 는 **7초** 뒤 Action 을 취소하므로 느린 초기화를 기다리지 않고 바로 답함.
 */
interface GleoActionRepository {

    /** 이번 실행이 Gleo Action 으로 열린 것인지 — 화면이 보여 주는 값. */
    val openedByGleoAction: StateFlow<Boolean>

    /** MainActivity 가 받은 Intent 를 그대로 넘김. Gleo Action 이 아니면 아무 일도 하지 않음. */
    fun handleActionIntent(intent: Intent?)
}

@Singleton
class DefaultGleoActionRepository @Inject constructor(
    private val callbackDataSource: GleoCallbackDataSource,
) : GleoActionRepository {

    private val _openedByGleoAction = MutableStateFlow(false)

    override val openedByGleoAction: StateFlow<Boolean> = _openedByGleoAction.asStateFlow()

    override fun handleActionIntent(intent: Intent?) {
        val action = intent?.action ?: return
        // 우리가 선언한 function 이름일 때만 답함 — 런처(ACTION_MAIN)나 남의 action 은 그냥 무시.
        if (action != AppConfig.GLEO_FUNCTION_OPEN) return

        // 악의적인 extras 는 꺼내는 것만으로도 터질 수 있어 읽기 자체를 감쌈.
        val actionParam = runCatching { intent.extras?.getString(EXTRA_ACTION_PARAM) }.getOrNull()
        val parsed = GleoActionParser.parse(action, actionParam)

        // 받은 값으로 동작을 가르지 않음 — 이 function 은 파라미터가 없고, 화면을 띄우는 일만 함.
        val callback = parsed.fold(
            onSuccess = {
                GleoCallback(
                    functionName = AppConfig.GLEO_FUNCTION_OPEN,
                    status = GleoStatus.SUCCESS,
                    message = AppConfig.GLEO_OPEN_SUCCESS_MESSAGE,
                )
            },
            onFailure = { error ->
                Log.w(TAG, "ACTION_PARAM 해석 실패: ${error.javaClass.simpleName}")
                GleoCallback(
                    functionName = AppConfig.GLEO_FUNCTION_OPEN,
                    status = GleoStatus.FAIL,
                    message = AppConfig.GLEO_OPEN_FAIL_MESSAGE,
                )
            },
        )
        _openedByGleoAction.value = parsed.isSuccess
        callbackDataSource.send(callback)
    }

    private companion object {
        const val TAG = "GleoAction"
        const val EXTRA_ACTION_PARAM = "ACTION_PARAM"
    }
}
