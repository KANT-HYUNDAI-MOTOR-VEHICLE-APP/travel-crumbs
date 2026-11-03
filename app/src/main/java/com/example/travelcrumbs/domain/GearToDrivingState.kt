package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.Gear

/**
 * 기어 → 주행 판정. 순수 계산 · 자료원 없음.
 * P → PARKED · R/N/D → DRIVING · 모름 → UNKNOWN(화면은 DRIVING 처럼 다룸).
 */
fun Gear.toDrivingState(): DrivingState = when (this) {
    Gear.PARK -> DrivingState.PARKED
    Gear.REVERSE, Gear.NEUTRAL, Gear.DRIVE -> DrivingState.DRIVING
    Gear.UNKNOWN -> DrivingState.UNKNOWN
}
