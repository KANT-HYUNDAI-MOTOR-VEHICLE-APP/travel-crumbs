package com.example.travelcrumbs.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.travelcrumbs.R

/**
 * 목적지 목록 — 목적지의 유일한 정의(스펙 app-shell-v5 2-1 · 4-1 · D33). 4개 = 레일 4칸(모두 레일에 보임 · D35).
 * 항목 하나 = 목적지 id(딥링크가 가리킬 이름 · #6 D4 · N3) · 글자 · 아이콘 · 주행 중 허용 여부.
 * 주행 중 허용 = 「기록」 하나(주행 전용 화면) · 나머지는 주행 중 내용 대신 주행 제한 안내(D37).
 * 목적지를 더하려면 여기 한 줄 + 화면 파일 하나 + AppShellScreen 의 화면 연결 한 줄(when 이 빠뜨림을 컴파일에서 잡음).
 */
enum class AppDestination(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val allowedWhileDriving: Boolean,
) {
    RECORDS(
        id = "records",
        labelRes = R.string.nav_records,
        iconRes = R.drawable.ic_nav_records,
        allowedWhileDriving = true,
    ),
    TRIPS(
        id = "trips",
        labelRes = R.string.nav_trips,
        iconRes = R.drawable.ic_nav_trips,
        allowedWhileDriving = false,
    ),
    RECOMMENDATIONS(
        id = "recommendations",
        labelRes = R.string.nav_recommendations,
        iconRes = R.drawable.ic_nav_recommendations,
        allowedWhileDriving = false,
    ),
    SETTINGS(
        id = "settings",
        labelRes = R.string.nav_settings,
        iconRes = R.drawable.ic_nav_settings,
        allowedWhileDriving = false,
    ),
    ;

    companion object {
        /** 처음 실행 · 앱 시작 · 정차 중 시스템 뒤로의 도착점 — 「기록」(D34). */
        val START: AppDestination = RECORDS

        /** 주행이 시작될 때마다 고르는 칸 — 주행 전용 화면(2-7). */
        val DRIVING_START: AppDestination = RECORDS

        /** 레일 칸 — 위에서 아래로 이 순서(D33). */
        val railItems: List<AppDestination> = entries

        /** id → 목적지. 없어진 id(`home` · `inbox`) · 모르는 id · null 은 「기록」(3-5 · V75 · V76). */
        fun fromId(id: String?): AppDestination = entries.firstOrNull { it.id == id } ?: START
    }
}
