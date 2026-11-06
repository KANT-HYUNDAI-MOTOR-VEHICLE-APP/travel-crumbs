package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.Gear
import com.example.travelcrumbs.domain.model.UxRestriction
import org.junit.Assert.assertEquals
import org.junit.Test

/** 스펙 app-shell 3-1 합성 표. */
class DrivingStateRulesTest {

    @Test
    fun `T10 기어 P 이고 규제 있으면 주행`() {
        assertEquals(DrivingState.DRIVING, combineDrivingState(Gear.PARK, UxRestriction.RESTRICTED))
    }

    @Test
    fun `T11 기어 P 이고 규제 모르면 정차`() {
        assertEquals(DrivingState.PARKED, combineDrivingState(Gear.PARK, UxRestriction.UNKNOWN))
    }

    @Test
    fun `T12 기어 모르고 규제 없으면 UNKNOWN`() {
        assertEquals(DrivingState.UNKNOWN, combineDrivingState(Gear.UNKNOWN, UxRestriction.UNRESTRICTED))
        assertEquals(DrivingState.UNKNOWN, combineDrivingState(Gear.UNKNOWN, UxRestriction.UNKNOWN))
    }

    @Test
    fun `T13 기어 D 이고 규제 없으면 주행`() {
        assertEquals(DrivingState.DRIVING, combineDrivingState(Gear.DRIVE, UxRestriction.UNRESTRICTED))
    }

    @Test
    fun `T5 T6 T7 기어 R N D 는 규제와 상관없이 주행`() {
        listOf(Gear.REVERSE, Gear.NEUTRAL, Gear.DRIVE).forEach { gear ->
            UxRestriction.entries.forEach { restriction ->
                assertEquals("$gear · $restriction", DrivingState.DRIVING, combineDrivingState(gear, restriction))
            }
        }
    }

    @Test
    fun `T1 기어 P 이고 규제 없으면 정차`() {
        assertEquals(DrivingState.PARKED, combineDrivingState(Gear.PARK, UxRestriction.UNRESTRICTED))
    }

    @Test
    fun `T9 기어 모르고 규제 있으면 주행`() {
        assertEquals(DrivingState.DRIVING, combineDrivingState(Gear.UNKNOWN, UxRestriction.RESTRICTED))
    }
}
