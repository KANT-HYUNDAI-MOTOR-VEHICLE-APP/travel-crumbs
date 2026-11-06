package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.NaviRequestStatus
import com.lemonappdev.konsist.api.Konsist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T114 — 표준 코드 그룹 `NAVI_REQ_STATUS` 의 앱 상수가 domain 한 곳(데이터 표준 v0.3 R10 · android-architecture 규칙 6). */
class NaviRequestStatusTest {

    @Test
    fun `T114 NAVI_REQ_STATUS 세 값 — 표준 가안 글자 그대로`() {
        assertEquals(
            listOf("REQUESTING", "SUCCESS", "FAILED"),
            NaviRequestStatus.entries.map { it.code },
        )
    }

    @Test
    fun `T114 코드값 글자는 domain 의 NaviRequestStatus 한 파일에만`() {
        val codes = NaviRequestStatus.entries.map { "\"${it.code}\"" }
        val holders = Konsist.scopeFromProduction().files
            .filter { file -> codes.any { file.text.contains(it) } }
            .map { it.name }
        assertEquals(listOf("NaviRequestStatus"), holders)
        assertTrue(
            Konsist.scopeFromProduction().files.first { it.name == "NaviRequestStatus" }
                .packagee?.name.orEmpty().endsWith(".domain.model"),
        )
    }
}
