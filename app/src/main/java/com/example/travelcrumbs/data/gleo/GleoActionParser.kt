package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.model.GleoAction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Gleo Action 의 `ACTION_PARAM`(JSON 문자열) 해석 — 순수 함수라 기기 없이 시험함.
 *
 * 들어온 값은 **믿지 않음**: 이 intent-filter 는 Gleo 말고 아무 앱이나 쏠 수 있음.
 * 비었거나 깨졌거나 JSON 객체가 아니어도 예외를 밖으로 던지지 않고 [Result] 로 돌려줌.
 */
object GleoActionParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(action: String?, actionParam: String?): Result<GleoAction> = runCatching {
        val functionName = action?.takeIf { it.isNotBlank() }
            ?: error("intent action is missing")
        val raw = actionParam?.takeIf { it.isNotBlank() } ?: EMPTY_OBJECT
        val element = json.parseToJsonElement(raw)
        val obj = element as? JsonObject ?: error("ACTION_PARAM is not a JSON object")
        GleoAction(
            functionName = functionName,
            parameters = obj.mapValues { (_, value) ->
                (value as? JsonPrimitive)?.content.orEmpty()
            },
        )
    }

    private const val EMPTY_OBJECT = "{}"
}
