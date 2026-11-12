package com.example.travelcrumbs.data.navi

/**
 * 상시 리스너 칸 하나(#66 스펙 3.8 「상시 리스너 수명」) — 붙음마다 [attach] · 끊김 · 세션 끝마다 [detach].
 * 이미 붙어 있으면 다시 붙이지 않음(중복 등록이면 같은 알림을 두 번 받아 대기 · 기록 순서가 흐트러짐 · T50).
 * SDK 타입을 모르게 붙이기 · 떼기를 함수로 받음(단위 시험).
 */
class NaviListenerSlot(
    private val add: () -> Unit,
    private val remove: () -> Unit,
) {
    private var attached = false

    @Synchronized
    fun attach() {
        if (attached) return
        if (runCatching(add).isSuccess) attached = true
    }

    @Synchronized
    fun detach() {
        if (!attached) return
        attached = false
        runCatching(remove)
    }

    val isAttached: Boolean
        @Synchronized get() = attached
}
