package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.model.GleoCallback
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Callback JSON 만들기 — 순수 함수라 기기 없이 시험함. */
object GleoCallbackFormat {

    fun toJson(callback: GleoCallback): String = buildJsonObject {
        put("functionName", callback.functionName)
        put("status", callback.status.wireName)
        put("message", callback.message)
    }.toString()
}
