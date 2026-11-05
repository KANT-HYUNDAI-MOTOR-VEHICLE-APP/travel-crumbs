package com.example.travelcrumbs.domain

import com.example.travelcrumbs.domain.model.DrivingState
import com.example.travelcrumbs.domain.model.Gear
import com.example.travelcrumbs.domain.model.UxRestriction

/**
 * 기어 + 주행 규제 → 주행 판정(스펙 app-shell 3-1). 순수 계산.
 * 하나라도 주행이면 DRIVING · 기어 P 이고 규제가 없거나 모르면 PARKED · 기어를 모르면 UNKNOWN.
 * 기어만 보는 [toDrivingState] 는 그대로 두고 그 위에 규제를 더함.
 */
fun combineDrivingState(gear: Gear, restriction: UxRestriction): DrivingState {
    if (restriction == UxRestriction.RESTRICTED) return DrivingState.DRIVING
    return gear.toDrivingState()
}
