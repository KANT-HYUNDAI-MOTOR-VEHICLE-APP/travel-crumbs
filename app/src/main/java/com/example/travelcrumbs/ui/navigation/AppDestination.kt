package com.example.travelcrumbs.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.travelcrumbs.R

/**
 * 목적지 목록 — 목적지의 유일한 정의(스펙 app-shell-v5 2-1 · 4-1). 6개 = 레일 5칸 + 레일에 없는 「설정」.
 * 항목 하나 = 목적지 id(딥링크가 가리킬 이름 · #6 D4 · N3) · 글자 · 아이콘 · 레일 표시 여부 · 주행 중 허용 여부.
 * 주행 중 허용 = 「기록」 하나(주행 전용 화면) · 나머지는 주행 중 내용 대신 주행 제한 안내(D29 · N20).
 * 목적지를 더하려면 여기 한 줄 + 화면 파일 하나 + AppShellScreen 의 화면 연결 한 줄(when 이 빠뜨림을 컴파일에서 잡음).
 */
enum class AppDestination(
    val id: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val inRail: Boolean,
    val allowedWhileDriving: Boolean,
) {
    HOME(
        id = "home",
        labelRes = R.string.nav_home,
        iconRes = R.drawable.ic_nav_home,
        inRail = true,
        allowedWhileDriving = false,
    ),
    RECORDS(
        id = "records",
        labelRes = R.string.nav_records,
        iconRes = R.drawable.ic_nav_records,
        inRail = true,
        allowedWhileDriving = true,
    ),
    TRIPS(
        id = "trips",
        labelRes = R.string.nav_trips,
        iconRes = R.drawable.ic_nav_trips,
        inRail = true,
        allowedWhileDriving = false,
    ),
    INBOX(
        id = "inbox",
        labelRes = R.string.nav_inbox,
        iconRes = R.drawable.ic_nav_inbox,
        inRail = true,
        allowedWhileDriving = false,
    ),
    RECOMMENDATIONS(
        id = "recommendations",
        labelRes = R.string.nav_recommendations,
        iconRes = R.drawable.ic_nav_recommendations,
        inRail = true,
        allowedWhileDriving = false,
    ),
    SETTINGS(
        id = "settings",
        labelRes = R.string.nav_settings,
        iconRes = R.drawable.ic_nav_settings,
        inRail = false,
        allowedWhileDriving = false,
    ),
    ;

    /** 이 목적지에 있을 때 강조할 레일 칸 — 레일에 없는 설정은 「홈」(설정은 홈에서 들어가는 화면 · N4). */
    val railItem: AppDestination get() = if (inRail) this else START

    companion object {
        /** 처음 실행 · 정차 중 시스템 뒤로의 도착점. */
        val START: AppDestination = HOME

        /** 주행이 시작될 때마다 고르는 칸 — 주행 전용 화면(2-7). */
        val DRIVING_START: AppDestination = RECORDS

        /** 레일에 보이는 칸 — 위에서 아래로 이 순서(D20). */
        val railItems: List<AppDestination> = entries.filter { it.inRail }

        fun fromId(id: String?): AppDestination = entries.firstOrNull { it.id == id } ?: START
    }
}
