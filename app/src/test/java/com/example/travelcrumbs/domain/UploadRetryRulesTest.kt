package com.example.travelcrumbs.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** 다시 보내기 간격(스펙 navigation 4.6 · U30). */
class UploadRetryRulesTest {

    @Test
    fun `T63 30초부터 두 배 · 최대 1시간에서 멈춤`() {
        assertEquals(30_000L, uploadRetryDelayMillis(0))
        assertEquals(60_000L, uploadRetryDelayMillis(1))
        assertEquals(1_920_000L, uploadRetryDelayMillis(6))
        assertEquals(AppConfig.PIN_UPLOAD_RETRY_MAX_MILLIS, uploadRetryDelayMillis(7))
        assertEquals(AppConfig.PIN_UPLOAD_RETRY_MAX_MILLIS, uploadRetryDelayMillis(1_000))
        assertEquals(30_000L, uploadRetryDelayMillis(-1))
    }
}
