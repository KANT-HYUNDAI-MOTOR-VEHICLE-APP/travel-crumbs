package com.example.travelcrumbs.data.gleo

import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.model.GleoCallback
import com.example.travelcrumbs.domain.model.GleoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GleoActionParserTest {

    @Test
    fun `T6 ACTION_PARAM 이 없어도 성공하고 파라미터는 비어 있음`() {
        val parsed = GleoActionParser.parse(AppConfig.GLEO_FUNCTION_OPEN, null)

        assertTrue(parsed.isSuccess)
        assertEquals(AppConfig.GLEO_FUNCTION_OPEN, parsed.getOrThrow().functionName)
        assertEquals(emptyMap<String, String>(), parsed.getOrThrow().parameters)
    }

    @Test
    fun `T7 ACTION_PARAM 이 깨져 있으면 예외 없이 실패로 돌아옴`() {
        val parsed = GleoActionParser.parse(AppConfig.GLEO_FUNCTION_OPEN, "{this is not json")

        assertTrue(parsed.isFailure)
    }

    @Test
    fun `T8 action 이 없으면 실패`() {
        assertTrue(GleoActionParser.parse(null, "{}").isFailure)
        assertTrue(GleoActionParser.parse("  ", "{}").isFailure)
    }

    @Test
    fun `T10 JSON 객체가 아니면 실패`() {
        assertTrue(GleoActionParser.parse(AppConfig.GLEO_FUNCTION_OPEN, "[1,2,3]").isFailure)
    }

    @Test
    fun `T11 callback JSON 은 functionName status message 세 칸`() {
        val json = GleoCallbackFormat.toJson(
            GleoCallback(
                functionName = AppConfig.GLEO_FUNCTION_OPEN,
                status = GleoStatus.SUCCESS,
                message = AppConfig.GLEO_OPEN_SUCCESS_MESSAGE,
            ),
        )

        assertEquals(
            """{"functionName":"OPEN_TRAVEL_CRUMBS","status":"success",""" +
                """"message":"Travel Crumbs is now open on the screen."}""",
            json,
        )
    }
}
