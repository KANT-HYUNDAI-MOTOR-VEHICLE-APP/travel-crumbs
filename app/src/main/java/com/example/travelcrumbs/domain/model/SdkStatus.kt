package com.example.travelcrumbs.domain.model

/** SDK 하나의 상태 — 화면이 「되는지 안 되는지」를 그릴 때 쓰는 공통 표현. */
sealed interface SdkStatus<out T> {
    data object Idle : SdkStatus<Nothing>
    data object Loading : SdkStatus<Nothing>
    data class Ready<T>(val value: T) : SdkStatus<T>
    data class Failed(val code: String) : SdkStatus<Nothing>
}
