package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.GleoCallback
import com.example.travelcrumbs.domain.model.GleoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** #25 · #69 스펙 4.2 — 함수마다 schema.json 에 선언한 키 · enum 만 받음. */
class GleoActionParserTest {

    @Test
    fun `RECORD 는 파라미터 없음만 받음`() {
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_RECORD, null).isSuccess)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_RECORD, "{}").isSuccess)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_RECORD, """{"x":"1"}""").isFailure)
    }

    @Test
    fun `69-T1 T2 CONTROL 은 operation 하나 · PAUSE RESUME END 만`() {
        for (op in listOf("PAUSE", "RESUME", "END")) {
            val parsed = GleoActionParser.parse(AppConfig.GLEO_FUNCTION_CONTROL, """{"operation":"$op"}""").getOrThrow()
            assertEquals(mapOf("operation" to op), parsed.parameters)
        }
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_CONTROL, null).isFailure)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_CONTROL, """{"operation":"STOP"}""").isFailure)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_CONTROL, """{"operation":"PAUSE","tripNo":"1"}""").isFailure)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_CONTROL, """{"operation":1}""").isFailure)
    }

    @Test
    fun `69 INFO 는 infoType 필수 · region 선택 · 그 밖 키 거절`() {
        val reco = GleoActionParser.parse(
            AppConfig.GLEO_FUNCTION_INFO,
            """{"infoType":"RECOMMENDED_ROADMAPS","region":"경주"}""",
        ).getOrThrow()
        assertEquals("경주", reco.parameters["region"])
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_INFO, """{"infoType":"TODAY_RECORDS"}""").isSuccess)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_INFO, """{"region":"경주"}""").isFailure)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_INFO, """{"infoType":"WEATHER"}""").isFailure)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_INFO, """{"infoType":"TODAY_RECORDS","lat":"37"}""").isFailure)
        val tooLong = "가".repeat(AppConfig.GLEO_MAX_REGION_CHARS + 1)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_INFO, """{"infoType":"RECOMMENDED_ROADMAPS","region":"$tooLong"}""").isFailure)
    }

    @Test
    fun `깨진 JSON · 배열 · 모르는 함수 · action 없음은 예외 없이 실패`() {
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_INFO, "{this is not json").isFailure)
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_INFO, "[1,2,3]").isFailure)
        assertTrue(GleoActionParser.parse("OPEN_TRAVEL_CRUMBS", "{}").isFailure)
        assertTrue(GleoActionParser.parse(null, "{}").isFailure)
        assertTrue(GleoActionParser.parse("  ", "{}").isFailure)
    }

    @Test
    fun `callback JSON 은 data 가 있을 때만 data 칸을 넣음`() {
        val plain = GleoCallbackFormat.toJson(GleoCallback(AppConfig.GLEO_FUNCTION_CONTROL, GleoStatus.SUCCESS, AppConfig.GLEO_PAUSED_MESSAGE))
        assertEquals(
            """{"functionName":"CONTROL_CURRENT_TRIP","status":"success","message":"The trip recording is paused."}""",
            plain,
        )
        val withData = GleoCallbackFormat.toJson(GleoCallback(AppConfig.GLEO_FUNCTION_INFO, GleoStatus.SUCCESS, "m", """[{"a":1}]"""))
        assertEquals(
            """{"functionName":"SHOW_TRAVEL_INFO","status":"success","message":"m","data":"[{\"a\":1}]"}""",
            withData,
        )
    }
}
