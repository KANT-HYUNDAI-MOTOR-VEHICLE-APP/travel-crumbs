package com.example.travelcrumbs.domain.model

/** Native NaviHelper emits a destination on start, but no destination on explicit cancellation. */
sealed interface NaviRouteEvent {
    data class Started(val destinationName: String) : NaviRouteEvent
    data class Arrived(val destinationName: String) : NaviRouteEvent
    data object Cancelled : NaviRouteEvent
}
