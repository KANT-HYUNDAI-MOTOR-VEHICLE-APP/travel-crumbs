package com.example.travelcrumbs.domain

/** v1.7 사용자 입력 검증. 이번 제출에서는 Kotlin [String.length] 기준을 사용한다. */
object InputValidation {
    fun nickname(value: String): Boolean = value.trim().length in AppConfig.NICKNAME_MIN_LENGTH..AppConfig.NICKNAME_MAX_LENGTH

    fun tripName(value: String): Boolean = value.trim().length in AppConfig.TRIP_NAME_MIN_LENGTH..AppConfig.TRIP_NAME_MAX_LENGTH

    fun memo(value: String): Boolean = value.length <= AppConfig.MEMO_MAX_LENGTH
}
