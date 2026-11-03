package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.Gear
import org.junit.Assert.assertEquals
import org.junit.Test

class GearToDrivingStateTest {

    @Test
    fun `T1 P 기어는 정차`() {
        assertEquals(DrivingState.PARKED, Gear.PARK.toDrivingState())
    }

    @Test
    fun `T2 R N D 기어는 주행`() {
        assertEquals(DrivingState.DRIVING, Gear.REVERSE.toDrivingState())
        assertEquals(DrivingState.DRIVING, Gear.NEUTRAL.toDrivingState())
        assertEquals(DrivingState.DRIVING, Gear.DRIVE.toDrivingState())
    }

    @Test
    fun `T3 모르는 기어는 UNKNOWN`() {
        assertEquals(DrivingState.UNKNOWN, Gear.UNKNOWN.toDrivingState())
    }
}
