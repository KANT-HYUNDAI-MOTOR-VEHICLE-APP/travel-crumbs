package com.example.travelcrumbs.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.travelcrumbs.R

/**
 * 레일 목적지 목록 — 레일 항목의 유일한 정의(스펙 app-shell 2-1 · 4-1).
 * 항목 하나 = 목적지 id(딥링크가 가리킬 이름 · D4) · 레일 글자 · 아이콘 · 주행 중 허용 여부.
 * 목적지를 더하려면 여기 한 줄 + 화면 파일 하나 + AppShellScreen 의 화면 연결 한 줄(when 이 빠뜨림을 컴파일에서 잡음).
 */
enum class AppDestination(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val allowedWhileDriving: Boolean,
) {
    // 레일 메뉴(디자인 시스템 지시서 10장) — 여행 기록 · 여행 목록 · 찍힌 장소 · 설정. id 는 딥링크 이름이라 그대로 둠
    HOME(id = "home", labelRes = R.string.nav_home, iconRes = R.drawable.ic_record, allowedWhileDriving = false),
    TRIPS(id = "trips", labelRes = R.string.nav_trips, iconRes = R.drawable.ic_nav_trips, allowedWhileDriving = false),
    PLACES(id = "places", labelRes = R.string.nav_places, iconRes = R.drawable.ic_place, allowedWhileDriving = false),
    SETTINGS(
        id = "settings",
        labelRes = R.string.nav_settings,
        iconRes = R.drawable.ic_nav_settings,
        allowedWhileDriving = false,
    ),
    ;

    companion object {
        /** 처음 실행 · 시스템 뒤로의 도착점. */
        val START: AppDestination = HOME

        fun fromId(id: String?): AppDestination = entries.firstOrNull { it.id == id } ?: START
    }
}
