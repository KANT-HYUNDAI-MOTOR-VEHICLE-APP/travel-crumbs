package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.GleoAction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Gleo Action 의 `ACTION_PARAM`(JSON 문자열) 해석 — 순수 함수라 기기 없이 시험함.
 *
 * 들어온 값은 **믿지 않음**: 이 intent-filter 는 Gleo 말고 아무 앱이나 쏠 수 있음.
 * 함수마다 schema.json 에 선언한 키만 받음 — 모르는 키 · enum 밖 값 · 문자열이 아닌 값 · 깨진 JSON 은 거절(#25 T5 · #69 4.2).
 * 예외를 밖으로 던지지 않고 [Result] 로 돌려줌.
 */
object GleoActionParser {

    val OPERATIONS: Set<String> = setOf("PAUSE", "RESUME", "END")
    val INFO_TYPES: Set<String> = setOf("TODAY_RECORDS", "RECOMMENDED_ROADMAPS")

    fun parse(action: String?, actionParam: String?): Result<GleoAction> = runCatching {
        val functionName = action?.takeIf { it.isNotBlank() } ?: error("intent action is missing")
        require((actionParam?.length ?: 0) <= AppConfig.GLEO_MAX_ACTION_PARAM_CHARS)
        val raw = actionParam?.takeIf { it.isNotBlank() } ?: EMPTY_OBJECT
        val obj = Json.parseToJsonElement(raw) as? JsonObject ?: error("ACTION_PARAM is not a JSON object")
        val values = obj.mapValues { (key, value) ->
            val primitive = value as? JsonPrimitive ?: error("$key is not a primitive")
            require(primitive.isString) { "$key is not a string" }
            primitive.content
        }
        when (functionName) {
            AppConfig.GLEO_FUNCTION_RECORD -> require(values.isEmpty())
            AppConfig.GLEO_FUNCTION_CONTROL -> {
                require(values.keys == setOf(AppConfig.GLEO_PARAM_OPERATION))
                require(values.getValue(AppConfig.GLEO_PARAM_OPERATION) in OPERATIONS)
            }
            AppConfig.GLEO_FUNCTION_INFO -> {
                require((values.keys - setOf(AppConfig.GLEO_PARAM_INFO_TYPE, AppConfig.GLEO_PARAM_REGION)).isEmpty())
                require(values[AppConfig.GLEO_PARAM_INFO_TYPE] in INFO_TYPES)
                values[AppConfig.GLEO_PARAM_REGION]?.let {
                    require(it.isNotBlank() && it.length <= AppConfig.GLEO_MAX_REGION_CHARS)
                }
            }
            else -> error("unknown function")
        }
        GleoAction(functionName = functionName, parameters = values)
    }

    private const val EMPTY_OBJECT = "{}"
}
