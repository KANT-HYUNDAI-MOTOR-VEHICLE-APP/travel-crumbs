package com.example.travelcrumbs.data.gleo

import android.content.Intent
import android.os.Bundle
import com.example.travelcrumbs.domain.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GleoIntentReaderTest {
    @Test
    fun `T2 T3 런처와 미등록 Intent는 읽지 않음`() {
        assertNull(GleoIntentReader.read(null))
        assertNull(GleoIntentReader.read(Intent(Intent.ACTION_MAIN)))
        assertNull(GleoIntentReader.read(Intent("OTHER_ACTION")))
    }

    @Test
    fun `T4 RECORD의 extras 부재와 ACTION_PARAM 문자열 허용`() {
        assertTrue(GleoIntentReader.read(Intent(AppConfig.GLEO_FUNCTION_RECORD))!!.readable)
        val read = GleoIntentReader.read(Intent(AppConfig.GLEO_FUNCTION_RECORD).putExtra("ACTION_PARAM", "{}"))!!
        assertTrue(read.readable)
        assertEquals("{}", read.actionParam)
    }

    @Test
    fun `T5 ACTION_PARAM의 숫자와 Bundle 타입은 부재로 오인하지 않고 거절`() {
        assertFalse(GleoIntentReader.read(Intent(AppConfig.GLEO_FUNCTION_RECORD).putExtra("ACTION_PARAM", 7))!!.readable)
        assertFalse(GleoIntentReader.read(Intent(AppConfig.GLEO_FUNCTION_RECORD).putExtra("ACTION_PARAM", Bundle()))!!.readable)
    }

    @Test
    fun `T5 개별 업무 extra는 새 위치나 여행을 받는 입력으로 쓰지 않고 거절`() {
        val intent = Intent(AppConfig.GLEO_FUNCTION_RECORD)
            .putExtra("ACTION_PARAM", "{}")
            .putExtra("tripUuid", "untrusted")
        assertFalse(GleoIntentReader.read(intent)!!.readable)
    }

    @Test
    fun `실제 Gleo의 패키지명 접두어 action도 같은 함수로 읽음`() {
        val record = GleoIntentReader.read(Intent("com.example.travelcrumbs.RECORD_CURRENT_PLACE"))!!
        assertEquals(AppConfig.GLEO_FUNCTION_RECORD, record.functionName)
        assertTrue(record.readable)
        val control = GleoIntentReader.read(Intent("com.example.travelcrumbs.CONTROL_CURRENT_TRIP"))!!
        assertEquals(AppConfig.GLEO_FUNCTION_CONTROL, control.functionName)
        val info = GleoIntentReader.read(Intent("SHOW_TRAVEL_INFO"))!!
        assertEquals(AppConfig.GLEO_FUNCTION_INFO, info.functionName)
        assertNull(GleoIntentReader.read(Intent("com.other.app.RECORD_CURRENT_PLACE")))
    }

    @Test
    fun `69-U4 OPEN_TRAVEL_CRUMBS 는 더 이상 Gleo 함수로 읽지 않음`() {
        assertNull(GleoIntentReader.read(Intent("OPEN_TRAVEL_CRUMBS")))
        assertNull(GleoIntentReader.read(Intent("com.example.travelcrumbs.OPEN_TRAVEL_CRUMBS")))
    }

    @Test
    fun `69 새 함수도 ACTION_PARAM 밖 extra 는 거절`() {
        val read = GleoIntentReader.read(Intent(AppConfig.GLEO_FUNCTION_CONTROL).putExtra("ACTION_PARAM", "{}").putExtra("other", "x"))!!
        assertFalse(read.readable)
    }
}
