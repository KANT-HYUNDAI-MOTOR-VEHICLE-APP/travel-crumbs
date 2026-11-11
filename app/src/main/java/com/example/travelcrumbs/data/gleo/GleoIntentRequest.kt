package com.example.travelcrumbs.data.gleo

import android.content.Intent
import android.util.Log
import com.example.travelcrumbs.domain.AppConfig

/** Android 경계에서만 읽는 입력. extras를 꺼내다 실패한 경우와 파라미터 부재를 구분. */
data class GleoIntentRequest(
    val functionName: String,
    val actionParam: String?,
    val readable: Boolean = true,
)

object GleoIntentReader {
    private const val EXTRA_ACTION_PARAM = "ACTION_PARAM"
    private const val TAG = "GleoIntent"

    @Suppress("DEPRECATION") // minSdk 28에서 Bundle.get의 실제 타입을 확인하기 위한 경계.
    fun read(intent: Intent?): GleoIntentRequest? {
        // 실제 Gleo 는 「패키지명.함수명」, 공식 예제는 「함수명」 — 함수명으로 맞춰 읽음.
        val action = intent?.action?.removePrefix(AppConfig.GLEO_ACTION_PREFIX) ?: return null
        if (action != AppConfig.GLEO_FUNCTION_OPEN && action != AppConfig.GLEO_FUNCTION_RECORD) return null
        // 실기에서 Gleo 가 넣는 extras 꼴을 확인하기 위한 키 이름만 기록(값은 남기지 않음).
        runCatching { Log.i(TAG, "action=${intent.action} extraKeys=${intent.extras?.keySet()?.sorted()}") }
        if (action == AppConfig.GLEO_FUNCTION_OPEN) {
            // 기존 OPEN의 파라미터 호환 동작 유지.
            return GleoIntentRequest(action, runCatching { intent.extras?.getString(EXTRA_ACTION_PARAM) }.getOrNull())
        }
        return runCatching {
            val extras = intent.extras
            require(extras?.keySet().orEmpty().all { it == EXTRA_ACTION_PARAM })
            val raw = extras?.get(EXTRA_ACTION_PARAM)
            require(raw == null || raw is String)
            GleoIntentRequest(action, raw as? String)
        }.getOrElse { GleoIntentRequest(action, null, readable = false) }
    }
}
