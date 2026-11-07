package com.example.travelcrumbs.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TripNameTest {

    @Test
    fun `기본 여행명은 날짜 여행`() {
        assertEquals("2026.11.05 여행", TripName.default(LocalDate.of(2026, 11, 5)))
    }
}
