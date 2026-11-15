package com.example.travelcrumbs.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidationTest {

    @Test
    fun `nickname accepts 2 to 12 trimmed characters`() {
        assertFalse(InputValidation.nickname("a"))
        assertTrue(InputValidation.nickname("  서윤맘  "))
        assertTrue(InputValidation.nickname("a".repeat(12)))
        assertFalse(InputValidation.nickname("a".repeat(13)))
    }

    @Test
    fun `trip name accepts 1 to 30 trimmed characters`() {
        assertFalse(InputValidation.tripName("   "))
        assertTrue(InputValidation.tripName("여행"))
        assertTrue(InputValidation.tripName("a".repeat(30)))
        assertFalse(InputValidation.tripName("a".repeat(31)))
    }

    @Test
    fun `memo accepts up to 200 characters`() {
        assertTrue(InputValidation.memo("a".repeat(200)))
        assertFalse(InputValidation.memo("a".repeat(201)))
    }
}
