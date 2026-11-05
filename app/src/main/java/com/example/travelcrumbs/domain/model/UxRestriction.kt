package com.example.travelcrumbs.domain.model

/** 주행 규제(CarUxRestrictions)를 도메인 값으로 바꾼 것 — AAOS 타입은 data/vehicle 밖으로 나가지 않음. */
enum class UxRestriction {
    /** 주의 분산 최적화가 필요한 상태(차가 움직이는 중 등). */
    RESTRICTED,
    UNRESTRICTED,

    /** 규제 서비스에 닿지 못함 — 기어만으로 판정. */
    UNKNOWN,
}
